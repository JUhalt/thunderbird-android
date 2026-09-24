package net.thunderbird.wear.ui.common

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.text.DateFormat
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.wear.R

/** The mailbox's name as shown on the watch. The phone leaves the unified inbox unnamed so it's labeled here. */
@Composable
fun WearMailbox.displayName(): String {
    return if (isUnified) stringResource(R.string.mailbox_unified) else name
}

/** The time for messages from today, otherwise the date. */
fun formatMessageDate(date: Long, now: Long = System.currentTimeMillis()): String {
    return DateUtils.formatSameDayTime(date, now, DateFormat.SHORT, DateFormat.SHORT).toString()
}
