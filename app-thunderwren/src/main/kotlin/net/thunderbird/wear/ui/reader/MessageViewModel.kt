package net.thunderbird.wear.ui.reader

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.message
import net.thunderbird.wear.ui.common.errorMessage
import net.thunderbird.wear.ui.reader.MessageContract.Effect
import net.thunderbird.wear.ui.reader.MessageContract.Event
import net.thunderbird.wear.ui.reader.MessageContract.State

/** The message with [messageId], opened from the mailbox with [mailboxId]. */
class MessageViewModel(
    private val messageId: String,
    private val mailboxId: String,
    private val phoneConnection: PhoneConnection,
) : BaseViewModel<State, Event, Effect>(initialState = State()),
    MessageContract.ViewModel {

    private var markedAsRead = false

    init {
        viewModelScope.launch {
            combine(phoneConnection.message(messageId, mailboxId), phoneConnection.mailboxes) { message, mailboxes ->
                message to message?.let { mailboxes?.accountToLabel(it) }
            }.collect { (message, account) ->
                updateState { it.copy(isLoading = false, message = message, account = account) }
                if (message != null) markAsReadOnce(message)
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.ReplyClicked -> emitEffect(Effect.OpenReply(mailboxId = mailboxId, messageId = messageId))

            Event.OpenOnPhoneClicked -> openOnPhone()

            Event.OpenOnPhoneConfirmationDismissed -> updateState { it.copy(showOpenOnPhoneConfirmation = false) }

            Event.ToggleReadClicked -> toggleRead()

            Event.ToggleStarClicked -> toggleStar()

            Event.ArchiveClicked -> {
                perform(
                    action = WearMessageAction.ARCHIVE,
                    closeOnSuccess = true,
                    notAvailableMessage = R.string.error_archive_unavailable,
                )
            }

            Event.DeleteClicked -> perform(WearMessageAction.DELETE, closeOnSuccess = true)
        }
    }

    private fun toggleRead() {
        val message = state.value.message ?: return
        perform(if (message.isRead) WearMessageAction.MARK_UNREAD else WearMessageAction.MARK_READ)
    }

    private fun toggleStar() {
        val message = state.value.message ?: return
        perform(if (message.isStarred) WearMessageAction.UNSTAR else WearMessageAction.STAR)
    }

    private fun openOnPhone() {
        runBusy {
            val result = phoneConnection.openOnPhone(messageId)
            updateState {
                it.copy(
                    showOpenOnPhoneConfirmation = result == PhoneResult.Success,
                    errorMessage = result.errorMessage(),
                )
            }
        }
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
            updateState { it.copy(errorMessage = result.errorMessage(notAvailableMessage)) }
            if (closeOnSuccess && result == PhoneResult.Success) emitEffect(Effect.Close)
        }
    }

    private fun runBusy(block: suspend () -> Unit) {
        if (state.value.isBusy) return

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null) }
            try {
                block()
            } finally {
                updateState { it.copy(isBusy = false) }
            }
        }
    }
}

/** The message's account, if there's more than one account to tell apart. */
private fun WearMailboxList.accountToLabel(message: WearMessageSummary): WearMailbox? {
    return account(message.accountId)?.takeIf { mailboxes.count { it.isAccount } > 1 }
}
