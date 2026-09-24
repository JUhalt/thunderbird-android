package net.thunderbird.feature.wear.companion.internal

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.feature.wear.companion.WearCompanion

/** The parts of the Wearable Data Layer the phone uses. */
internal interface WearDataLayer {
    /** Whether a watch with the watch app is paired with this phone, even if it isn't in range right now. */
    suspend fun isWatchPaired(): Boolean

    /**
     * Publishes [items], keyed by Data Layer path, and removes previously published inbox items that aren't in
     * [items] anymore, for example those of a removed account.
     */
    suspend fun publish(items: Map<String, ByteArray>)
}

/**
 * [WearDataLayer] backed by Google Play Services.
 *
 * The calls throw if Play Services or the Wear OS companion app isn't available; callers handle that.
 */
internal class PlayServicesWearDataLayer(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WearDataLayer {
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val dataClient = Wearable.getDataClient(context)

    override suspend fun isWatchPaired(): Boolean = withContext(ioDispatcher) {
        val capability = Tasks.await(
            capabilityClient.getCapability(WearCompanion.WATCH_CAPABILITY, CapabilityClient.FILTER_ALL),
        )
        capability.nodes.isNotEmpty()
    }

    override suspend fun publish(items: Map<String, ByteArray>) {
        withContext(ioDispatcher) {
            for ((path, data) in items) {
                val request = PutDataRequest.create(path)
                    .setData(data)
                    .setUrgent()
                Tasks.await(dataClient.putDataItem(request))
            }

            deleteStaleInboxes(currentPaths = items.keys)
        }
    }

    private fun deleteStaleInboxes(currentPaths: Set<String>) {
        val inboxesUri = wearUri(WearCompanion.INBOX_PATH_PREFIX)
        val dataItems = Tasks.await(dataClient.getDataItems(inboxesUri, DataClient.FILTER_PREFIX))
        val stalePaths = try {
            dataItems.mapNotNull { it.uri.path }.filter { it !in currentPaths }
        } finally {
            dataItems.release()
        }

        for (path in stalePaths) {
            Tasks.await(dataClient.deleteDataItems(wearUri(path)))
        }
    }

    private fun wearUri(path: String): Uri {
        return Uri.Builder()
            .scheme(PutDataRequest.WEAR_URI_SCHEME)
            .path(path)
            .build()
    }
}
