package net.thunderbird.wear.ui.reader

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.message
import net.thunderbird.wear.ui.common.errorMessage

data class MessageUiState(
    val isLoading: Boolean = true,
    /** The message, or `null` if it's no longer in the inbox (for example archived on the phone). */
    val message: WearMessageSummary? = null,
    /** The account the message belongs to, if there are several accounts. */
    val account: WearMailbox? = null,
    val isBusy: Boolean = false,
    @field:StringRes val errorMessage: Int? = null,
    val showOpenOnPhoneConfirmation: Boolean = false,
    /** Set after the message was archived or deleted; the screen should close. */
    val isClosed: Boolean = false,
) {
    /** Encrypted messages can only be answered on the phone, which can encrypt the reply. */
    val canReply: Boolean
        get() = message != null && !message.isEncrypted
}

/** The message with [messageId], opened from the mailbox with [mailboxId]. */
class MessageViewModel(
    private val messageId: String,
    mailboxId: String,
    private val phoneConnection: PhoneConnection,
) : ViewModel() {
    private val state = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = state.asStateFlow()

    private var markedAsRead = false

    init {
        viewModelScope.launch {
            combine(phoneConnection.message(messageId, mailboxId), phoneConnection.mailboxes) { message, mailboxes ->
                message to message?.let { mailboxes?.accountToLabel(it) }
            }.collect { (message, account) ->
                state.update { it.copy(isLoading = false, message = message, account = account) }
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

    fun archive() {
        perform(
            WearMessageAction.ARCHIVE,
            closeOnSuccess = true,
            notAvailableMessage = R.string.error_archive_unavailable,
        )
    }

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

    private fun perform(
        action: WearMessageAction,
        closeOnSuccess: Boolean = false,
        @StringRes notAvailableMessage: Int = R.string.error_failed,
    ) {
        runBusy {
            val result = phoneConnection.performAction(messageId, action)
            state.update {
                it.copy(
                    errorMessage = result.errorMessage(notAvailableMessage),
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

/** The message's account, if there's more than one account to tell apart. */
private fun WearMailboxList.accountToLabel(message: WearMessageSummary): WearMailbox? {
    return account(message.accountId)?.takeIf { mailboxes.count { it.isAccount } > 1 }
}
