package net.thunderbird.wear.sync

import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import net.thunderbird.core.logging.Logger
import org.koin.android.ext.android.inject

class PhoneSyncListenerService : WearableListenerService() {

    private val logger: Logger by inject()

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            val path = event.dataItem.uri.path
            if (path == "/account_sync") {
                val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                val email = dataMap.getString("account_email")
                logger.debug { "Received phone companion account sync for: $email" }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/ping_watch") {
            logger.debug { "Received ping from Thunderbird phone companion app" }
        }
    }
}
