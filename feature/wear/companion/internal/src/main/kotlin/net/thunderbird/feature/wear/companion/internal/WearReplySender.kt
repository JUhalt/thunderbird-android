package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.message.QuickReplyResult
import com.fsck.k9.message.QuickReplySender
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearResponse

/** Sends a reply written on the watch. Must be called off the main thread. */
internal fun interface WearReplySender {
    fun reply(reference: MessageReference, text: String): WearResponse
}

internal class QuickReplyWearReplySender(
    private val quickReplySender: QuickReplySender,
) : WearReplySender {

    override fun reply(reference: MessageReference, text: String): WearResponse {
        return when (quickReplySender.sendReply(reference, text)) {
            QuickReplyResult.SENT -> WearResponse.Ok
            QuickReplyResult.MESSAGE_NOT_FOUND -> WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)
            QuickReplyResult.NOT_AVAILABLE -> WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE)
            QuickReplyResult.FAILED -> WearResponse.Error(WearErrorReason.FAILED)
        }
    }
}
