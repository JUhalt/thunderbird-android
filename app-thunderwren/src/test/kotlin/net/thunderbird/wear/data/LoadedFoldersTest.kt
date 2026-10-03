package net.thunderbird.wear.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.wear.testing.message

class LoadedFoldersTest {
    private val testSubject = LoadedFolders()
    private val receipts = FolderRef(accountId = "work", folderId = 5)

    @Test
    fun `nothing is known until it has been loaded`() = runTest {
        assertThat(testSubject.folders("work").first()).isNull()
        assertThat(testSubject.folder(receipts.mailboxId).first()).isNull()
        assertThat(testSubject.inbox(receipts.mailboxId).first()).isNull()
    }

    @Test
    fun `a loaded folder can be found by its mailbox ID`() = runTest {
        loadReceipts()

        assertThat(testSubject.folder(receipts.mailboxId).first()?.name).isEqualTo("Receipts")
        assertThat(testSubject.inbox(receipts.mailboxId).first()?.messages?.map { it.id }).isEqualTo(listOf("r1", "r2"))
    }

    @Test
    fun `reading a message lowers the unread counts of its folder`() = runTest {
        testSubject.putFolders("work", listOf(RECEIPTS.copy(unreadCount = 7)))
        loadReceipts()

        testSubject.apply("r1", WearMessageAction.MARK_READ)

        val snapshot = testSubject.inbox(receipts.mailboxId).first()!!
        assertThat(snapshot.messages.first { it.id == "r1" }.isRead).isTrue()
        // Unread messages beyond the loaded ones still count.
        assertThat(snapshot.unreadCount).isEqualTo(RECEIPTS.unreadCount - 1)
        assertThat(testSubject.folder(receipts.mailboxId).first()?.unreadCount).isEqualTo(RECEIPTS.unreadCount - 1)
        assertThat(testSubject.folders("work").first()?.single()?.unreadCount).isEqualTo(RECEIPTS.unreadCount - 1)
    }

    @Test
    fun `archived and deleted messages leave the folder`() = runTest {
        loadReceipts()

        testSubject.apply("r1", WearMessageAction.ARCHIVE)
        testSubject.apply("r2", WearMessageAction.DELETE)

        assertThat(testSubject.inbox(receipts.mailboxId).first()?.messages).isEqualTo(emptyList())
        assertThat(testSubject.inbox(receipts.mailboxId).first()?.unreadCount).isEqualTo(RECEIPTS.unreadCount - 1)
    }

    @Test
    fun `mark all read clears the folder's unread count`() = runTest {
        loadReceipts()

        testSubject.markAllRead(receipts.mailboxId)

        val snapshot = testSubject.inbox(receipts.mailboxId).first()!!
        assertThat(snapshot.unreadCount).isEqualTo(0)
        assertThat(snapshot.messages.all { it.isRead }).isTrue()
        assertThat(testSubject.folder(receipts.mailboxId).first()?.unreadCount).isEqualTo(0)
    }

    @Test
    fun `clear forgets everything`() = runTest {
        testSubject.putFolders("work", listOf(RECEIPTS))
        loadReceipts()

        testSubject.clear()

        assertThat(testSubject.folders("work").first()).isNull()
        assertThat(testSubject.inbox(receipts.mailboxId).first()).isNull()
    }

    private fun loadReceipts() {
        testSubject.putFolder(
            accountId = "work",
            folder = RECEIPTS,
            snapshot = WearInboxSnapshot(
                mailboxId = receipts.mailboxId,
                generatedAt = 1,
                unreadCount = RECEIPTS.unreadCount,
                messages = listOf(message("r1"), message("r2", isRead = true)),
            ),
        )
    }

    private companion object {
        val RECEIPTS = WearFolder(id = 5, name = "Receipts", type = WearFolderType.REGULAR, unreadCount = 3)
    }
}
