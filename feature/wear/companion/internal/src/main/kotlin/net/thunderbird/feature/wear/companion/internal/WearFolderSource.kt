package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.first
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearResponse

/** Lists an account's folders and loads a folder's messages for the watch. Must be called off the main thread. */
internal interface WearFolderSource {
    /** Answers with [WearResponse.Folders]. */
    suspend fun loadFolders(accountId: String): WearResponse

    /** Answers with [WearResponse.Folder]. */
    suspend fun loadFolder(accountId: String, folderId: Long): WearResponse
}

/**
 * The folders the phone's folder list shows, in the same order and with the same names, except Drafts and the Outbox:
 * drafts are written and sent on the phone. Only these folders can be opened.
 */
@OptIn(ExperimentalTime::class)
@Suppress("LongParameterList")
internal class DisplayFolderWearFolderSource(
    private val accountManager: LegacyAccountManager,
    private val accountDtoManager: LegacyAccountDtoManager,
    private val displayFolderRepository: DisplayFolderRepository,
    private val folderName: (Folder) -> String,
    private val snapshotLoader: WearSnapshotLoader,
    private val clock: Clock,
) : WearFolderSource {

    override suspend fun loadFolders(accountId: String): WearResponse {
        val folders = loadWearFolders(accountId) ?: return WearResponse.Error(WearErrorReason.MAILBOX_NOT_FOUND)
        return WearResponse.Folders(folders.take(WearCompanion.MAX_FOLDERS))
    }

    override suspend fun loadFolder(accountId: String, folderId: Long): WearResponse {
        val account = accountManager.getAccounts().firstOrNull { it.uuid == accountId }
        val folder = loadWearFolders(accountId)?.firstOrNull { it.id == folderId }
        if (account == null || folder == null) return WearResponse.Error(WearErrorReason.MAILBOX_NOT_FOUND)

        val snapshot = snapshotLoader.load(
            mailboxId = WearCompanion.folderMailboxId(accountId, folderId),
            accounts = listOf(account),
            search = WearMailboxSearches.folder(accountId, folderId),
            generatedAt = clock.now().toEpochMilliseconds(),
        )
        return WearResponse.Folder(folder = folder, snapshot = snapshot)
    }

    /** The account's folders, or `null` if there is no such account. */
    private suspend fun loadWearFolders(accountId: String): List<WearFolder>? {
        val account = accountDtoManager.getAccount(accountId) ?: return null
        return displayFolderRepository.getDisplayFoldersFlow(account, includeHiddenFolders = false)
            .first()
            .mapNotNull { it.toWearFolder() }
    }

    private fun DisplayFolder.toWearFolder(): WearFolder? {
        val type = folder.type.toWearFolderType() ?: return null
        return WearFolder(
            id = folder.id,
            name = folderName(folder).take(WearCompanion.MAX_FOLDER_NAME_LENGTH),
            type = type,
            unreadCount = unreadMessageCount,
        )
    }
}

/** The folder's type on the watch, or `null` for Drafts and the Outbox, which the watch doesn't show. */
internal fun FolderType.toWearFolderType(): WearFolderType? = when (this) {
    FolderType.INBOX -> WearFolderType.INBOX
    FolderType.SENT -> WearFolderType.SENT
    FolderType.ARCHIVE -> WearFolderType.ARCHIVE
    FolderType.SPAM -> WearFolderType.SPAM
    FolderType.TRASH -> WearFolderType.TRASH
    FolderType.REGULAR -> WearFolderType.REGULAR
    FolderType.DRAFTS, FolderType.OUTBOX -> null
}
