package net.thunderbird.wear.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.SelectedMailboxStore

sealed interface InboxUiState {
    data object Loading : InboxUiState

    /** Nothing has been received from the phone yet. */
    data class NotConnected(val isRefreshing: Boolean) : InboxUiState

    data class Content(
        val mailbox: WearMailbox,
        val canSwitchMailbox: Boolean,
        val messages: ImmutableList<WearMessageSummary>,
        val isRefreshing: Boolean,
    ) : InboxUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val phoneConnection: PhoneConnection,
    selectedMailboxStore: SelectedMailboxStore,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)

    val uiState: StateFlow<InboxUiState> =
        combine(phoneConnection.mailboxes, selectedMailboxStore.selectedMailboxId) { mailboxes, selectedId ->
            mailboxes to mailboxes?.resolve(selectedId)
        }.flatMapLatest { (mailboxes, mailbox) ->
            if (mailboxes == null || mailbox == null) {
                flowOf(null)
            } else {
                phoneConnection.inbox(mailbox.id).map { inbox ->
                    InboxContent(
                        mailbox = mailbox,
                        canSwitchMailbox = mailboxes.mailboxes.size > 1,
                        messages = inbox?.messages?.toImmutableList() ?: persistentListOf(),
                    )
                }
            }
        }.combine(isRefreshing) { content, refreshing ->
            if (content == null) {
                InboxUiState.NotConnected(isRefreshing = refreshing)
            } else {
                InboxUiState.Content(
                    mailbox = content.mailbox,
                    canSwitchMailbox = content.canSwitchMailbox,
                    messages = content.messages,
                    isRefreshing = refreshing,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), InboxUiState.Loading)

    init {
        // The phone only republishes when its message list changes, so ask for fresh data when the app opens.
        refresh()
    }

    fun refresh() {
        if (isRefreshing.value) return

        viewModelScope.launch {
            isRefreshing.value = true
            try {
                phoneConnection.refresh()
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private data class InboxContent(
        val mailbox: WearMailbox,
        val canSwitchMailbox: Boolean,
        val messages: ImmutableList<WearMessageSummary>,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/** The selected mailbox, or the unified inbox if the selected account was removed on the phone. */
internal fun WearMailboxList.resolve(selectedId: String): WearMailbox? {
    return mailboxes.firstOrNull { it.id == selectedId }
        ?: mailboxes.firstOrNull { it.isUnified }
        ?: mailboxes.firstOrNull()
}
