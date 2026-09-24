package net.thunderbird.feature.wear.companion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A request the watch sends to the phone at [WearCompanion.REQUEST_PATH]. */
@Serializable
sealed interface WearRequest {
    /** Asks the phone to publish a fresh [WearInboxSnapshot]. */
    @Serializable
    @SerialName("refresh")
    data object Refresh : WearRequest

    /** Asks the phone to apply [action] to the message with [messageId]. */
    @Serializable
    @SerialName("action")
    data class PerformAction(
        val messageId: String,
        val action: WearMessageAction,
    ) : WearRequest
}

@Serializable
enum class WearMessageAction {
    MARK_READ,
    MARK_UNREAD,
    STAR,
    UNSTAR,
    ARCHIVE,
    DELETE,
}

/** The phone's answer to a [WearRequest]. */
@Serializable
sealed interface WearResponse {
    @Serializable
    @SerialName("ok")
    data object Ok : WearResponse

    @Serializable
    @SerialName("error")
    data class Error(val reason: WearErrorReason) : WearResponse
}

@Serializable
enum class WearErrorReason {
    /** The request couldn't be decoded, for example because it came from a newer protocol version. */
    UNSUPPORTED_REQUEST,

    /** The message no longer exists or its account was removed. */
    MESSAGE_NOT_FOUND,

    /** The action isn't possible for this message, for example archiving without an archive folder. */
    ACTION_NOT_AVAILABLE,

    /** Something went wrong on the phone. */
    FAILED,
}
