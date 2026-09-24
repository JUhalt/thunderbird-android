package net.thunderbird.feature.wear.companion

import kotlinx.serialization.Serializable

/**
 * The mailboxes the watch can switch between: the unified inbox, its unread and starred views, then each account's
 * inbox.
 *
 * Published at [WearCompanion.MAILBOXES_PATH].
 *
 * @property glanceVisibility How much the watch may show outside the app, for example on its Tile, following
 * Thunderbird's lock screen notification setting.
 */
@Serializable
data class WearMailboxList(
    val version: Int = WearCompanion.PROTOCOL_VERSION,
    val generatedAt: Long,
    val mailboxes: List<WearMailbox>,
    val glanceVisibility: WearGlanceVisibility = WearGlanceVisibility.COUNT,
) {
    /** Total unread count of the unified inbox, or `0` if it's missing. */
    val unifiedUnreadCount: Int
        get() = mailboxes.firstOrNull { it.isUnified }?.unreadCount ?: 0

    /** The account mailbox with [accountId], or `null` if there is none. */
    fun account(accountId: String): WearMailbox? = mailboxes.firstOrNull { it.isAccount && it.id == accountId }
}

/**
 * How much the watch may show at a glance, outside the app: on its Tile and complication.
 *
 * Inside the app everything is shown, like in Thunderbird once the phone is unlocked.
 */
@Serializable
enum class WearGlanceVisibility {
    /** Senders and subjects of new messages. */
    EVERYTHING,

    /** Senders of new messages, without subjects. */
    SENDERS,

    /** Only the number of unread messages. */
    COUNT,

    /** Nothing about the mailbox, not even the number of unread messages. */
    NOTHING,
}

/**
 * A mailbox on the watch.
 *
 * @property id [WearCompanion.UNIFIED_MAILBOX_ID], [WearCompanion.UNREAD_MAILBOX_ID], or
 * [WearCompanion.STARRED_MAILBOX_ID] for the views of the unified inbox, otherwise the account's UUID.
 * @property name Account name. Empty for the views of the unified inbox, which the watch labels itself.
 * @property email Account email address. Empty for the views of the unified inbox.
 * @property color ARGB color of the account. `null` for the views of the unified inbox.
 * @property unreadCount Number of unread messages in this mailbox.
 * @property monogram The account's monogram (usually two letters), as shown on the phone. Empty for the views of the
 * unified inbox.
 */
@Serializable
data class WearMailbox(
    val id: String,
    val name: String,
    val email: String,
    val color: Int?,
    val unreadCount: Int,
    val monogram: String = "",
) {
    val isUnified: Boolean
        get() = id == WearCompanion.UNIFIED_MAILBOX_ID

    /** Whether this is an account's inbox, as opposed to a view of the unified inbox. */
    val isAccount: Boolean
        get() = id !in UNIFIED_VIEW_IDS

    private companion object {
        val UNIFIED_VIEW_IDS = setOf(
            WearCompanion.UNIFIED_MAILBOX_ID,
            WearCompanion.UNREAD_MAILBOX_ID,
            WearCompanion.STARRED_MAILBOX_ID,
        )
    }
}

/**
 * The newest messages of one mailbox, published at [WearCompanion.inboxPath].
 *
 * @property mailboxId The [WearMailbox.id] this snapshot belongs to.
 * @property generatedAt Time the snapshot was created, in milliseconds since the epoch.
 * @property unreadCount Number of unread messages in the mailbox, which can exceed [messages].
 * @property messages The newest messages, newest first, at most [WearCompanion.MAX_MESSAGES].
 */
@Serializable
data class WearInboxSnapshot(
    val version: Int = WearCompanion.PROTOCOL_VERSION,
    val mailboxId: String,
    val generatedAt: Long,
    val unreadCount: Int,
    val messages: List<WearMessageSummary>,
)

/**
 * A message as shown on the watch.
 *
 * @property id Opaque identifier the watch passes back in [WearRequest]s and [WearCompanion.openOnPhoneUri].
 * @property date Message date in milliseconds since the epoch.
 * @property preview Plain-text preview, empty if unavailable, at most [WearCompanion.MAX_PREVIEW_LENGTH] characters.
 * @property isEncrypted Whether the message is end-to-end encrypted, so no preview is available.
 * @property accountColor ARGB color of the account the message belongs to.
 * @property accountId [WearMailbox.id] of the account the message belongs to.
 */
@Serializable
data class WearMessageSummary(
    val id: String,
    val senderName: String,
    val senderAddress: String,
    val subject: String,
    val preview: String,
    val date: Long,
    val isRead: Boolean,
    val isStarred: Boolean,
    val hasAttachments: Boolean,
    val isEncrypted: Boolean,
    val accountColor: Int,
    val accountId: String = "",
)
