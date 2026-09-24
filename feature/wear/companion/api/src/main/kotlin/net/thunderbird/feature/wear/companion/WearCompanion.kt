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

    /** Message path the watch sends [WearRequest]s to. */
    const val REQUEST_PATH = "/thunderwren/v1/request"

    /** Maximum number of messages included in a [WearInboxSnapshot]. */
    const val MAX_MESSAGES = 25

    /** Maximum length of [WearMessageSummary.preview]. */
    const val MAX_PREVIEW_LENGTH = 300

    const val OPEN_ON_PHONE_SCHEME = "thunderwren"
    const val OPEN_ON_PHONE_HOST = "open"
    const val OPEN_ON_PHONE_MESSAGE_PARAMETER = "message"

    /** Data Layer path of the [WearInboxSnapshot] for the mailbox with [mailboxId]. */
    fun inboxPath(mailboxId: String): String = INBOX_PATH_PREFIX + mailboxId

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
