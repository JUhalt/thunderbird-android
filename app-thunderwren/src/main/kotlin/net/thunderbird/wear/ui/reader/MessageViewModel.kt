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
import net.thunderbird.wear.data.BodyResult
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.WatchSettingsStore
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
    private val settingsStore: WatchSettingsStore,
) : BaseViewModel<State, Event, Effect>(initialState = State()),
    MessageContract.ViewModel {

    private var markedAsRead = false
    private var bodyRequested = false

    init {
        viewModelScope.launch {
            combine(phoneConnection.message(messageId, mailboxId), phoneConnection.mailboxes) { message, mailboxes ->
                message to message?.let { mailboxes?.accountToLabel(it) }
            }.collect { (message, account) ->
                updateState { it.copy(isLoading = false, message = message, account = account) }
                if (message != null) {
                    markAsReadOnce(message)
                    loadBodyOnce(message)
                }
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

            Event.DeleteClicked -> {
                if (settingsStore.settings.value.confirmDelete) {
                    updateState { it.copy(showDeleteConfirmation = true) }
                } else {
                    perform(WearMessageAction.DELETE, closeOnSuccess = true)
                }
            }

            Event.DeleteConfirmed -> {
                updateState { it.copy(showDeleteConfirmation = false) }
                perform(WearMessageAction.DELETE, closeOnSuccess = true)
            }

            Event.DeleteDismissed -> updateState { it.copy(showDeleteConfirmation = false) }
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

    /** Opening a message reads it, as on the phone, unless that's turned off in the settings. */
    private fun markAsReadOnce(message: WearMessageSummary) {
        if (markedAsRead) return
        markedAsRead = true
        if (!settingsStore.settings.value.markAsReadWhenOpened) return

        if (!message.isRead) {
            viewModelScope.launch { phoneConnection.performAction(messageId, WearMessageAction.MARK_READ) }
        }
    }

    /** The inbox only has a preview, so ask the phone for the whole text. Encrypted messages are read on the phone. */
    private fun loadBodyOnce(message: WearMessageSummary) {
        if (bodyRequested || message.isEncrypted) return
        bodyRequested = true

        viewModelScope.launch {
            updateState { it.copy(isLoadingBody = true) }
            when (val result = phoneConnection.loadBody(messageId)) {
                is BodyResult.Loaded -> updateState {
                    it.copy(
                        isLoadingBody = false,
                        // An empty text, for example of a message without a text part, adds nothing to the preview.
                        body = result.text.takeIf(String::isNotBlank),
                        isBodyIncomplete = !result.isComplete,
                    )
                }

                is BodyResult.Failed -> updateState { it.copy(isLoadingBody = false, isBodyUnavailable = true) }
            }
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
