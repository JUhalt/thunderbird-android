package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearProtocolCodec

/** Publishes the mailboxes and their inboxes to the watch. */
internal interface WearInboxPublisher {
    /** Publishes a snapshot right away. Returns `false` if that failed. */
    suspend fun publishNow(): Boolean

    /** Publishes a snapshot soon, coalescing with other requests, if a watch is paired. */
    fun requestPublish()
}

/**
 * Keeps the watch's copy of the mailboxes current by republishing whenever the message list changes.
 *
 * Nothing is loaded or sent while no watch with the watch app is paired.
 */
@OptIn(FlowPreview::class)
internal class WearSnapshotPublisher(
    private val publicationSource: WearPublicationSource,
    private val dataLayer: WearDataLayer,
    private val messageListRepository: MessageListRepository,
    private val logger: Logger,
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WearInboxPublisher {
    private val started = AtomicBoolean(false)
    private val publishRequests = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val messageListChangedListener = MessageListChangedListener { requestPublish() }

    /** Starts listening for message list changes and publishes once. Calling it again has no effect. */
    fun start() {
        if (!started.compareAndSet(false, true)) return

        messageListRepository.addListener(messageListChangedListener)
        coroutineScope.launch {
            publishRequests
                .onStart { emit(Unit) }
                .debounce(PUBLISH_DEBOUNCE)
                .collect { publishIfWatchPaired() }
        }
    }

    override fun requestPublish() {
        publishRequests.tryEmit(Unit)
    }

    override suspend fun publishNow(): Boolean {
        return try {
            val publication = withContext(ioDispatcher) { publicationSource.load() }
            dataLayer.publish(publication.toDataItems())
            logger.debug(TAG) { "Published ${publication.inboxes.size} mailboxes to the watch" }
            true
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Play Services can fail in many ways (missing, outdated, no Wear OS app); never crash the phone app.
            logger.warn(TAG, e) { "Couldn't publish the inbox to the watch" }
            false
        }
    }

    private suspend fun publishIfWatchPaired() {
        val isWatchPaired = try {
            dataLayer.isWatchPaired()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            logger.debug(TAG, e) { "Wearable Data Layer isn't available" }
            false
        }

        if (isWatchPaired) {
            publishNow()
        }
    }

    private fun WearPublication.toDataItems(): Map<String, ByteArray> = buildMap {
        put(WearCompanion.MAILBOXES_PATH, WearProtocolCodec.encodeMailboxes(mailboxes))
        for (inbox in inboxes) {
            put(WearCompanion.inboxPath(inbox.mailboxId), WearProtocolCodec.encodeSnapshot(inbox))
        }
    }

    private companion object {
        const val TAG = "WearCompanion"
        val PUBLISH_DEBOUNCE = 2.seconds
    }
}
