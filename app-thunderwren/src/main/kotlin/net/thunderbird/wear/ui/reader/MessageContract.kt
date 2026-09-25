package net.thunderbird.wear.ui.reader

import androidx.annotation.StringRes
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageSummary

interface MessageContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val isLoading: Boolean = true,
        /** The message, or `null` if it's no longer in the inbox (for example archived on the phone). */
        val message: WearMessageSummary? = null,
        /** The account the message belongs to, if there are several accounts. */
        val account: WearMailbox? = null,
        val isBusy: Boolean = false,
        @field:StringRes val errorMessage: Int? = null,
        val showOpenOnPhoneConfirmation: Boolean = false,
    ) {
        /** Encrypted messages can only be answered on the phone, which can encrypt the reply. */
        val canReply: Boolean
            get() = message != null && !message.isEncrypted
    }

    sealed interface Event {
        data object ReplyClicked : Event
        data object OpenOnPhoneClicked : Event
        data object OpenOnPhoneConfirmationDismissed : Event
        data object ToggleReadClicked : Event
        data object ToggleStarClicked : Event
        data object ArchiveClicked : Event
        data object DeleteClicked : Event
    }

    sealed interface Effect {
        /** Opens the reply screen for the message with [messageId] from the mailbox with [mailboxId]. */
        data class OpenReply(val mailboxId: String, val messageId: String) : Effect

        /** The message was archived or deleted. */
        data object Close : Effect
    }
}
