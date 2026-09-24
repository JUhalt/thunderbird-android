package net.thunderbird.wear.ui.reader

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.STARRED
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.UNREAD
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message

@OptIn(ExperimentalCoroutinesApi::class)
class MessageViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `opening an unread message marks it as read once`() = runTest {
        publish(message("m1", isRead = false))

        createViewModel("m1")
        advanceUntilIdle()
        publish(message("m1", isRead = false))
        advanceUntilIdle()

        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.MARK_READ)
    }

    @Test
    fun `opening a read message doesn't send anything`() = runTest {
        publish(message("m1", isRead = true))

        createViewModel("m1")
        advanceUntilIdle()

        assertThat(phone.performedActions).isEmpty()
    }

    @Test
    fun `message that isn't in the inbox anymore is reported as missing`() = runTest {
        publish(message("other"))

        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.message).isNull()
    }

    @Test
    fun `toggling star sends star or unstar depending on the current state`() = runTest {
        publish(message("m1", isRead = true, isStarred = true))
        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        viewModel.toggleStar()
        advanceUntilIdle()

        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.UNSTAR)
    }

    @Test
    fun `archiving closes the message screen`() = runTest {
        publish(message("m1", isRead = true))
        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        viewModel.archive()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isClosed).isTrue()
    }

    @Test
    fun `archiving without an archive folder shows an error and keeps the screen open`() = runTest {
        publish(message("m1", isRead = true))
        phone.actionResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        viewModel.archive()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isClosed).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_archive_unavailable)
    }

    @Test
    fun `opening on the phone shows the confirmation`() = runTest {
        publish(message("m1", isRead = true))
        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        viewModel.openOnPhone()
        advanceUntilIdle()

        assertThat(phone.openedOnPhone).containsExactly("m1")
        assertThat(viewModel.uiState.value.showOpenOnPhoneConfirmation).isTrue()
    }

    @Test
    fun `opening on the phone without a phone shows an error`() = runTest {
        publish(message("m1", isRead = true))
        phone.openOnPhoneResult = PhoneResult.NoPhone
        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        viewModel.openOnPhone()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.showOpenOnPhoneConfirmation).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_no_phone)
    }

    @Test
    fun `message opened from the unread view stays open after it's read`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(UNREAD)),
            inboxes = mapOf(UNIFIED to listOf(message("m1")), UNREAD to listOf(message("m1"))),
        )
        val viewModel = createViewModel("m1", mailboxId = UNREAD)
        advanceUntilIdle()

        // The phone applies MARK_READ: the message leaves the unread view but is still in the unified inbox.
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(UNREAD)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", isRead = true)), UNREAD to emptyList()),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.message?.isRead).isEqualTo(true)
    }

    @Test
    fun `old message opened from the starred view is kept after it's unstarred`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(STARRED)),
            inboxes = mapOf(UNIFIED to emptyList(), STARRED to listOf(message("m1", isRead = true, isStarred = true))),
        )
        val viewModel = createViewModel("m1", mailboxId = STARRED)
        advanceUntilIdle()

        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(STARRED)),
            inboxes = mapOf(UNIFIED to emptyList(), STARRED to emptyList()),
        )
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.message?.id).isEqualTo("m1")
    }

    @Test
    fun `account is shown when there are several accounts`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox("work", name = "Work"), mailbox("home", name = "Home")),
            inboxes = mapOf(UNIFIED to listOf(message("m1", isRead = true, accountId = "home"))),
        )
        val viewModel = createViewModel("m1")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.account?.name).isEqualTo("Home")
    }

    @Test
    fun `encrypted messages can't be replied to on the watch`() = runTest {
        publish(message("m1", isRead = true, isEncrypted = true), message("m2", isRead = true))

        val encrypted = createViewModel("m1")
        val plain = createViewModel("m2")
        advanceUntilIdle()

        assertThat(encrypted.uiState.value.canReply).isFalse()
        assertThat(plain.uiState.value.canReply).isTrue()
    }

    private fun publish(vararg messages: WearMessageSummary) {
        phone.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = mapOf(UNIFIED to messages.toList()))
    }

    private fun createViewModel(messageId: String, mailboxId: String = UNIFIED) = MessageViewModel(
        messageId = messageId,
        mailboxId = mailboxId,
        phoneConnection = phone,
    )
}
