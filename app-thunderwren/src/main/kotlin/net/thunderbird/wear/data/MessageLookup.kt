package net.thunderbird.wear.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.runningReduce
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMessageSummary

/**
 * The message with [messageId] as the phone last published it, looked up in the mailbox it was opened from
 * ([mailboxId]) and in the unified inbox. Emits `null` if it's in neither, for example because it was archived.
 *
 * A message opened from the "Unread" or "Starred" view leaves that view when it's read or unstarred. It's then found
 * in the unified inbox or, if it's too old to be in there, its last known version is kept.
 */
fun PhoneConnection.message(messageId: String, mailboxId: String): Flow<WearMessageSummary?> {
    val mailboxIds = listOf(mailboxId, WearCompanion.UNIFIED_MAILBOX_ID).distinct()
    val keepWhenGone = mailboxId == WearCompanion.UNREAD_MAILBOX_ID || mailboxId == WearCompanion.STARRED_MAILBOX_ID

    return combine(mailboxIds.map(::inbox)) { snapshots ->
        snapshots.firstNotNullOfOrNull { snapshot -> snapshot?.messages?.firstOrNull { it.id == messageId } }
    }.runningReduce { last, current -> current ?: last.takeIf { keepWhenGone } }
}
