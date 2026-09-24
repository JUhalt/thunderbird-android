package net.thunderbird.wear.ui.inbox

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
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
import net.thunderbird.wear.testing.FakeDemoModeStore
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeSelectedMailboxStore
import net.thunderbird.wear.testing.UNIFIED
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

    private fun publishTwoAccounts() {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED, unreadCount = 2), mailbox("work"), mailbox("home")),
            inboxes = mapOf(
                UNIFIED to listOf(message("w1"), message("h1")),
                "work" to listOf(message("w1")),
                "home" to listOf(message("h1")),
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
