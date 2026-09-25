package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearProtocolCodec
import net.thunderbird.feature.wear.companion.WearRequest
import net.thunderbird.feature.wear.companion.WearResponse

/** Answers the requests the watch sends to [WearCompanion.REQUEST_PATH]. */
internal class WearRequestHandler(
    private val publisher: WearInboxPublisher,
    private val messageActions: WearMessageActions,
    private val mailboxActions: WearMailboxActions,
    private val replySender: WearReplySender,
    private val feature: WearCompanionFeature,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun handle(requestData: ByteArray): ByteArray {
        val request = WearProtocolCodec.decodeRequest(requestData).takeIf { feature.isEnabled() }
        val response = when (request) {
            null -> WearResponse.Error(WearErrorReason.UNSUPPORTED_REQUEST)
            WearRequest.Refresh -> refresh()
            is WearRequest.PerformAction -> performAction(request)
            is WearRequest.Reply -> reply(request)
            is WearRequest.MarkAllRead -> markAllRead(request)
        }

        return WearProtocolCodec.encodeResponse(response)
    }

    private suspend fun refresh(): WearResponse {
        return if (publisher.publishNow()) WearResponse.Ok else WearResponse.Error(WearErrorReason.FAILED)
    }

    private suspend fun performAction(request: WearRequest.PerformAction): WearResponse {
        val reference = MessageReference.parse(request.messageId)
            ?: return WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)

        return republishIfOk { messageActions.perform(reference, request.action) }
    }

    private suspend fun reply(request: WearRequest.Reply): WearResponse {
        val reference = MessageReference.parse(request.messageId)
        val text = request.text.trim()

        return when {
            reference == null -> WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)

            text.isEmpty() || text.length > WearCompanion.MAX_REPLY_LENGTH -> {
                WearResponse.Error(WearErrorReason.UNSUPPORTED_REQUEST)
            }

            else -> republishIfOk { replySender.reply(reference, text) }
        }
    }

    private suspend fun markAllRead(request: WearRequest.MarkAllRead): WearResponse {
        return republishIfOk { mailboxActions.markAllRead(request.mailboxId) }
    }

    /** Runs [block] off the main thread and, if it succeeded, republishes so the watch sees the result. */
    private suspend fun republishIfOk(block: () -> WearResponse): WearResponse {
        val response = withContext(ioDispatcher) { block() }

        if (response == WearResponse.Ok) {
            publisher.requestPublish()
        }

        return response
    }
}
