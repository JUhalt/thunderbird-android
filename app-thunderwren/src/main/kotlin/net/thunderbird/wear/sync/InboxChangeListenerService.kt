package net.thunderbird.wear.sync

import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import net.thunderbird.wear.complication.UnreadComplicationService
import net.thunderbird.wear.tile.UnreadTileService

/**
 * Started by Google Play Services when the phone publishes a new mailbox list, even while the app is closed.
 *
 * Refreshes the Tile and complication so they show the new unread count.
 */
class InboxChangeListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        TileService.getUpdater(this).requestUpdate(UnreadTileService::class.java)
        ComplicationDataSourceUpdateRequester
            .create(this, ComponentName(this, UnreadComplicationService::class.java))
            .requestUpdateAll()
    }
}
