package net.thunderbird.feature.wear.companion

import kotlinx.serialization.Serializable

/**
 * The mailboxes the watch can switch between: the unified inbox first, then each account's inbox.
 *
 * Published at [WearCompanion.MAILBOXES_PATH].
 */
@Serializable
data class WearMailboxList(
    val version: Int = WearCompanion.PROTOCOL_VERSION,
    val generatedAt: Long,
    val mailboxes: List<WearMailbox>,
) {
    /** Total unread count of the unified inbox, or `0` if it's missing. */
    val unifiedUnreadCount: Int
        get() = mailboxes.firstOrNull { it.isUnified }?.unreadCount ?: 0
}

/**
 * A mailbox on the watch.
 *
 * @property id [WearCompanion.UNIFIED_MAILBOX_ID] for the unified inbox, otherwise the account's UUID.
 * @property name Account name. Empty for the unified inbox, which the watch labels itself.
 * @property email Account email address. Empty for the unified inbox.
 * @property color ARGB color of the account. `null` for the unified inbox.
 * @property unreadCount Number of unread messages in this mailbox's inbox.
 */
@Serializable
data class WearMailbox(
    val id: String,
    val name: String,
    val email: String,
    val color: Int?,
    val unreadCount: Int,
) {
    val isUnified: Boolean
        get() = id == WearCompanion.UNIFIED_MAILBOX_ID
}

/**
 * The newest messages of one mailbox's inbox, published at [WearCompanion.inboxPath].
 *
 * @property mailboxId The [WearMailbox.id] this snapshot belongs to.
 * @property generatedAt Time the snapshot was created, in milliseconds since the epoch.
 * @property unreadCount Number of unread messages in the inbox, which can exceed [messages].
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
 * @property id Opaque identifier the watch passes back in [WearRequest.PerformAction] and [WearCompanion.openOnPhoneUri].
 * @property date Message date in milliseconds since the epoch.
 * @property preview Plain-text preview, empty if unavailable, at most [WearCompanion.MAX_PREVIEW_LENGTH] characters.
 * @property isEncrypted Whether the message is end-to-end encrypted, so no preview is available.
 * @property accountColor ARGB color of the account the message belongs to.
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
)
