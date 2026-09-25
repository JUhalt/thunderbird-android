package net.thunderbird.wear.complication

import android.app.PendingIntent
import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
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
import net.thunderbird.wear.sync.currentGlance
import org.koin.android.ext.android.inject

/**
 * Short-text complication showing the unified inbox's unread count, or only the app's icon if Thunderbird's privacy
 * setting hides even that. Tapping it opens the app.
 */
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
            val glance = phoneConnection.currentGlance(maxMessages = 0)
            listener.onComplicationData(
                if (glance ==
                    null
                ) {
                    NoDataComplicationData()
                } else {
                    createData(glance.unreadCount)
                },
            )
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    /** [unreadCount] is `null` if it may not be shown. */
    private fun createData(unreadCount: Int?): ShortTextComplicationData {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            ThunderWrenActivity.createIntent(this),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val icon = MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_notification)).build()
        val description = if (unreadCount == null) {
            getString(R.string.app_name)
        } else {
            resources.getQuantityString(R.plurals.unread_count, unreadCount, unreadCount)
        }

        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(unreadCount?.toString().orEmpty()).build(),
            contentDescription = PlainComplicationText.Builder(description).build(),
        )
            .setMonochromaticImage(icon)
            .setTapAction(openApp)
            .build()
    }

    private companion object {
        const val PREVIEW_UNREAD_COUNT = 3
    }
}
