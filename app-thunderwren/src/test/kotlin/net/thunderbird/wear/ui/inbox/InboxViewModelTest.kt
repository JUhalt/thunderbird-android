package net.thunderbird.wear.ui.inbox

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.doesNotContain
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.wear.R
import net.thunderbird.wear.data.DemoMode
import net.thunderbird.wear.data.FolderRef
import net.thunderbird.wear.data.MessageSwipeAction
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.testing.FakeDemoModeStore
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeSelectedMailboxStore
import net.thunderbird.wear.testing.FakeWatchSettingsStore
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.UNREAD
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message
import net.thunderbird.wear.ui.inbox.InboxContract.Effect
import net.thunderbird.wear.ui.inbox.InboxContract.Event
import net.thunderbird.wear.ui.inbox.InboxContract.State

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection()
    private val selectedMailbox = FakeSelectedMailboxStore()
    private val demoMode = FakeDemoModeStore()
    private val settings = FakeWatchSettingsStore()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `asks the phone for fresh data when opened`() = runTest {
        createTestSubject()
        advanceUntilIdle()

        assertThat(phone.refreshCount).isEqualTo(1)
    }

    @Test
    fun `shows not connected until the phone has published`() = runTest {
        val testSubject = createTestSubject()

        assertThat(stateOf(testSubject)).isInstanceOf(State.NotConnected::class)
    }

    @Test
    fun `shows the unified inbox by default`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.Content

        assertThat(state.mailbox.id).isEqualTo(UNIFIED)
        assertThat(state.messages.map { it.id }).containsExactly("w1", "h1")
        assertThat(state.canSwitchMailbox).isTrue()
    }

    @Test
    fun `shows the selected account's inbox`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        selectedMailbox.select("work")

        val state = stateOf(testSubject) as State.Content
        assertThat(state.mailbox.id).isEqualTo("work")
        assertThat(state.messages.map { it.id }).containsExactly("w1")
    }

    @Test
    fun `falls back to the unified inbox when the selected account was removed`() = runTest {
        selectedMailbox.select("removed-account")
        publishTwoAccounts()
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.Content

        assertThat(state.mailbox.id).isEqualTo(UNIFIED)
    }

    @Test
    fun `starting and exiting the demo toggles demo mode`() = runTest {
        val testSubject = createTestSubject()

        testSubject.event(Event.StartDemoClicked)
        assertThat(demoMode.mode.value).isEqualTo(DemoMode.UNTIL_PHONE_CONNECTS)

        testSubject.event(Event.ExitDemoClicked)
        assertThat(demoMode.mode.value).isEqualTo(DemoMode.OFF)
    }

    @Test
    fun `mailbox can't be switched with a single mailbox`() = runTest {
        phone.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = mapOf(UNIFIED to emptyList()))
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.Content

        assertThat(state.canSwitchMailbox).isFalse()
    }

    @Test
    fun `messages of several accounts show which account they belong to`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()

        val unified = stateOf(testSubject) as State.Content
        assertThat(unified.accounts.keys).containsExactlyInAnyOrder("work", "home")

        selectedMailbox.select("work")
        val account = stateOf(testSubject) as State.Content
        assertThat(account.accounts).isEmpty()
    }

    @Test
    fun `shows the unread view`() = runTest {
        selectedMailbox.select(UNREAD)
        publishTwoAccounts()
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.Content

        assertThat(state.mailbox.id).isEqualTo(UNREAD)
        assertThat(state.messages.map { it.id }).containsExactly("h1")
    }

    @Test
    fun `archived message disappears right away`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.ArchiveClicked("w1"))

        val state = stateOf(testSubject) as State.Content
        assertThat(phone.performedActions).containsExactly("w1" to WearMessageAction.ARCHIVE)
        assertThat(state.messages.map { it.id }).containsExactly("h1")
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `message comes back with an error if it couldn't be archived`() = runTest {
        phone.actionResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.ArchiveClicked("w1"))

        val state = stateOf(testSubject) as State.Content
        assertThat(state.messages.map { it.id }).containsExactly("w1", "h1")
        assertThat(state.errorMessage).isEqualTo(R.string.error_archive_unavailable)
    }

    @Test
    fun `deleted message disappears right away`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.DeleteClicked("h1"))

        val state = stateOf(testSubject) as State.Content
        assertThat(phone.performedActions).containsExactly("h1" to WearMessageAction.DELETE)
        assertThat(state.messages.map { it.id }).containsExactly("w1")
    }

    @Test
    fun `mark all read is sent for the mailbox`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.MarkAllReadConfirmed(UNIFIED))

        val state = stateOf(testSubject) as State.Content
        assertThat(phone.markedAllRead).containsExactly(UNIFIED)
        assertThat(state.isMarkingAllRead).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `mark all read on an older phone asks to update Thunderbird`() = runTest {
        phone.markAllReadResult = PhoneResult.Failed(WearErrorReason.UNSUPPORTED_REQUEST)
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.MarkAllReadConfirmed(UNIFIED))

        val state = stateOf(testSubject) as State.Content
        assertThat(state.errorMessage).isEqualTo(R.string.error_update_phone_app)
    }

    @Test
    fun `tapping the mailbox opens the mailbox picker`() = runTest {
        val testSubject = createTestSubject()

        testSubject.effect.test {
            testSubject.event(Event.MailboxClicked)

            assertThat(awaitItem()).isEqualTo(Effect.OpenMailboxes)
        }
    }

    @Test
    fun `tapping a message opens it from the current mailbox`() = runTest {
        selectedMailbox.select(UNREAD)
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.effect.test {
            testSubject.event(Event.MessageClicked("h1"))

            assertThat(awaitItem()).isEqualTo(Effect.OpenMessage(mailboxId = UNREAD, messageId = "h1"))
        }
    }

    @Test
    fun `tells why refreshing failed when the companion is turned off on the phone`() = runTest {
        phone.refreshResult = PhoneResult.Failed(WearErrorReason.COMPANION_DISABLED)
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.NotConnected

        assertThat(state.errorMessage).isEqualTo(R.string.error_companion_disabled)
    }

    @Test
    fun `failures of the refresh when the app opens aren't shown`() = runTest {
        phone.refreshResult = PhoneResult.NoPhone
        publishTwoAccounts()
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.Content

        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `failures of a refresh the user asked for are shown`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)
        phone.refreshResult = PhoneResult.NoPhone

        testSubject.event(Event.RefreshClicked)
        val state = stateOf(testSubject) as State.Content

        assertThat(state.errorMessage).isEqualTo(R.string.error_no_phone)
    }

    @Test
    fun `shows the swipe and preview settings`() = runTest {
        settings.update {
            it.copy(
                swipeLeft = MessageSwipeAction.DELETE,
                swipeRight = MessageSwipeAction.ARCHIVE,
                showPreviews = false,
            )
        }
        publishTwoAccounts()
        val testSubject = createTestSubject()

        val state = stateOf(testSubject) as State.Content

        assertThat(state.swipeLeft).isEqualTo(MessageSwipeAction.DELETE)
        assertThat(state.swipeRight).isEqualTo(MessageSwipeAction.ARCHIVE)
        assertThat(state.showPreviews).isFalse()
    }

    @Test
    fun `deleting waits for the confirmation when that's turned on`() = runTest {
        settings.update { it.copy(confirmDelete = true) }
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.DeleteClicked("w1"))
        val asking = stateOf(testSubject) as State.Content

        assertThat(asking.pendingDeleteMessageId).isEqualTo("w1")
        assertThat(phone.performedActions).isEmpty()

        testSubject.event(Event.DeleteConfirmed)
        val deleted = stateOf(testSubject) as State.Content

        assertThat(deleted.pendingDeleteMessageId).isNull()
        assertThat(phone.performedActions).containsExactly("w1" to WearMessageAction.DELETE)
        assertThat(deleted.messages.map { it.id }).doesNotContain("w1")
    }

    @Test
    fun `dismissing the delete confirmation keeps the message`() = runTest {
        settings.update { it.copy(confirmDelete = true) }
        publishTwoAccounts()
        val testSubject = createTestSubject()
        stateOf(testSubject)

        testSubject.event(Event.DeleteClicked("w1"))
        testSubject.event(Event.DeleteDismissed)
        val state = stateOf(testSubject) as State.Content

        assertThat(state.pendingDeleteMessageId).isNull()
        assertThat(phone.performedActions).isEmpty()
        assertThat(state.messages.map { it.id }).contains("w1")
    }

    @Test
    fun `settings button opens the settings`() = runTest {
        publishTwoAccounts()
        val testSubject = createTestSubject()

        testSubject.effect.test {
            testSubject.event(Event.SettingsClicked)

            assertThat(awaitItem()).isEqualTo(Effect.OpenSettings)
        }
    }

    @Test
    fun `a folder is loaded when opened and shown like its account's inbox`() = runTest {
        publishWorkFolders()
        val testSubject = createTestSubject(folder = RECEIPTS)

        val state = stateOf(testSubject) as State.Content

        assertThat(phone.loadedFolderRequests).containsExactly(RECEIPTS)
        assertThat(phone.refreshCount).isEqualTo(0)
        assertThat(state.mailbox.id).isEqualTo(RECEIPTS.mailboxId)
        assertThat(state.mailbox.name).isEqualTo("Receipts")
        assertThat(state.mailbox.monogram).isEqualTo("WO")
        assertThat(state.mailbox.unreadCount).isEqualTo(1)
        assertThat(state.messages.map { it.id }).containsExactly("r1", "r2")
        assertThat(state.folderType).isEqualTo(WearFolderType.REGULAR)
    }

    @Test
    fun `a folder shows loading until its messages arrive`() = runTest {
        publishWorkFolders()
        val testSubject = createTestSubject(folder = RECEIPTS)

        // Nothing has run yet, so the folder is still being loaded.
        assertThat(testSubject.state.value).isEqualTo(State.Loading)

        assertThat(stateOf(testSubject)).isInstanceOf(State.Content::class)
    }

    @Test
    fun `a folder that couldn't be loaded says why`() = runTest {
        publishWorkFolders()
        phone.loadFolders("work")
        phone.loadFolderResult = PhoneResult.NoPhone
        val testSubject = createTestSubject(folder = RECEIPTS)

        val state = stateOf(testSubject) as State.Content

        assertThat(state.messages).isEmpty()
        assertThat(state.errorMessage).isEqualTo(R.string.error_no_phone)
    }

    @Test
    fun `refresh loads the folder again`() = runTest {
        publishWorkFolders()
        val testSubject = createTestSubject(folder = RECEIPTS)
        stateOf(testSubject)

        testSubject.event(Event.RefreshClicked)
        advanceUntilIdle()

        assertThat(phone.loadedFolderRequests).containsExactly(RECEIPTS, RECEIPTS)
    }

    @Test
    fun `archived messages in a folder disappear from it`() = runTest {
        publishWorkFolders()
        val testSubject = createTestSubject(folder = RECEIPTS)
        stateOf(testSubject)

        testSubject.event(Event.ArchiveClicked("r1"))

        val state = stateOf(testSubject) as State.Content
        assertThat(state.messages.map { it.id }).containsExactly("r2")
        assertThat(state.mailbox.unreadCount).isEqualTo(0)
    }

    @Test
    fun `archiving in the Archive folder says the message is archived already`() = runTest {
        publishWorkFolders()
        phone.actionResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        val testSubject = createTestSubject(folder = ARCHIVE)
        stateOf(testSubject)

        testSubject.event(Event.ArchiveClicked("a1"))

        val state = stateOf(testSubject) as State.Content
        assertThat(state.errorMessage).isEqualTo(R.string.error_already_archived)
        assertThat(state.messages.map { it.id }).containsExactly("a1")
    }

    @Test
    fun `mark all read in a folder marks that folder`() = runTest {
        publishWorkFolders()
        val testSubject = createTestSubject(folder = RECEIPTS)
        stateOf(testSubject)

        testSubject.event(Event.MarkAllReadConfirmed(RECEIPTS.mailboxId))
        advanceUntilIdle()

        assertThat(phone.markedAllRead).containsExactly(RECEIPTS.mailboxId)
    }

    private fun publishWorkFolders() {
        phone.publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("work", name = "Work")), inboxes = emptyMap())
        phone.phoneFolders["work"] = listOf(
            WearFolder(id = RECEIPTS.folderId, name = "Receipts", type = WearFolderType.REGULAR, unreadCount = 1),
            WearFolder(id = ARCHIVE.folderId, name = "Archive", type = WearFolderType.ARCHIVE, unreadCount = 0),
        )
        phone.phoneFolderMessages[RECEIPTS] = listOf(
            message("r1", accountId = "work"),
            message("r2", isRead = true, accountId = "work"),
        )
        phone.phoneFolderMessages[ARCHIVE] = listOf(message("a1", isRead = true, accountId = "work"))
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

    private fun createTestSubject(folder: FolderRef? = null) = InboxViewModel(
        phoneConnection = phone,
        selectedMailboxStore = selectedMailbox,
        demoModeStore = demoMode,
        settingsStore = settings,
        folder = folder,
    )

    private companion object {
        val RECEIPTS = FolderRef(accountId = "work", folderId = 5)
        val ARCHIVE = FolderRef(accountId = "work", folderId = 3)
    }

    private fun TestScope.stateOf(testSubject: InboxViewModel): State {
        advanceUntilIdle()
        return testSubject.state.value
    }
}
