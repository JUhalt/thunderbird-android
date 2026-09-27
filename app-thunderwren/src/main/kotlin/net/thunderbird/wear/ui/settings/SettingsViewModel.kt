package net.thunderbird.wear.ui.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.wear.data.WatchSettingsStore
import net.thunderbird.wear.ui.settings.SettingsContract.Effect
import net.thunderbird.wear.ui.settings.SettingsContract.Event
import net.thunderbird.wear.ui.settings.SettingsContract.State

class SettingsViewModel(
    private val settingsStore: WatchSettingsStore,
    appVersion: String,
) : BaseViewModel<State, Event, Effect>(initialState = State(appVersion = appVersion)),
    SettingsContract.ViewModel {

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings -> updateState { it.copy(settings = settings) } }
        }
    }

    override fun event(event: Event) {
        when (event) {
            is Event.SwipeActionsSelected -> settingsStore.update { it.copy(swipeActions = event.swipeActions) }

            is Event.ConfirmDeleteChanged -> settingsStore.update { it.copy(confirmDelete = event.enabled) }

            is Event.MarkAsReadWhenOpenedChanged -> {
                settingsStore.update { it.copy(markAsReadWhenOpened = event.enabled) }
            }

            is Event.ShowPreviewsChanged -> settingsStore.update { it.copy(showPreviews = event.enabled) }
        }
    }
}
