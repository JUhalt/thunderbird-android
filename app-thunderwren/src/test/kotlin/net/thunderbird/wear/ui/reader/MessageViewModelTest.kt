package net.thunderbird.wear.ui.reader

import app.cash.turbine.test
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
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.BodyResult
import net.thunderbird.wear.data.FolderRef
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeWatchSettingsStore
import net.thunderbird.wear.testing.STARRED
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.UNREAD
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message
import net.thunderbird.wear.ui.reader.MessageContract.Effect
import net.thunderbird.wear.ui.reader.MessageContract.Event

@OptIn(ExperimentalCoroutinesApi::class)
class MessageViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection()
    private val settings = FakeWatchSettingsStore()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `opening an unread message marks it as read once`() = runTest {
        publish(message("m1", isRead = false))

        createTestSubject("m1")
        advanceUntilIdle()
        publish(message("m1", isRead = false))
        advanceUntilIdle()

        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.MARK_READ)
    }

    @Test
    fun `opening a read message doesn't send anything`() = runTest {
        publish(message("m1", isRead = true))

        createTestSubject("m1")
        advanceUntilIdle()

        assertThat(phone.performedActions).isEmpty()
    }

    @Test
    fun `opening a message doesn't mark it as read when that's turned off`() = runTest {
        settings.update { it.copy(markAsReadWhenOpened = false) }
        publish(message("m1", isRead = false))

        createTestSubject("m1")
        advanceUntilIdle()

        assertThat(phone.performedActions).isEmpty()
    }

    @Test
    fun `deleting asks first when confirmation is turned on`() = runTest {
        settings.update { it.copy(confirmDelete = true) }
        publish(message("m1", isRead = true))
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.event(Event.DeleteClicked)
        advanceUntilIdle()

        assertThat(testSubject.state.value.showDeleteConfirmation).isTrue()
        assertThat(phone.performedActions).isEmpty()

        testSubject.effect.test {
            testSubject.event(Event.DeleteConfirmed)

            assertThat(awaitItem()).isEqualTo(Effect.Close)
        }
        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.DELETE)
        assertThat(testSubject.state.value.showDeleteConfirmation).isFalse()
    }

    @Test
    fun `dismissing the delete confirmation keeps the message`() = runTest {
        settings.update { it.copy(confirmDelete = true) }
        publish(message("m1", isRead = true))
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.event(Event.DeleteClicked)
        testSubject.event(Event.DeleteDismissed)
        advanceUntilIdle()

        assertThat(testSubject.state.value.showDeleteConfirmation).isFalse()
        assertThat(phone.performedActions).isEmpty()
    }

    @Test
    fun `opening a message loads its whole text from the phone`() = runTest {
        phone.bodyResult = BodyResult.Loaded(text = "Hello,\n\nthe whole text.", isComplete = true)
        publish(message("m1", isRead = true))

        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        assertThat(phone.loadedBodies).containsExactly("m1")
        assertThat(testSubject.state.value.body).isEqualTo("Hello,\n\nthe whole text.")
        assertThat(testSubject.state.value.isLoadingBody).isFalse()
        assertThat(testSubject.state.value.isBodyIncomplete).isFalse()
    }

    @Test
    fun `partial text from the phone is marked as incomplete`() = runTest {
        phone.bodyResult = BodyResult.Loaded(text = "The beginning", isComplete = false)
        publish(message("m1", isRead = true))

        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        assertThat(testSubject.state.value.body).isEqualTo("The beginning")
        assertThat(testSubject.state.value.isBodyIncomplete).isTrue()
    }

    @Test
    fun `preview stays when the whole text can't be loaded`() = runTest {
        phone.bodyResult = BodyResult.Failed(PhoneResult.NoPhone)
        publish(message("m1", isRead = true))

        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        assertThat(testSubject.state.value.body).isNull()
        assertThat(testSubject.state.value.isBodyUnavailable).isTrue()
    }

    @Test
    fun `encrypted messages aren't loaded`() = runTest {
        publish(message("m1", isRead = true, isEncrypted = true))

        createTestSubject("m1")
        advanceUntilIdle()

        assertThat(phone.loadedBodies).isEmpty()
    }

    @Test
    fun `message that isn't in the inbox anymore is reported as missing`() = runTest {
        publish(message("other"))

        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        assertThat(testSubject.state.value.isLoading).isFalse()
        assertThat(testSubject.state.value.message).isNull()
    }

    @Test
    fun `toggling star sends star or unstar depending on the current state`() = runTest {
        publish(message("m1", isRead = true, isStarred = true))
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.event(Event.ToggleStarClicked)
        advanceUntilIdle()

        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.UNSTAR)
    }

    @Test
    fun `archiving closes the message screen`() = runTest {
        publish(message("m1", isRead = true))
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.effect.test {
            testSubject.event(Event.ArchiveClicked)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(Effect.Close)
        }
    }

    @Test
    fun `archiving without an archive folder shows an error and keeps the screen open`() = runTest {
        publish(message("m1", isRead = true))
        phone.actionResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.effect.test {
            testSubject.event(Event.ArchiveClicked)
            advanceUntilIdle()

            expectNoEvents()
        }
        assertThat(testSubject.state.value.errorMessage).isEqualTo(R.string.error_archive_unavailable)
    }

    @Test
    fun `opening on the phone shows the confirmation`() = runTest {
        publish(message("m1", isRead = true))
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.event(Event.OpenOnPhoneClicked)
        advanceUntilIdle()

        assertThat(phone.openedOnPhone).containsExactly("m1")
        assertThat(testSubject.state.value.showOpenOnPhoneConfirmation).isTrue()
    }

    @Test
    fun `opening on the phone without a phone shows an error`() = runTest {
        publish(message("m1", isRead = true))
        phone.openOnPhoneResult = PhoneResult.NoPhone
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        testSubject.event(Event.OpenOnPhoneClicked)
        advanceUntilIdle()

        assertThat(testSubject.state.value.showOpenOnPhoneConfirmation).isFalse()
        assertThat(testSubject.state.value.errorMessage).isEqualTo(R.string.error_no_phone)
    }

    @Test
    fun `message opened from the unread view stays open after it's read`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(UNREAD)),
            inboxes = mapOf(UNIFIED to listOf(message("m1")), UNREAD to listOf(message("m1"))),
        )
        val testSubject = createTestSubject("m1", mailboxId = UNREAD)
        advanceUntilIdle()

        // The phone applies MARK_READ: the message leaves the unread view but is still in the unified inbox.
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(UNREAD)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", isRead = true)), UNREAD to emptyList()),
        )
        advanceUntilIdle()

        assertThat(testSubject.state.value.message?.isRead).isEqualTo(true)
    }

    @Test
    fun `old message opened from the starred view is kept after it's unstarred`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(STARRED)),
            inboxes = mapOf(UNIFIED to emptyList(), STARRED to listOf(message("m1", isRead = true, isStarred = true))),
        )
        val testSubject = createTestSubject("m1", mailboxId = STARRED)
        advanceUntilIdle()

        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox(STARRED)),
            inboxes = mapOf(UNIFIED to emptyList(), STARRED to emptyList()),
        )
        advanceUntilIdle()

        assertThat(testSubject.state.value.message?.id).isEqualTo("m1")
    }

    @Test
    fun `account is shown when there are several accounts`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED), mailbox("work", name = "Work"), mailbox("home", name = "Home")),
            inboxes = mapOf(UNIFIED to listOf(message("m1", isRead = true, accountId = "home"))),
        )
        val testSubject = createTestSubject("m1")
        advanceUntilIdle()

        assertThat(testSubject.state.value.account?.name).isEqualTo("Home")
    }

    @Test
    fun `encrypted messages can't be replied to on the watch`() = runTest {
        publish(message("m1", isRead = true, isEncrypted = true), message("m2", isRead = true))

        val encrypted = createTestSubject("m1")
        val plain = createTestSubject("m2")
        advanceUntilIdle()

        assertThat(encrypted.state.value.canReply).isFalse()
        assertThat(plain.state.value.canReply).isTrue()
    }

    @Test
    fun `reply opens the reply screen for the same mailbox`() = runTest {
        publish(message("m1", isRead = true))
        val testSubject = createTestSubject("m1", mailboxId = UNIFIED)
        advanceUntilIdle()

        testSubject.effect.test {
            testSubject.event(Event.ReplyClicked)

            assertThat(awaitItem()).isEqualTo(Effect.OpenReply(mailboxId = UNIFIED, messageId = "m1"))
        }
    }

    private fun publish(vararg messages: WearMessageSummary) {
        phone.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = mapOf(UNIFIED to messages.toList()))
    }

    @Test
    fun `a message opened from the Archive folder can't be archived again`() = runTest {
        val archive = FolderRef(accountId = "work", folderId = 3)
        phone.publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("work")), inboxes = emptyMap())
        phone.phoneFolders["work"] = listOf(
            WearFolder(id = archive.folderId, name = "Archive", type = WearFolderType.ARCHIVE, unreadCount = 0),
        )
        phone.phoneFolderMessages[archive] = listOf(message("a1", isRead = true, accountId = "work"))
        phone.loadFolder(archive)

        val testSubject = createTestSubject("a1", mailboxId = archive.mailboxId)
        advanceUntilIdle()

        assertThat(testSubject.state.value.message?.id).isEqualTo("a1")
        assertThat(testSubject.state.value.canArchive).isFalse()
    }

    private fun createTestSubject(messageId: String, mailboxId: String = UNIFIED) = MessageViewModel(
        messageId = messageId,
        mailboxId = mailboxId,
        phoneConnection = phone,
        settingsStore = settings,
    )
}
