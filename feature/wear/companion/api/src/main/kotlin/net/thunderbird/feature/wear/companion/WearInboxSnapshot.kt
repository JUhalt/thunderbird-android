package net.thunderbird.feature.wear.companion

import kotlinx.serialization.Serializable

/**
 * The unified inbox as published by the phone for the watch.
 *
 * @property generatedAt Time the snapshot was created, in milliseconds since the epoch.
 * @property unreadCount Number of unread messages in the unified inbox, which can exceed [messages].
 * @property messages The newest messages, newest first, at most [WearCompanion.MAX_MESSAGES].
 */
@Serializable
data class WearInboxSnapshot(
    val version: Int = WearCompanion.PROTOCOL_VERSION,
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
