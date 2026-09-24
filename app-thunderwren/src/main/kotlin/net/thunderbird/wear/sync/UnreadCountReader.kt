package net.thunderbird.wear.sync

import kotlinx.coroutines.flow.first
import net.thunderbird.wear.data.PhoneConnection

/** The unified inbox's unread count for the Tile and complication, or `null` if the phone hasn't published yet. */
suspend fun PhoneConnection.currentUnreadCount(): Int? {
    return mailboxes.first()?.unifiedUnreadCount
}
