package net.thunderbird.wear.ui.mailbox

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.wear.companion.WearMailbox

interface MailboxPickerContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val mailboxes: ImmutableList<WearMailbox> = persistentListOf(),
        val selectedMailboxId: String? = null,
    )

    sealed interface Event {
        data class MailboxClicked(val mailboxId: String) : Event
    }

    sealed interface Effect {
        data object Close : Effect
    }
}
