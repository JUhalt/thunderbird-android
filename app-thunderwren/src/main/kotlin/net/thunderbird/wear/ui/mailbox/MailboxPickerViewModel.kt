package net.thunderbird.wear.ui.mailbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.ui.inbox.resolve

data class MailboxPickerUiState(
    val mailboxes: ImmutableList<WearMailbox> = persistentListOf(),
    val selectedMailboxId: String? = null,
)

class MailboxPickerViewModel(
    phoneConnection: PhoneConnection,
    private val selectedMailboxStore: SelectedMailboxStore,
) : ViewModel() {

    val uiState: StateFlow<MailboxPickerUiState> =
        combine(phoneConnection.mailboxes, selectedMailboxStore.selectedMailboxId) { mailboxes, selectedId ->
            MailboxPickerUiState(
                mailboxes = mailboxes?.mailboxes?.toImmutableList() ?: persistentListOf(),
                selectedMailboxId = mailboxes?.resolve(selectedId)?.id,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), MailboxPickerUiState())

    fun select(mailboxId: String) {
        selectedMailboxStore.select(mailboxId)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
