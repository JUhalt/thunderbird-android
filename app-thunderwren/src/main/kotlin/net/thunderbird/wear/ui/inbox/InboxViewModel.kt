package net.thunderbird.wear.ui.inbox

import androidx.annotation.StringRes
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.feature.wear.companion.WearCompanion
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
import net.thunderbird.wear.ui.inbox.InboxContract.Effect
import net.thunderbird.wear.ui.inbox.InboxContract.Event
import net.thunderbird.wear.ui.inbox.InboxContract.State

@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val phoneConnection: PhoneConnection,
    selectedMailboxStore: SelectedMailboxStore,
    private val demoModeStore: DemoModeStore,
) : BaseViewModel<State, Event, Effect>(initialState = State.Loading),
    InboxContract.ViewModel {

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

    init {
        viewModelScope.launch {
            combine(content, isRefreshing, phoneConnection.isDemo, actionState, ::toState).collect { newState ->
                updateState { newState }
            }
        }

        // The phone only republishes when its message list changes, so ask for fresh data when the app opens.
        refresh()
    }

    override fun event(event: Event) {
        when (event) {
            Event.MailboxClicked -> emitEffect(Effect.OpenMailboxes)

            is Event.MessageClicked -> openMessage(event.messageId)

            is Event.ArchiveClicked -> {
                removeMessage(
                    messageId = event.messageId,
                    action = WearMessageAction.ARCHIVE,
                    notAvailableMessage = R.string.error_archive_unavailable,
                )
            }

            is Event.DeleteClicked -> removeMessage(event.messageId, WearMessageAction.DELETE)

            is Event.MarkAllReadConfirmed -> markAllRead(event.mailboxId)

            Event.RefreshClicked -> refresh()

            // Shows the demo mailbox until a phone with Thunderbird publishes real data.
            Event.StartDemoClicked -> demoModeStore.setEnabled(true)

            Event.ExitDemoClicked -> demoModeStore.setEnabled(false)
        }
    }

    private fun toState(
        content: InboxContent?,
        isRefreshing: Boolean,
        isDemo: Boolean,
        actions: ActionState,
    ): State {
        return if (content == null) {
            State.NotConnected(isRefreshing = isRefreshing)
        } else {
            State.Content(
                mailbox = content.mailbox,
                canSwitchMailbox = content.canSwitchMailbox,
                messages = content.messages.filterNot { it.id in actions.hiddenMessageIds }.toImmutableList(),
                isRefreshing = isRefreshing,
                isDemo = isDemo,
                accounts = content.accounts,
                isMarkingAllRead = actions.isMarkingAllRead,
                errorMessage = actions.errorMessage,
            )
        }
    }

    private fun refresh() {
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

    private fun openMessage(messageId: String) {
        val mailboxId = (state.value as? State.Content)?.mailbox?.id ?: WearCompanion.UNIFIED_MAILBOX_ID
        emitEffect(Effect.OpenMessage(mailboxId = mailboxId, messageId = messageId))
    }

    private fun markAllRead(mailboxId: String) {
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
