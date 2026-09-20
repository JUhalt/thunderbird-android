package net.thunderbird.wear.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.ComplicationRequest

class UnreadComplicationService : ComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("2").build(),
            contentDescription = PlainComplicationText.Builder("2 Unread Emails").build(),
        ).build()
    }

    override fun onComplicationRequest(
        request: ComplicationRequest,
        listener: ComplicationRequestListener,
    ) {
        val unreadCount = "2"
        val data = ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(unreadCount).build(),
            contentDescription = PlainComplicationText.Builder("$unreadCount Unread Emails").build(),
        ).build()

        listener.onComplicationData(data)
    }
}
