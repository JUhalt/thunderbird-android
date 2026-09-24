package net.thunderbird.wear.ui.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.DemoModeStore
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.ui.common.errorMessage

sealed interface InboxUiState {
    data object Loading : InboxUiState

    /** Nothing has been received from the phone yet. */
    data class NotConnected(val isRefreshing: Boolean) : InboxUiState

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
    ) : InboxUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val phoneConnection: PhoneConnection,
    selectedMailboxStore: SelectedMailboxStore,
    private val demoModeStore: DemoModeStore,
) : ViewModel() {
    private val isRefreshing = MutableStateFlow(false)
    private val actionState = MutableStateFlow(ActionState())

    private val content: Flow<InboxContent?> =
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
                        messages = inbox?.messages.orEmpty(),
                        accounts = mailboxes.accountsToLabel(mailbox),
                    )
                }
            }
        }

    val uiState: StateFlow<InboxUiState> =
        combine(content, isRefreshing, phoneConnection.isDemo, actionState) { content, refreshing, isDemo, actions ->
            if (content == null) {
                InboxUiState.NotConnected(isRefreshing = refreshing)
            } else {
                InboxUiState.Content(
                    mailbox = content.mailbox,
                    canSwitchMailbox = content.canSwitchMailbox,
                    messages = content.messages.filterNot { it.id in actions.hiddenMessageIds }.toImmutableList(),
                    isRefreshing = refreshing,
                    isDemo = isDemo,
                    accounts = content.accounts,
                    isMarkingAllRead = actions.isMarkingAllRead,
                    errorMessage = actions.errorMessage,
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

    fun archive(messageId: String) {
        removeMessage(messageId, WearMessageAction.ARCHIVE, notAvailableMessage = R.string.error_archive_unavailable)
    }

    fun delete(messageId: String) {
        removeMessage(messageId, WearMessageAction.DELETE)
    }

    fun markAllRead(mailboxId: String) {
        if (actionState.value.isMarkingAllRead) return

        viewModelScope.launch {
            actionState.update { it.copy(isMarkingAllRead = true, errorMessage = null) }
            try {
                val result = phoneConnection.markAllRead(mailboxId)
                actionState.update { it.copy(errorMessage = result.errorMessage()) }
            } finally {
                actionState.update { it.copy(isMarkingAllRead = false) }
            }
        }
    }

    /** Shows the demo mailbox until a phone with Thunderbird publishes real data. */
    fun startDemo() {
        demoModeStore.setEnabled(true)
    }

    fun exitDemo() {
        demoModeStore.setEnabled(false)
    }

    /**
     * Hides the message right away, like the phone's message list does. It stays hidden once the phone has removed
     * it, and comes back with an error if the phone couldn't.
     */
    private fun removeMessage(
        messageId: String,
        action: WearMessageAction,
        @StringRes notAvailableMessage: Int = R.string.error_failed,
    ) {
        actionState.update { it.copy(hiddenMessageIds = it.hiddenMessageIds + messageId, errorMessage = null) }

        viewModelScope.launch {
            val result = phoneConnection.performAction(messageId, action)
            if (result != PhoneResult.Success) {
                actionState.update {
                    it.copy(
                        hiddenMessageIds = it.hiddenMessageIds - messageId,
                        errorMessage = result.errorMessage(notAvailableMessage),
                    )
                }
            }
        }
    }

    private data class InboxContent(
        val mailbox: WearMailbox,
        val canSwitchMailbox: Boolean,
        val messages: List<WearMessageSummary>,
        val accounts: ImmutableMap<String, WearMailbox>,
    )

    private data class ActionState(
        val hiddenMessageIds: Set<String> = emptySet(),
        val isMarkingAllRead: Boolean = false,
        @field:StringRes val errorMessage: Int? = null,
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

/** The accounts by ID if [mailbox] shows messages of several accounts, otherwise nothing. */
private fun WearMailboxList.accountsToLabel(mailbox: WearMailbox): ImmutableMap<String, WearMailbox> {
    val accounts = mailboxes.filter { it.isAccount }
    return if (mailbox.isAccount || accounts.size < 2) {
        persistentMapOf()
    } else {
        accounts.associateBy { it.id }.toImmutableMap()
    }
}
