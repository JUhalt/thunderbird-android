package net.thunderbird.wear.data

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction

/** The watch's view of Thunderbird on the paired phone. See RFC 0010. */
interface PhoneConnection {
    /** The last mailbox list the phone published, or `null` if none has arrived yet. */
    val mailboxes: Flow<WearMailboxList?>

    /** Whether the data comes from the built-in demo mailbox instead of a phone. */
    val isDemo: Flow<Boolean>

    /**
     * The last inbox snapshot the phone published for [mailboxId], or `null` if none has arrived yet. For a folder's
     * [WearCompanion.folderMailboxId], the messages last loaded with [loadFolder].
     */
    fun inbox(mailboxId: String): Flow<WearInboxSnapshot?>

    /** The folders of the account with [accountId] last loaded with [loadFolders], or `null` until then. */
    fun folders(accountId: String): Flow<List<WearFolder>?>

    /** The folder with the [WearCompanion.folderMailboxId] [mailboxId], or `null` if it hasn't been loaded. */
    fun folder(mailboxId: String): Flow<WearFolder?>

    /** Asks the phone for the folders of the account with [accountId]. [folders] has them afterwards. */
    suspend fun loadFolders(accountId: String): PhoneResult

    /** Asks the phone for the newest messages in [folder]. [inbox] has them afterwards. */
    suspend fun loadFolder(folder: FolderRef): PhoneResult

    /** Asks the phone to publish fresh data. */
    suspend fun refresh(): PhoneResult

    suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult

    /** Marks every message in the mailbox with [mailboxId] as read. */
    suspend fun markAllRead(mailboxId: String): PhoneResult

    /** Asks the phone to send [text] as a reply to the sender of the message with [messageId]. */
    suspend fun reply(messageId: String, text: String): PhoneResult

    /** Opens the message on the phone so it can be read in full or replied to. */
    suspend fun openOnPhone(messageId: String): PhoneResult

    /** Asks the phone for the whole text of the message with [messageId]. */
    suspend fun loadBody(messageId: String): BodyResult
}

sealed interface BodyResult {
    /** [isComplete] is `false` if the phone could only send part of the text. */
    data class Loaded(val text: String, val isComplete: Boolean) : BodyResult

    data class Failed(val result: PhoneResult) : BodyResult
}

sealed interface PhoneResult {
    data object Success : PhoneResult

    /** No phone with Thunderbird is reachable right now. */
    data object NoPhone : PhoneResult

    data class Failed(val reason: WearErrorReason) : PhoneResult

    /** The demo mailbox can't do this, for example open a message on the phone. */
    data object NotAvailableInDemo : PhoneResult
}
