package net.thunderbird.wear.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
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
import net.thunderbird.wear.R
import net.thunderbird.wear.ThunderWrenActivity
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.sync.currentUnreadCount
import org.koin.android.ext.android.inject

/** Tile showing the unified inbox's unread count. Tapping it opens the app. */
class UnreadTileService : TileService() {
    private val phoneConnection: PhoneConnection by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val tile = SettableFuture.create<TileBuilders.Tile>()
        serviceScope.launch {
            try {
                tile.set(createTile(phoneConnection.currentUnreadCount()))
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

    private fun createTile(unreadCount: Int?): TileBuilders.Tile {
        val openApp = ModifiersBuilders.Clickable.Builder()
            .setId(CLICKABLE_ID_OPEN_APP)
            .setOnClick(
                ActionBuilders.LaunchAction.Builder()
                    .setAndroidActivity(
                        ActionBuilders.AndroidActivity.Builder()
                            .setPackageName(packageName)
                            .setClassName(ThunderWrenActivity::class.java.name)
                            .build(),
                    )
                    .build(),
            )
            .build()

        val status = if (unreadCount == null) {
            getString(R.string.tile_not_connected)
        } else {
            getString(R.string.tile_unread_count, unreadCount)
        }

        val content = LayoutElementBuilders.Column.Builder()
            .setModifiers(ModifiersBuilders.Modifiers.Builder().setClickable(openApp).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(getString(R.string.app_name)).build())
            .addContent(LayoutElementBuilders.Text.Builder().setText(status).setMaxLines(2).build())
            .build()

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
        const val CLICKABLE_ID_OPEN_APP = "open_app"
    }
}
