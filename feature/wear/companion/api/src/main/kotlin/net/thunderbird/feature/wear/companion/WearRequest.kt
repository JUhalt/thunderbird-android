package net.thunderbird.feature.wear.companion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A request the watch sends to the phone at [WearCompanion.REQUEST_PATH]. */
@Serializable
sealed interface WearRequest {
    /** Asks the phone to publish a fresh [WearInboxSnapshot]. */
    @Serializable
    @SerialName("refresh")
    data object Refresh : WearRequest

    /** Asks the phone to apply [action] to the message with [messageId]. */
    @Serializable
    @SerialName("action")
    data class PerformAction(
        val messageId: String,
        val action: WearMessageAction,
    ) : WearRequest

    /**
     * Asks the phone to send [text] as a reply to the sender of the message with [messageId].
     *
     * The phone sends it from the account the message belongs to, like a reply written there, and marks the message
     * as answered. [text] is at most [WearCompanion.MAX_REPLY_LENGTH] characters.
     */
    @Serializable
    @SerialName("reply")
    data class Reply(
        val messageId: String,
        val text: String,
    ) : WearRequest

    /**
     * Asks the phone to mark every message in the mailbox with [mailboxId] as read. It can also be a folder's
     * [WearCompanion.folderMailboxId].
     */
    @Serializable
    @SerialName("mark_all_read")
    data class MarkAllRead(
        val mailboxId: String,
    ) : WearRequest

    /**
     * Asks the phone for the text of the message with [messageId], to read it in full on the watch. The phone answers
     * with [WearResponse.Body]. The text is only sent when asked for and isn't stored on the watch.
     */
    @Serializable
    @SerialName("load_body")
    data class LoadBody(
        val messageId: String,
    ) : WearRequest

    /**
     * Asks the phone for the folders of the account with [accountId]. The phone answers with [WearResponse.Folders].
     */
    @Serializable
    @SerialName("load_folders")
    data class LoadFolders(
        val accountId: String,
    ) : WearRequest

    /**
     * Asks the phone for the newest messages in the folder with [folderId] of the account with [accountId]. The phone
     * answers with [WearResponse.Folder]. Folders are only sent when asked for, so the phone doesn't publish every
     * folder of every account.
     */
    @Serializable
    @SerialName("load_folder")
    data class LoadFolder(
        val accountId: String,
        val folderId: Long,
    ) : WearRequest
}

@Serializable
enum class WearMessageAction {
    MARK_READ,
    MARK_UNREAD,
    STAR,
    UNSTAR,
    ARCHIVE,
    DELETE,
}

/** The phone's answer to a [WearRequest]. */
@Serializable
sealed interface WearResponse {
    @Serializable
    @SerialName("ok")
    data object Ok : WearResponse

    @Serializable
    @SerialName("error")
    data class Error(val reason: WearErrorReason) : WearResponse

    /**
     * The plain text of a message, answering [WearRequest.LoadBody]. It is at most [WearCompanion.MAX_BODY_LENGTH]
     * characters. [isComplete] is `false` if the text was shortened, or the phone has only downloaded part of the
     * message, so the rest can only be read on the phone.
     */
    @Serializable
    @SerialName("body")
    data class Body(
        val text: String,
        val isComplete: Boolean,
    ) : WearResponse

    /** The folders of an account, answering [WearRequest.LoadFolders]. At most [WearCompanion.MAX_FOLDERS]. */
    @Serializable
    @SerialName("folders")
    data class Folders(
        val folders: List<WearFolder>,
    ) : WearResponse

    /**
     * A folder and its newest messages, answering [WearRequest.LoadFolder]. The [snapshot]'s mailbox ID is the
     * folder's [WearCompanion.folderMailboxId].
     */
    @Serializable
    @SerialName("folder")
    data class Folder(
        val folder: WearFolder,
        val snapshot: WearInboxSnapshot,
    ) : WearResponse
}

@Serializable
enum class WearErrorReason {
    /** The request couldn't be decoded, for example because it came from a newer protocol version. */
    UNSUPPORTED_REQUEST,

    /** The message no longer exists or its account was removed. */
    MESSAGE_NOT_FOUND,

    /**
     * The action isn't possible for this message, for example archiving without an archive folder, or replying to an
     * encrypted message.
     */
    ACTION_NOT_AVAILABLE,

    /** The mailbox or folder no longer exists, for example because its account was removed. */
    MAILBOX_NOT_FOUND,

    /** The watch companion is turned off in Thunderbird on the phone. */
    COMPANION_DISABLED,

    /** Something went wrong on the phone. */
    FAILED,
}
