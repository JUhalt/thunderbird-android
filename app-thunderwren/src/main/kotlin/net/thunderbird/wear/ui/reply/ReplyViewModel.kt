package net.thunderbird.wear.ui.reply

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.message
import net.thunderbird.wear.ui.common.errorMessage
import net.thunderbird.wear.ui.reply.ReplyContract.Effect
import net.thunderbird.wear.ui.reply.ReplyContract.Event
import net.thunderbird.wear.ui.reply.ReplyContract.State

/** Replies to the message with [messageId], opened from the mailbox with [mailboxId]. */
class ReplyViewModel(
    private val messageId: String,
    mailboxId: String,
    private val phoneConnection: PhoneConnection,
) : BaseViewModel<State, Event, Effect>(initialState = State()),
    ReplyContract.ViewModel {

    init {
        viewModelScope.launch {
            phoneConnection.message(messageId, mailboxId).collect { message ->
                updateState { it.copy(isLoading = false, message = message) }
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.SpeakOrTypeClicked -> emitEffect(Effect.OpenReplyInput)
            is Event.ReplyInputReceived -> event.text?.let(::setDraft)
            Event.ReplyInputUnavailable -> updateState { it.copy(errorMessage = R.string.reply_input_unavailable) }
            is Event.QuickReplyClicked -> setDraft(event.text)
            Event.SendClicked -> send()
            Event.ChangeClicked -> updateState { it.copy(draft = null, errorMessage = null) }
            Event.SentConfirmationDismissed -> emitEffect(Effect.Close)
        }
    }

    /** Shows [text] for review. Blank text, for example from cancelled voice input, is ignored. */
    private fun setDraft(text: String) {
        val draft = text.trim().take(WearCompanion.MAX_REPLY_LENGTH)
        if (draft.isEmpty()) return

        updateState { it.copy(draft = draft, errorMessage = null) }
    }

    private fun send() {
        val current = state.value
        val draft = current.draft
        if (draft == null || current.isSending || current.isSent) return

        viewModelScope.launch {
            updateState { it.copy(isSending = true, errorMessage = null) }
            try {
                val result = phoneConnection.reply(messageId, draft)
                updateState {
                    it.copy(
                        isSent = result == PhoneResult.Success,
                        errorMessage = result.errorMessage(notAvailableMessage = R.string.error_reply_unavailable),
                    )
                }
            } finally {
                updateState { it.copy(isSending = false) }
            }
        }
    }
}
