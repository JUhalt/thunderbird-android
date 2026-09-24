package net.thunderbird.wear.tile

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.sync.currentGlance
import org.koin.android.ext.android.inject

/**
 * Tile showing the unified inbox's unread count and, if Thunderbird's privacy setting allows it, the newest unread
 * messages. Tapping a message opens it; tapping anything else opens the app.
 */
class UnreadTileService : TileService() {
    private val phoneConnection: PhoneConnection by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val tile = SettableFuture.create<TileBuilders.Tile>()
        serviceScope.launch {
            try {
                val glance = phoneConnection.currentGlance(maxMessages = UnreadTileLayout.MAX_MESSAGES)
                val screenWidthDp = requestParams.deviceConfiguration.screenWidthDp
                tile.set(createTile(UnreadTileLayout(this@UnreadTileService).create(glance, screenWidthDp)))
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                tile.setException(e)
            }
        }
        return tile
    }

    @Deprecated("Deprecated in the Tiles library")
    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion(RESOURCES_VERSION)
                .build(),
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createTile(content: LayoutElementBuilders.LayoutElement): TileBuilders.Tile {
        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(LayoutElementBuilders.Layout.Builder().setRoot(content).build())
                            .build(),
                    )
                    .build(),
            )
            .build()
    }

    private companion object {
        const val RESOURCES_VERSION = "1"
    }
}
