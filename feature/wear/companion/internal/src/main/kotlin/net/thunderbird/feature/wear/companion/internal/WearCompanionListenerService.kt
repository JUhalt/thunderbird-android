package net.thunderbird.feature.wear.companion.internal

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.thunderbird.feature.wear.companion.WearCompanion
import org.koin.android.ext.android.inject

/**
 * Started by Google Play Services when the watch sends a request or a watch with the watch app connects.
 */
internal class WearCompanionListenerService : WearableListenerService() {
    private val requestHandler: WearRequestHandler by inject()
    private val publisher: WearSnapshotPublisher by inject()

    private val serviceScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        publisher.start()
    }

    override fun onRequest(nodeId: String, path: String, request: ByteArray): Task<ByteArray>? {
        if (path != WearCompanion.REQUEST_PATH) return null

        val response = TaskCompletionSource<ByteArray>()
        serviceScope.launch {
            response.setResult(requestHandler.handle(request))
        }

        return response.task
    }

    override fun onCapabilityChanged(capabilityInfo: CapabilityInfo) {
        if (capabilityInfo.name == WearCompanion.WATCH_CAPABILITY && capabilityInfo.nodes.isNotEmpty()) {
            publisher.requestPublish()
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
