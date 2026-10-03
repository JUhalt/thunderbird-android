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
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.data.DemoMode
import net.thunderbird.wear.data.DemoModeStore
import net.thunderbird.wear.data.FolderRef
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.data.WatchSettings
import net.thunderbird.wear.data.WatchSettingsStore
import net.thunderbird.wear.ui.common.errorMessage
import net.thunderbird.wear.ui.inbox.InboxContract.Effect
import net.thunderbird.wear.ui.inbox.InboxContract.Event
import net.thunderbird.wear.ui.inbox.InboxContract.State

/**
 * The messages of the mailbox selected in the mailbox picker, or of [folder] if it's opened from the folder list.
 *
 * The inboxes are kept up to date by the phone. A folder is loaded from the phone when it's opened.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InboxViewModel(
    private val phoneConnection: PhoneConnection,
    selectedMailboxStore: SelectedMailboxStore,
    private val demoModeStore: DemoModeStore,
    private val settingsStore: WatchSettingsStore,
    private val folder: FolderRef? = null,
) : BaseViewModel<State, Event, Effect>(initialState = State.Loading),
    InboxContract.ViewModel {

    private val isRefreshing = MutableStateFlow(false)
    private val actionState = MutableStateFlow(ActionState())

    private val content: Flow<InboxContent?> = if (folder != null) {
        folderContent(folder)
    } else {
        selectedMailboxContent(selectedMailboxStore)
    }

    private fun selectedMailboxContent(selectedMailboxStore: SelectedMailboxStore): Flow<InboxContent?> =
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

    /** The folder, shown like its account's inbox, or `null` until the folder or the account is known. */
    private fun folderContent(folder: FolderRef): Flow<InboxContent?> =
        combine(
            phoneConnection.mailboxes,
            phoneConnection.folder(folder.mailboxId),
            phoneConnection.inbox(folder.mailboxId),
        ) { mailboxes, wearFolder, inbox ->
            val account = mailboxes?.account(folder.accountId)
            if (account == null || wearFolder == null) {
                null
            } else {
                InboxContent(
                    mailbox = account.copy(
                        id = folder.mailboxId,
                        name = wearFolder.name,
                        unreadCount = inbox?.unreadCount ?: wearFolder.unreadCount,
                    ),
                    canSwitchMailbox = true,
                    messages = inbox?.messages.orEmpty(),
                    accounts = persistentMapOf(),
                    folderType = wearFolder.type,
                    isLoaded = inbox != null,
                )
            }
        }

    init {
        // The phone only republishes when its message list changes, so ask for fresh data when the app opens. A folder
        // is loaded when it's opened.
        refresh(isRequestedByUser = false)

        viewModelScope.launch {
            combine(
                content,
                isRefreshing,
                phoneConnection.isDemo,
                actionState,
                settingsStore.settings,
                ::toState,
            ).collect { newState ->
                updateState { newState }
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.MailboxClicked -> emitEffect(Effect.OpenMailboxes)

            is Event.MessageClicked -> openMessage(event.messageId)

            is Event.ArchiveClicked -> {
                val isArchiveFolder = (state.value as? State.Content)?.folderType == WearFolderType.ARCHIVE
                removeMessage(
                    messageId = event.messageId,
                    action = WearMessageAction.ARCHIVE,
                    notAvailableMessage = if (isArchiveFolder) {
                        R.string.error_already_archived
                    } else {
                        R.string.error_archive_unavailable
                    },
                )
            }

            is Event.DeleteClicked -> delete(event.messageId)

            Event.DeleteConfirmed -> {
                val messageId = actionState.value.pendingDeleteMessageId ?: return
                actionState.update { it.copy(pendingDeleteMessageId = null) }
                removeMessage(messageId, WearMessageAction.DELETE)
            }

            Event.DeleteDismissed -> actionState.update { it.copy(pendingDeleteMessageId = null) }

            Event.SettingsClicked -> emitEffect(Effect.OpenSettings)

            is Event.MarkAllReadConfirmed -> markAllRead(event.mailboxId)

            Event.RefreshClicked -> refresh(isRequestedByUser = true)

            // Shows the demo mailbox until a phone with Thunderbird publishes real data.
            Event.StartDemoClicked -> demoModeStore.setMode(DemoMode.UNTIL_PHONE_CONNECTS)

            Event.ExitDemoClicked -> demoModeStore.setMode(DemoMode.OFF)
        }
    }

    private fun toState(
        content: InboxContent?,
        isRefreshing: Boolean,
        isDemo: Boolean,
        actions: ActionState,
        settings: WatchSettings,
    ): State {
        return when {
            content != null && (content.isLoaded || !isRefreshing) -> State.Content(
                mailbox = content.mailbox,
                canSwitchMailbox = content.canSwitchMailbox,
                messages = content.messages.filterNot { it.id in actions.hiddenMessageIds }.toImmutableList(),
                isRefreshing = isRefreshing,
                isDemo = isDemo,
                accounts = content.accounts,
                isMarkingAllRead = actions.isMarkingAllRead,
                errorMessage = actions.errorMessage,
                swipeLeft = settings.swipeLeft,
                swipeRight = settings.swipeRight,
                showPreviews = settings.showPreviews,
                confirmDelete = settings.confirmDelete,
                pendingDeleteMessageId = actions.pendingDeleteMessageId,
                folderType = content.folderType,
            )

            // The folder is being loaded for the first time.
            folder != null && isRefreshing -> State.Loading

            else -> State.NotConnected(isRefreshing = isRefreshing, errorMessage = actions.errorMessage)
        }
    }

    /**
     * Asks the phone for fresh data. Failures of the automatic refresh when the app opens aren't shown, because the
     * phone often isn't reachable for a moment then and the last data is still there, except when the companion is
     * turned off on the phone. A folder only has data once it's loaded, so failing to load it is always shown.
     */
    private fun refresh(isRequestedByUser: Boolean) {
        if (isRefreshing.value) return
        isRefreshing.value = true

        viewModelScope.launch {
            try {
                val result = if (folder != null) phoneConnection.loadFolder(folder) else phoneConnection.refresh()
                if (isRequestedByUser || folder != null || result.isCompanionDisabled) {
                    actionState.update { it.copy(errorMessage = result.errorMessage()) }
                }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    private fun openMessage(messageId: String) {
        val mailboxId = (state.value as? State.Content)?.mailbox?.id ?: WearCompanion.UNIFIED_MAILBOX_ID
        emitEffect(Effect.OpenMessage(mailboxId = mailboxId, messageId = messageId))
    }

    private fun delete(messageId: String) {
        if (settingsStore.settings.value.confirmDelete) {
            actionState.update { it.copy(pendingDeleteMessageId = messageId) }
        } else {
            removeMessage(messageId, WearMessageAction.DELETE)
        }
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
        val folderType: WearFolderType? = null,
        /** Whether the messages have arrived. A folder's are only known once it has been loaded. */
        val isLoaded: Boolean = true,
    )

    private data class ActionState(
        val hiddenMessageIds: Set<String> = emptySet(),
        val isMarkingAllRead: Boolean = false,
        @field:StringRes val errorMessage: Int? = null,
        val pendingDeleteMessageId: String? = null,
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

private val PhoneResult.isCompanionDisabled: Boolean
    get() = this is PhoneResult.Failed && reason == WearErrorReason.COMPANION_DISABLED
