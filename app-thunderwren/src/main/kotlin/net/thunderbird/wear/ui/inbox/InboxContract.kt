package net.thunderbird.wear.ui.inbox

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.data.MessageSwipeAction

interface InboxContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    sealed interface State {
        data object Loading : State

        /** Nothing has been received from the phone yet. */
        data class NotConnected(
            val isRefreshing: Boolean,
            @field:StringRes val errorMessage: Int? = null,
        ) : State

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
            val swipeLeft: MessageSwipeAction = MessageSwipeAction.ARCHIVE,
            /** What swiping to the right does, or `null` if it goes back. */
            val swipeRight: MessageSwipeAction? = null,
            val showPreviews: Boolean = true,
            val confirmDelete: Boolean = false,
            /** The message waiting for the user to confirm its deletion. */
            val pendingDeleteMessageId: String? = null,
            /** The type of the folder shown, or `null` if it's an inbox or a view of the inboxes. */
            val folderType: WearFolderType? = null,
        ) : State
    }

    sealed interface Event {
        /** The mailbox at the top was tapped: switch mailboxes, or go back to the folder list from a folder. */
        data object MailboxClicked : Event
        data class MessageClicked(val messageId: String) : Event
        data class ArchiveClicked(val messageId: String) : Event
        data class DeleteClicked(val messageId: String) : Event
        data object DeleteConfirmed : Event
        data object DeleteDismissed : Event
        data object SettingsClicked : Event

        /** The user confirmed marking everything in the mailbox with [mailboxId] as read. */
        data class MarkAllReadConfirmed(val mailboxId: String) : Event
        data object RefreshClicked : Event
        data object StartDemoClicked : Event
        data object ExitDemoClicked : Event
    }

    sealed interface Effect {
        data object OpenMailboxes : Effect
        data object OpenSettings : Effect

        /** Opens the message with [messageId] from the mailbox with [mailboxId]. */
        data class OpenMessage(val mailboxId: String, val messageId: String) : Effect
    }
}
