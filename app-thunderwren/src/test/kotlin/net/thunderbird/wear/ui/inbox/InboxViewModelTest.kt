package net.thunderbird.wear.ui.inbox

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.testing.FakeDemoModeStore
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeSelectedMailboxStore
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.UNREAD
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection()
    private val selectedMailbox = FakeSelectedMailboxStore()
    private val demoMode = FakeDemoModeStore()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `asks the phone for fresh data when opened`() = runTest {
        createViewModel()
        advanceUntilIdle()

        assertThat(phone.refreshCount).isEqualTo(1)
    }

    @Test
    fun `shows not connected until the phone has published`() = runTest {
        val viewModel = createViewModel()

        assertThat(stateOf(viewModel)).isInstanceOf(InboxUiState.NotConnected::class)
    }

    @Test
    fun `shows the unified inbox by default`() = runTest {
        publishTwoAccounts()
        val viewModel = createViewModel()

        val state = stateOf(viewModel) as InboxUiState.Content

        assertThat(state.mailbox.id).isEqualTo(UNIFIED)
        assertThat(state.messages.map { it.id }).containsExactly("w1", "h1")
        assertThat(state.canSwitchMailbox).isTrue()
    }

    @Test
    fun `shows the selected account's inbox`() = runTest {
        publishTwoAccounts()
        val viewModel = createViewModel()
        stateOf(viewModel)

        selectedMailbox.select("work")

        val state = stateOf(viewModel) as InboxUiState.Content
        assertThat(state.mailbox.id).isEqualTo("work")
        assertThat(state.messages.map { it.id }).containsExactly("w1")
    }

    @Test
    fun `falls back to the unified inbox when the selected account was removed`() = runTest {
        selectedMailbox.select("removed-account")
        publishTwoAccounts()
        val viewModel = createViewModel()

        val state = stateOf(viewModel) as InboxUiState.Content

        assertThat(state.mailbox.id).isEqualTo(UNIFIED)
    }

    @Test
    fun `starting and exiting the demo toggles demo mode`() = runTest {
        val viewModel = createViewModel()

        viewModel.startDemo()
        assertThat(demoMode.isEnabled.value).isTrue()

        viewModel.exitDemo()
        assertThat(demoMode.isEnabled.value).isFalse()
    }

    @Test
    fun `mailbox can't be switched with a single mailbox`() = runTest {
        phone.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = mapOf(UNIFIED to emptyList()))
        val viewModel = createViewModel()

        val state = stateOf(viewModel) as InboxUiState.Content

        assertThat(state.canSwitchMailbox).isFalse()
    }

    @Test
    fun `messages of several accounts show which account they belong to`() = runTest {
        publishTwoAccounts()
        val viewModel = createViewModel()

        val unified = stateOf(viewModel) as InboxUiState.Content
        assertThat(unified.accounts.keys).containsExactlyInAnyOrder("work", "home")

        selectedMailbox.select("work")
        val account = stateOf(viewModel) as InboxUiState.Content
        assertThat(account.accounts).isEmpty()
    }

    @Test
    fun `shows the unread view`() = runTest {
        selectedMailbox.select(UNREAD)
        publishTwoAccounts()
        val viewModel = createViewModel()

        val state = stateOf(viewModel) as InboxUiState.Content

        assertThat(state.mailbox.id).isEqualTo(UNREAD)
        assertThat(state.messages.map { it.id }).containsExactly("h1")
    }

    @Test
    fun `archived message disappears right away`() = runTest {
        publishTwoAccounts()
        val viewModel = createViewModel()
        stateOf(viewModel)

        viewModel.archive("w1")

        val state = stateOf(viewModel) as InboxUiState.Content
        assertThat(phone.performedActions).containsExactly("w1" to WearMessageAction.ARCHIVE)
        assertThat(state.messages.map { it.id }).containsExactly("h1")
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `message comes back with an error if it couldn't be archived`() = runTest {
        phone.actionResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        publishTwoAccounts()
        val viewModel = createViewModel()
        stateOf(viewModel)

        viewModel.archive("w1")

        val state = stateOf(viewModel) as InboxUiState.Content
        assertThat(state.messages.map { it.id }).containsExactly("w1", "h1")
        assertThat(state.errorMessage).isEqualTo(R.string.error_archive_unavailable)
    }

    @Test
    fun `deleted message disappears right away`() = runTest {
        publishTwoAccounts()
        val viewModel = createViewModel()
        stateOf(viewModel)

        viewModel.delete("h1")

        val state = stateOf(viewModel) as InboxUiState.Content
        assertThat(phone.performedActions).containsExactly("h1" to WearMessageAction.DELETE)
        assertThat(state.messages.map { it.id }).containsExactly("w1")
    }

    @Test
    fun `mark all read is sent for the mailbox`() = runTest {
        publishTwoAccounts()
        val viewModel = createViewModel()
        stateOf(viewModel)

        viewModel.markAllRead(UNIFIED)

        val state = stateOf(viewModel) as InboxUiState.Content
        assertThat(phone.markedAllRead).containsExactly(UNIFIED)
        assertThat(state.isMarkingAllRead).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `mark all read on an older phone asks to update Thunderbird`() = runTest {
        phone.markAllReadResult = PhoneResult.Failed(WearErrorReason.UNSUPPORTED_REQUEST)
        publishTwoAccounts()
        val viewModel = createViewModel()
        stateOf(viewModel)

        viewModel.markAllRead(UNIFIED)

        val state = stateOf(viewModel) as InboxUiState.Content
        assertThat(state.errorMessage).isEqualTo(R.string.error_update_phone_app)
    }

    private fun publishTwoAccounts() {
        phone.publish(
            mailboxes = listOf(
                mailbox(UNIFIED, unreadCount = 2),
                mailbox(UNREAD, unreadCount = 1),
                mailbox("work"),
                mailbox("home"),
            ),
            inboxes = mapOf(
                UNIFIED to listOf(message("w1", isRead = true, accountId = "work"), message("h1", accountId = "home")),
                UNREAD to listOf(message("h1", accountId = "home")),
                "work" to listOf(message("w1", isRead = true, accountId = "work")),
                "home" to listOf(message("h1", accountId = "home")),
            ),
        )
    }

    private fun createViewModel() = InboxViewModel(
        phoneConnection = phone,
        selectedMailboxStore = selectedMailbox,
        demoModeStore = demoMode,
    )

    private fun TestScope.stateOf(viewModel: InboxViewModel): InboxUiState {
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        return viewModel.uiState.value
    }
}
