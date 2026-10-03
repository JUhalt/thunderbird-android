package net.thunderbird.feature.wear.companion

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Constants shared by the Thunderbird phone app and the Wear OS app.
 *
 * See RFC 0010 (docs/engineering/rfcs/0010-wear-os-companion.md).
 */
object WearCompanion {
    /** Version of the payloads described in this module. Bump when a change isn't backward compatible. */
    const val PROTOCOL_VERSION = 1

    /** Capability advertised by the phone app when the companion is available. */
    const val PHONE_CAPABILITY = "thunderbird_wear_companion"

    /** Capability advertised by the watch app. */
    const val WATCH_CAPABILITY = "thunderwren_watch"

    /** Data Layer path of the [WearMailboxList] published by the phone. */
    const val MAILBOXES_PATH = "/thunderwren/v1/mailboxes"

    /** Prefix of the Data Layer paths of the [WearInboxSnapshot]s published by the phone, one per mailbox. */
    const val INBOX_PATH_PREFIX = "/thunderwren/v1/inbox/"

    /** [WearMailbox.id] of the unified inbox. Account mailboxes use the account's UUID. */
    const val UNIFIED_MAILBOX_ID = "unified"

    /** [WearMailbox.id] of the unread messages in the unified inbox. */
    const val UNREAD_MAILBOX_ID = "unread"

    /** [WearMailbox.id] of the starred messages in the unified inbox. */
    const val STARRED_MAILBOX_ID = "starred"

    /** Prefix of the [WearMailbox.id] of a folder opened on the watch. See [folderMailboxId]. */
    const val FOLDER_MAILBOX_ID_PREFIX = "folder:"

    /** Message path the watch sends [WearRequest]s to. */
    const val REQUEST_PATH = "/thunderwren/v1/request"

    /** Maximum number of messages included in a [WearInboxSnapshot]. */
    const val MAX_MESSAGES = 25

    /** Maximum number of folders in [WearResponse.Folders]. */
    const val MAX_FOLDERS = 200

    /** Maximum length of [WearFolder.name]. */
    const val MAX_FOLDER_NAME_LENGTH = 100

    /** Maximum length of [WearMessageSummary.preview]. */
    const val MAX_PREVIEW_LENGTH = 300

    /**
     * Maximum length of [WearResponse.Body.text]. It keeps a response well below the Data Layer's 100 KB message limit,
     * and is far more than anyone reads on a watch.
     */
    const val MAX_BODY_LENGTH = 20_000

    /** Maximum length of [WearRequest.Reply.text]. Longer replies belong on the phone. */
    const val MAX_REPLY_LENGTH = 2_000

    const val OPEN_ON_PHONE_SCHEME = "thunderwren"
    const val OPEN_ON_PHONE_HOST = "open"
    const val OPEN_ON_PHONE_MESSAGE_PARAMETER = "message"

    /** Data Layer path of the [WearInboxSnapshot] for the mailbox with [mailboxId]. */
    fun inboxPath(mailboxId: String): String = INBOX_PATH_PREFIX + mailboxId

    /**
     * [WearMailbox.id] of the folder with [folderId] of the account with [accountId], for example to mark everything in
     * it as read with [WearRequest.MarkAllRead]. Folders are loaded on request, so they have no Data Layer path.
     */
    fun folderMailboxId(accountId: String, folderId: Long): String = "$FOLDER_MAILBOX_ID_PREFIX$folderId:$accountId"

    /**
     * The account ID and folder ID of a mailbox ID created by [folderMailboxId], or `null` if [mailboxId] isn't one.
     */
    fun parseFolderMailboxId(mailboxId: String): Pair<String, Long>? {
        val folderAndAccount = mailboxId.takeIf { it.startsWith(FOLDER_MAILBOX_ID_PREFIX) }
            ?.removePrefix(FOLDER_MAILBOX_ID_PREFIX)
            ?: return null
        val folderId = folderAndAccount.substringBefore(':').toLongOrNull()
        val accountId = folderAndAccount.substringAfter(':', missingDelimiterValue = "")

        return if (folderId != null && accountId.isNotEmpty()) accountId to folderId else null
    }

    /** URI the watch opens on the phone to show a message there. */
    fun openOnPhoneUri(messageId: String): String {
        val encodedId = URLEncoder.encode(messageId, Charsets.UTF_8.name())
        return "$OPEN_ON_PHONE_SCHEME://$OPEN_ON_PHONE_HOST?$OPEN_ON_PHONE_MESSAGE_PARAMETER=$encodedId"
    }

    /** Extracts the message ID from a URI created by [openOnPhoneUri], or returns `null` if it isn't one. */
    fun parseOpenOnPhoneUri(uri: String): String? {
        val prefix = "$OPEN_ON_PHONE_SCHEME://$OPEN_ON_PHONE_HOST?$OPEN_ON_PHONE_MESSAGE_PARAMETER="
        val encodedId = uri.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)
            ?.takeIf { it.isNotEmpty() && !it.contains('&') }

        return encodedId?.let { runCatching { URLDecoder.decode(it, Charsets.UTF_8.name()) }.getOrNull() }
    }
}
