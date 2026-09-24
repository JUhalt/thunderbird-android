package net.thunderbird.wear.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.thunderbird.wear.R
import net.thunderbird.wear.ThunderWrenActivity
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.sync.currentUnreadCount
import org.koin.android.ext.android.inject

/** Short-text complication showing the unified inbox's unread count. Tapping it opens the app. */
class UnreadComplicationService : ComplicationDataSourceService() {
    private val phoneConnection: PhoneConnection by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return createData(unreadCount = PREVIEW_UNREAD_COUNT)
    }

    override fun onComplicationRequest(request: ComplicationRequest, listener: ComplicationRequestListener) {
        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            listener.onComplicationData(null)
            return
        }

        serviceScope.launch {
            val unreadCount = phoneConnection.currentUnreadCount()
            listener.onComplicationData(
                if (unreadCount == null) NoDataComplicationData() else createData(unreadCount),
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createData(unreadCount: Int): ShortTextComplicationData {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, ThunderWrenActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(unreadCount.toString()).build(),
            contentDescription = PlainComplicationText.Builder(
                getString(R.string.inbox_unread_count, unreadCount),
            ).build(),
        )
            .setTapAction(openApp)
            .build()
    }

    private companion object {
        const val PREVIEW_UNREAD_COUNT = 3
    }
}
