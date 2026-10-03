package net.thunderbird.wear.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary

/** A folder of the account with [accountId]. */
data class FolderRef(val accountId: String, val folderId: Long) {
    /** The folder's [net.thunderbird.feature.wear.companion.WearMailbox.id]. */
    val mailboxId: String
        get() = WearCompanion.folderMailboxId(accountId, folderId)
}

/**
 * Folders and their messages, loaded from the phone when asked for. Unlike the inboxes, the phone doesn't publish
 * them, so they're only kept in memory, and loaded again when a folder is opened.
 *
 * Actions the phone has done are applied here too, so open folders show them without loading them again.
 */
class LoadedFolders {
    /** The folder lists by account ID. */
    private val folderLists = MutableStateFlow<Map<String, List<WearFolder>>>(emptyMap())

    /** The opened folders and their messages by folder mailbox ID. */
    private val openedFolders = MutableStateFlow<Map<String, WearFolder>>(emptyMap())
    private val snapshots = MutableStateFlow<Map<String, WearInboxSnapshot>>(emptyMap())

    fun folders(accountId: String): Flow<List<WearFolder>?> = folderLists.map { it[accountId] }.distinctUntilChanged()

    /** The folder with the folder mailbox ID [mailboxId], or `null` if it hasn't been loaded. */
    fun folder(mailboxId: String): Flow<WearFolder?> {
        val (accountId, folderId) = WearCompanion.parseFolderMailboxId(mailboxId) ?: return flowOf(null)
        return combine(openedFolders, folderLists) { opened, lists ->
            opened[mailboxId] ?: lists[accountId]?.firstOrNull { it.id == folderId }
        }.distinctUntilChanged()
    }

    /** The messages of the folder with the folder mailbox ID [mailboxId], or `null` if they haven't been loaded. */
    fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> = snapshots.map { it[mailboxId] }.distinctUntilChanged()

    fun putFolders(accountId: String, folders: List<WearFolder>) {
        folderLists.update { it + (accountId to folders) }
    }

    fun putFolder(accountId: String, folder: WearFolder, snapshot: WearInboxSnapshot) {
        val mailboxId = WearCompanion.folderMailboxId(accountId, folder.id)
        openedFolders.update { it + (mailboxId to folder) }
        snapshots.update { it + (mailboxId to snapshot.copy(mailboxId = mailboxId)) }
        // Keep the folder list's unread count in step with what was just loaded.
        setUnreadCount(mailboxId, folder.unreadCount)
    }

    /** Applies an [action] the phone has done to the message with [messageId], wherever it was loaded. */
    fun apply(messageId: String, action: WearMessageAction) {
        updateSnapshots { snapshot ->
            if (snapshot.messages.none { it.id == messageId }) return@updateSnapshots snapshot

            val messages = when (action) {
                WearMessageAction.ARCHIVE, WearMessageAction.DELETE -> snapshot.messages.filterNot {
                    it.id == messageId
                }

                else -> snapshot.messages.map { if (it.id == messageId) it.apply(action) else it }
            }
            snapshot.withMessages(messages)
        }
    }

    /** Marks everything in the folder with the folder mailbox ID [mailboxId] as read, as the phone has done. */
    fun markAllRead(mailboxId: String) {
        updateSnapshots { snapshot ->
            if (snapshot.mailboxId == mailboxId) {
                snapshot.copy(unreadCount = 0, messages = snapshot.messages.map { it.copy(isRead = true) })
            } else {
                snapshot
            }
        }
    }

    /** Forgets everything, for example because the companion was turned off on the phone. */
    fun clear() {
        folderLists.value = emptyMap()
        openedFolders.value = emptyMap()
        snapshots.value = emptyMap()
    }

    private fun updateSnapshots(transform: (WearInboxSnapshot) -> WearInboxSnapshot) {
        val changed = mutableListOf<WearInboxSnapshot>()
        snapshots.update { snapshots ->
            changed.clear()
            snapshots.mapValues { (_, snapshot) -> transform(snapshot).also { if (it != snapshot) changed += it } }
        }
        for (snapshot in changed) setUnreadCount(snapshot.mailboxId, snapshot.unreadCount)
    }

    private fun setUnreadCount(mailboxId: String, unreadCount: Int) {
        val (accountId, folderId) = WearCompanion.parseFolderMailboxId(mailboxId) ?: return
        openedFolders.update { opened ->
            opened[mailboxId]?.let { opened + (mailboxId to it.copy(unreadCount = unreadCount)) } ?: opened
        }
        folderLists.update { lists ->
            val folders = lists[accountId] ?: return@update lists
            lists + (accountId to folders.map { if (it.id == folderId) it.copy(unreadCount = unreadCount) else it })
        }
    }
}

/**
 * The snapshot with [messages] instead of its own. Its unread count changes by as much as the unread messages did,
 * because it also counts unread messages beyond the newest ones it contains.
 */
private fun WearInboxSnapshot.withMessages(messages: List<WearMessageSummary>): WearInboxSnapshot {
    val unreadChange = messages.count { !it.isRead } - this.messages.count { !it.isRead }
    return copy(messages = messages, unreadCount = (unreadCount + unreadChange).coerceAtLeast(0))
}

/** The message after [action], for the actions that keep it in its folder. */
internal fun WearMessageSummary.apply(action: WearMessageAction): WearMessageSummary = when (action) {
    WearMessageAction.MARK_READ -> copy(isRead = true)
    WearMessageAction.MARK_UNREAD -> copy(isRead = false)
    WearMessageAction.STAR -> copy(isStarred = true)
    WearMessageAction.UNSTAR -> copy(isStarred = false)
    WearMessageAction.ARCHIVE, WearMessageAction.DELETE -> this
}
