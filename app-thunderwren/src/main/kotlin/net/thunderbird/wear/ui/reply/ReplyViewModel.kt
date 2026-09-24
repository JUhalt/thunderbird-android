package net.thunderbird.wear.ui.reply

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.message
import net.thunderbird.wear.ui.common.errorMessage

data class ReplyUiState(
    val isLoading: Boolean = true,
    /** The message being replied to, or `null` if it's gone. */
    val message: WearMessageSummary? = null,
    /** The reply to review before sending, or `null` while it's being chosen. */
    val draft: String? = null,
    val isSending: Boolean = false,
    @field:StringRes val errorMessage: Int? = null,
    /** Set once the phone accepted the reply; the screen should confirm and close. */
    val isSent: Boolean = false,
)

/** Replies to the message with [messageId], opened from the mailbox with [mailboxId]. */
class ReplyViewModel(
    private val messageId: String,
    mailboxId: String,
    private val phoneConnection: PhoneConnection,
) : ViewModel() {
    private val state = MutableStateFlow(ReplyUiState())
    val uiState: StateFlow<ReplyUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            phoneConnection.message(messageId, mailboxId).collect { message ->
                state.update { it.copy(isLoading = false, message = message) }
            }
        }
    }

    /** Shows [text] for review. Blank text, for example from cancelled voice input, is ignored. */
    fun setDraft(text: String) {
        val draft = text.trim().take(WearCompanion.MAX_REPLY_LENGTH)
        if (draft.isEmpty()) return

        state.update { it.copy(draft = draft, errorMessage = null) }
    }

    fun clearDraft() {
        state.update { it.copy(draft = null, errorMessage = null) }
    }

    fun showInputUnavailable() {
        state.update { it.copy(errorMessage = R.string.reply_input_unavailable) }
    }

    fun send() {
        val current = state.value
        val draft = current.draft
        if (draft == null || current.isSending || current.isSent) return

        viewModelScope.launch {
            state.update { it.copy(isSending = true, errorMessage = null) }
            try {
                val result = phoneConnection.reply(messageId, draft)
                state.update {
                    it.copy(
                        isSent = result == PhoneResult.Success,
                        errorMessage = result.errorMessage(notAvailableMessage = R.string.error_reply_unavailable),
                    )
                }
            } finally {
                state.update { it.copy(isSending = false) }
            }
        }
    }
}
