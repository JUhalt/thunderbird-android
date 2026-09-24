package net.thunderbird.wear.ui.reader

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.SelectedMailboxStore

data class MessageUiState(
    val isLoading: Boolean = true,
    /** The message, or `null` if it's no longer in the inbox (for example archived on the phone). */
    val message: WearMessageSummary? = null,
    val isBusy: Boolean = false,
    @field:StringRes val errorMessage: Int? = null,
    val showOpenOnPhoneConfirmation: Boolean = false,
    /** Set after the message was archived or deleted; the screen should close. */
    val isClosed: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class MessageViewModel(
    private val messageId: String,
    private val phoneConnection: PhoneConnection,
    selectedMailboxStore: SelectedMailboxStore,
) : ViewModel() {
    private val state = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = state.asStateFlow()

    private var markedAsRead = false

    init {
        viewModelScope.launch {
            selectedMailboxStore.selectedMailboxId
                .flatMapLatest { mailboxId -> phoneConnection.inbox(mailboxId) }
                .map { inbox -> inbox?.messages?.firstOrNull { it.id == messageId } }
                .collect { message ->
                    state.update { it.copy(isLoading = false, message = message) }
                    if (message != null) markAsReadOnce(message)
                }
        }
    }

    fun toggleRead() {
        val message = state.value.message ?: return
        perform(if (message.isRead) WearMessageAction.MARK_UNREAD else WearMessageAction.MARK_READ)
    }

    fun toggleStar() {
        val message = state.value.message ?: return
        perform(if (message.isStarred) WearMessageAction.UNSTAR else WearMessageAction.STAR)
    }

    fun archive() = perform(WearMessageAction.ARCHIVE, closeOnSuccess = true)

    fun delete() = perform(WearMessageAction.DELETE, closeOnSuccess = true)

    fun openOnPhone() {
        runBusy {
            val result = phoneConnection.openOnPhone(messageId)
            state.update {
                it.copy(
                    showOpenOnPhoneConfirmation = result == PhoneResult.Success,
                    errorMessage = result.errorMessage(),
                )
            }
        }
    }

    fun dismissOpenOnPhoneConfirmation() {
        state.update { it.copy(showOpenOnPhoneConfirmation = false) }
    }

    /** Opening a message reads it, as on the phone. */
    private fun markAsReadOnce(message: WearMessageSummary) {
        if (markedAsRead) return
        markedAsRead = true

        if (!message.isRead) {
            viewModelScope.launch { phoneConnection.performAction(messageId, WearMessageAction.MARK_READ) }
        }
    }

    private fun perform(action: WearMessageAction, closeOnSuccess: Boolean = false) {
        runBusy {
            val result = phoneConnection.performAction(messageId, action)
            state.update {
                it.copy(
                    errorMessage = result.errorMessage(),
                    isClosed = closeOnSuccess && result == PhoneResult.Success,
                )
            }
        }
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (state.value.isBusy) return

        viewModelScope.launch {
            state.update { it.copy(isBusy = true, errorMessage = null) }
            try {
                block()
            } finally {
                state.update { it.copy(isBusy = false) }
            }
        }
    }
}

@StringRes
private fun PhoneResult.errorMessage(): Int? = when (this) {
    PhoneResult.Success -> null

    PhoneResult.NoPhone -> R.string.error_no_phone

    is PhoneResult.Failed -> when (reason) {
        WearErrorReason.ACTION_NOT_AVAILABLE -> R.string.error_archive_unavailable
        WearErrorReason.MESSAGE_NOT_FOUND -> R.string.message_not_found
        WearErrorReason.UNSUPPORTED_REQUEST, WearErrorReason.FAILED -> R.string.error_failed
    }
}
