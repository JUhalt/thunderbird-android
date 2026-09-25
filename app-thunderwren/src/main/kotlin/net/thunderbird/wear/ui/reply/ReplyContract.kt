package net.thunderbird.wear.ui.reply

import androidx.annotation.StringRes
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.wear.companion.WearMessageSummary

interface ReplyContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val isLoading: Boolean = true,
        /** The message being replied to, or `null` if it's gone. */
        val message: WearMessageSummary? = null,
        /** The reply to review before sending, or `null` while it's being chosen. */
        val draft: String? = null,
        val isSending: Boolean = false,
        @field:StringRes val errorMessage: Int? = null,
        /** Set once the phone accepted the reply; the screen confirms it and then closes. */
        val isSent: Boolean = false,
    )

    sealed interface Event {
        data object SpeakOrTypeClicked : Event

        /** Text from the system's reply input, or `null` if it was cancelled. */
        data class ReplyInputReceived(val text: String?) : Event

        /** The system's reply input can't be opened on this watch. */
        data object ReplyInputUnavailable : Event
        data class QuickReplyClicked(val text: String) : Event
        data object SendClicked : Event
        data object ChangeClicked : Event
        data object SentConfirmationDismissed : Event
    }

    sealed interface Effect {
        /** Opens the system's voice and keyboard input; its result is sent back as [Event.ReplyInputReceived]. */
        data object OpenReplyInput : Effect
        data object Close : Effect
    }
}
