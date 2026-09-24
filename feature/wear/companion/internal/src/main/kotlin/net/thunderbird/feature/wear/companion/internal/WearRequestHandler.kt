package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearProtocolCodec
import net.thunderbird.feature.wear.companion.WearRequest
import net.thunderbird.feature.wear.companion.WearResponse

/** Answers the requests the watch sends to [net.thunderbird.feature.wear.companion.WearCompanion.REQUEST_PATH]. */
internal class WearRequestHandler(
    private val publisher: WearInboxPublisher,
    private val messageActions: WearMessageActions,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun handle(requestData: ByteArray): ByteArray {
        val response = when (val request = WearProtocolCodec.decodeRequest(requestData)) {
            null -> WearResponse.Error(WearErrorReason.UNSUPPORTED_REQUEST)
            WearRequest.Refresh -> refresh()
            is WearRequest.PerformAction -> performAction(request)
        }

        return WearProtocolCodec.encodeResponse(response)
    }

    private suspend fun refresh(): WearResponse {
        return if (publisher.publishNow()) WearResponse.Ok else WearResponse.Error(WearErrorReason.FAILED)
    }

    private suspend fun performAction(request: WearRequest.PerformAction): WearResponse {
        val reference = MessageReference.parse(request.messageId)
            ?: return WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)

        val response = withContext(ioDispatcher) {
            messageActions.perform(reference, request.action)
        }

        if (response == WearResponse.Ok) {
            publisher.requestPublish()
        }

        return response
    }
}
