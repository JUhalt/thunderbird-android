package net.thunderbird.wear.sync

import kotlinx.coroutines.flow.first
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.data.PhoneConnection

/**
 * What the Tile and complication may show: people nearby can see them, so the phone decides how much with
 * Thunderbird's lock screen notification setting.
 *
 * @property unreadCount Unread messages in the unified inbox, or `null` if even that is hidden.
 * @property latestUnread The newest unread messages, if their senders may be shown.
 */
data class Glance(
    val unreadCount: Int?,
    val latestUnread: List<GlanceMessage>,
)

/** @property subject `null` if subjects are hidden. */
data class GlanceMessage(
    val id: String,
    val sender: String,
    val subject: String?,
)

/** The [Glance] for the last data the phone published, or `null` if it hasn't published yet. */
suspend fun PhoneConnection.currentGlance(maxMessages: Int): Glance? {
    val mailboxes = mailboxes.first() ?: return null
    val visibility = mailboxes.glanceVisibility

    val showSenders = visibility == WearGlanceVisibility.EVERYTHING || visibility == WearGlanceVisibility.SENDERS
    val showSubjects = visibility == WearGlanceVisibility.EVERYTHING
    val latestUnread = if (showSenders && maxMessages > 0) latestUnread(maxMessages) else emptyList()

    return Glance(
        unreadCount = mailboxes.unifiedUnreadCount.takeIf { visibility != WearGlanceVisibility.NOTHING },
        latestUnread = latestUnread.map { message ->
            GlanceMessage(
                id = message.id,
                sender = message.senderName.ifEmpty { message.senderAddress },
                subject = message.subject.takeIf { showSubjects },
            )
        },
    )
}

private suspend fun PhoneConnection.latestUnread(maxMessages: Int): List<WearMessageSummary> {
    // Older phones don't publish the unread view, but their unified inbox has the newest unread messages too.
    val unread = inbox(WearCompanion.UNREAD_MAILBOX_ID).first()?.messages
        ?: inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()?.messages?.filterNot { it.isRead }

    return unread.orEmpty().take(maxMessages)
}
