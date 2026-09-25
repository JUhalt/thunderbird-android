package net.thunderbird.wear.ui.mailbox

import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.ui.inbox.resolve
import net.thunderbird.wear.ui.mailbox.MailboxPickerContract.Effect
import net.thunderbird.wear.ui.mailbox.MailboxPickerContract.Event
import net.thunderbird.wear.ui.mailbox.MailboxPickerContract.State

class MailboxPickerViewModel(
    phoneConnection: PhoneConnection,
    private val selectedMailboxStore: SelectedMailboxStore,
) : BaseViewModel<State, Event, Effect>(initialState = State()),
    MailboxPickerContract.ViewModel {

    init {
        viewModelScope.launch {
            combine(phoneConnection.mailboxes, selectedMailboxStore.selectedMailboxId) { mailboxes, selectedId ->
                State(
                    mailboxes = mailboxes?.mailboxes?.toImmutableList() ?: persistentListOf(),
                    selectedMailboxId = mailboxes?.resolve(selectedId)?.id,
                )
            }.collect { newState -> updateState { newState } }
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.MailboxClicked -> {
                selectedMailboxStore.select(event.mailboxId)
                emitEffect(Effect.Close)
            }
        }
    }
}
