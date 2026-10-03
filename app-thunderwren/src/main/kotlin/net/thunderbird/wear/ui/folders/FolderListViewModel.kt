package net.thunderbird.wear.ui.folders

import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.ui.common.errorMessage
import net.thunderbird.wear.ui.folders.FolderListContract.Effect
import net.thunderbird.wear.ui.folders.FolderListContract.Event
import net.thunderbird.wear.ui.folders.FolderListContract.State

/** The folders of the account with [accountId], loaded from the phone each time the list is opened. */
class FolderListViewModel(
    private val accountId: String,
    private val phoneConnection: PhoneConnection,
) : BaseViewModel<State, Event, Effect>(initialState = State()),
    FolderListContract.ViewModel {

    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<Int?>(null)

    init {
        load()

        viewModelScope.launch {
            combine(
                phoneConnection.mailboxes,
                phoneConnection.folders(accountId),
                isLoading,
                errorMessage,
            ) { mailboxes, folders, isLoading, errorMessage ->
                State(
                    account = mailboxes?.account(accountId),
                    folders = folders?.toImmutableList(),
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                )
            }.collect { newState -> updateState { newState } }
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.FolderClicked -> emitEffect(Effect.OpenFolder(accountId = accountId, folderId = event.folderId))
            Event.RetryClicked -> load()
        }
    }

    /** Folders change rarely, but their unread counts often, so the list is loaded again each time. */
    private fun load() {
        if (isLoading.value) return
        isLoading.value = true

        viewModelScope.launch {
            try {
                errorMessage.value = phoneConnection.loadFolders(accountId).errorMessage()
            } finally {
                isLoading.value = false
            }
        }
    }
}
