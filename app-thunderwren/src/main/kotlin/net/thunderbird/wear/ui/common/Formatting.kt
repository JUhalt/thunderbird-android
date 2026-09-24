package net.thunderbird.wear.ui.common

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.text.DateFormat
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.wear.R

/** The mailbox's name as shown on the watch. The phone leaves the unified inbox and its views unnamed. */
@Composable
fun WearMailbox.displayName(): String {
    return when (id) {
        WearCompanion.UNIFIED_MAILBOX_ID -> stringResource(R.string.mailbox_unified)
        WearCompanion.UNREAD_MAILBOX_ID -> stringResource(R.string.mailbox_unread)
        WearCompanion.STARRED_MAILBOX_ID -> stringResource(R.string.mailbox_starred)
        else -> name
    }
}

/** The account's monogram, or the first two letters of its name if the phone didn't send one. */
val WearMailbox.displayMonogram: String
    get() = monogram.ifEmpty { name.filterNot { it.isWhitespace() }.take(2).uppercase() }

/** The time for messages from today, otherwise the date. */
fun formatMessageDate(date: Long, now: Long = System.currentTimeMillis()): String {
    return DateUtils.formatSameDayTime(date, now, DateFormat.SHORT, DateFormat.SHORT).toString()
}
