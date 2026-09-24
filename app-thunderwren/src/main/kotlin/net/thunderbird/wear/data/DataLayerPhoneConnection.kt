package net.thunderbird.wear.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearProtocolCodec
import net.thunderbird.feature.wear.companion.WearRequest
import net.thunderbird.feature.wear.companion.WearResponse

/** [PhoneConnection] over the Wearable Data Layer (Google Play Services). */
class DataLayerPhoneConnection(
    private val context: Context,
    private val logger: Logger,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PhoneConnection {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val remoteActivityHelper by lazy {
        RemoteActivityHelper(context, Executors.newSingleThreadExecutor())
    }

    override val mailboxes: Flow<WearMailboxList?> =
        dataItem(WearCompanion.MAILBOXES_PATH)
            .map { data -> data?.let(WearProtocolCodec::decodeMailboxes) }
            .distinctUntilChanged()

    override val isDemo: Flow<Boolean> = flowOf(false)

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> =
        dataItem(WearCompanion.inboxPath(mailboxId))
            .map { data -> data?.let(WearProtocolCodec::decodeSnapshot) }
            .distinctUntilChanged()

    override suspend fun refresh(): PhoneResult = sendRequest(WearRequest.Refresh)

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        return sendRequest(WearRequest.PerformAction(messageId, action))
    }

    override suspend fun openOnPhone(messageId: String): PhoneResult = withContext(ioDispatcher) {
        val phone = findPhone() ?: return@withContext PhoneResult.NoPhone
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse(WearCompanion.openOnPhoneUri(messageId)))

        runCatchingDataLayer {
            remoteActivityHelper.startRemoteActivity(intent, phone.id).get()
            PhoneResult.Success
        }
    }

    private suspend fun sendRequest(request: WearRequest): PhoneResult = withContext(ioDispatcher) {
        val phone = findPhone() ?: return@withContext PhoneResult.NoPhone

        runCatchingDataLayer {
            val responseData = messageClient
                .sendRequest(phone.id, WearCompanion.REQUEST_PATH, WearProtocolCodec.encodeRequest(request))
                .await()

            when (val response = WearProtocolCodec.decodeResponse(responseData)) {
                WearResponse.Ok -> PhoneResult.Success
                is WearResponse.Error -> PhoneResult.Failed(response.reason)
                null -> PhoneResult.Failed(WearErrorReason.UNSUPPORTED_REQUEST)
            }
        }
    }

    /** Finds a reachable phone running Thunderbird, preferring one connected directly over Bluetooth. */
    private suspend fun findPhone(): Node? {
        val nodes = try {
            capabilityClient
                .getCapability(WearCompanion.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Play Services isn't available or is being updated.
            logger.warn(TAG, e) { "Couldn't look for a phone" }
            emptySet()
        }

        return nodes.firstOrNull { it.isNearby } ?: nodes.firstOrNull()
    }

    private inline fun runCatchingDataLayer(block: () -> PhoneResult): PhoneResult {
        return try {
            block()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // The phone went away or Play Services failed; the user can retry.
            logger.warn(TAG, e) { "Request to the phone failed" }
            PhoneResult.Failed(WearErrorReason.FAILED)
        }
    }

    /** Emits the data of the item at [path] now and whenever the phone changes or deletes it. */
    private fun dataItem(path: String): Flow<ByteArray?> = callbackFlow {
        // The wildcard host (wear://*/path) matches the item whichever node (phone) published it.
        val uri = Uri.Builder()
            .scheme(PutDataRequest.WEAR_URI_SCHEME)
            .authority(ANY_NODE)
            .path(path)
            .build()

        val listener = DataClient.OnDataChangedListener { events ->
            for (event in events) {
                if (event.dataItem.uri.path == path) {
                    // The event buffer is released after this callback returns, so copy the data.
                    trySend(if (event.type == DataEvent.TYPE_DELETED) null else event.dataItem.data?.copyOf())
                }
            }
        }

        try {
            dataClient.addListener(listener, uri, DataClient.FILTER_LITERAL).await()

            val items = dataClient.getDataItems(uri, DataClient.FILTER_LITERAL).await()
            val data = try {
                items.firstOrNull()?.data?.copyOf()
            } finally {
                items.release()
            }
            send(data)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Without Play Services there is nothing to show.
            logger.warn(TAG, e) { "Couldn't read $path" }
            send(null)
        }

        awaitClose { dataClient.removeListener(listener) }
    }

    private companion object {
        const val TAG = "ThunderWren"
        const val ANY_NODE = "*"
    }
}
