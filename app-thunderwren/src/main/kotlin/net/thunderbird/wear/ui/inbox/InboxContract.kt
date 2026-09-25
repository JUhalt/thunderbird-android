package net.thunderbird.wear.ui.inbox

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageSummary

interface InboxContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    sealed interface State {
        data object Loading : State

        /** Nothing has been received from the phone yet. */
        data class NotConnected(val isRefreshing: Boolean) : State

        data class Content(
            val mailbox: WearMailbox,
            val canSwitchMailbox: Boolean,
            val messages: ImmutableList<WearMessageSummary>,
            val isRefreshing: Boolean,
            /** The built-in demo mailbox is shown because no phone has published yet. */
            val isDemo: Boolean = false,
            /** The accounts by ID, to show which one each message belongs to. Empty when that's clear already. */
            val accounts: ImmutableMap<String, WearMailbox> = persistentMapOf(),
            val isMarkingAllRead: Boolean = false,
            @field:StringRes val errorMessage: Int? = null,
        ) : State
    }

    sealed interface Event {
        data object MailboxClicked : Event
        data class MessageClicked(val messageId: String) : Event
        data class ArchiveClicked(val messageId: String) : Event
        data class DeleteClicked(val messageId: String) : Event

        /** The user confirmed marking everything in the mailbox with [mailboxId] as read. */
        data class MarkAllReadConfirmed(val mailboxId: String) : Event
        data object RefreshClicked : Event
        data object StartDemoClicked : Event
        data object ExitDemoClicked : Event
    }

    sealed interface Effect {
        data object OpenMailboxes : Effect

        /** Opens the message with [messageId] from the mailbox with [mailboxId]. */
        data class OpenMessage(val mailboxId: String, val messageId: String) : Effect
    }
}
