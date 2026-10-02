package net.thunderbird.wear.ui.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import net.thunderbird.core.ui.contract.mvi.BaseViewModel
import net.thunderbird.wear.data.DemoMode
import net.thunderbird.wear.data.DemoModeStore
import net.thunderbird.wear.data.MessageSwipeAction
import net.thunderbird.wear.data.WatchSettingsStore
import net.thunderbird.wear.ui.settings.SettingsContract.Effect
import net.thunderbird.wear.ui.settings.SettingsContract.Event
import net.thunderbird.wear.ui.settings.SettingsContract.State

class SettingsViewModel(
    private val settingsStore: WatchSettingsStore,
    private val demoModeStore: DemoModeStore,
    appVersion: String,
) : BaseViewModel<State, Event, Effect>(initialState = State(appVersion = appVersion)),
    SettingsContract.ViewModel {

    init {
        viewModelScope.launch {
            combine(settingsStore.settings, demoModeStore.mode) { settings, demoMode ->
                settings to (demoMode != DemoMode.OFF)
            }.collect { (settings, isDemoOn) ->
                updateState { it.copy(settings = settings, isDemoOn = isDemoOn) }
            }
        }
    }

    override fun event(event: Event) {
        when (event) {
            Event.SwipeLeftClicked -> settingsStore.update { it.copy(swipeLeft = it.swipeLeft.other) }

            Event.SwipeRightClicked -> settingsStore.update { it.copy(swipeRight = it.swipeRight.next) }

            is Event.ConfirmDeleteChanged -> settingsStore.update { it.copy(confirmDelete = event.enabled) }

            is Event.MarkAsReadWhenOpenedChanged -> {
                settingsStore.update { it.copy(markAsReadWhenOpened = event.enabled) }
            }

            is Event.ShowPreviewsChanged -> settingsStore.update { it.copy(showPreviews = event.enabled) }

            is Event.DemoChanged -> demoModeStore.setMode(if (event.enabled) DemoMode.ON else DemoMode.OFF)
        }
    }
}

private val MessageSwipeAction.other: MessageSwipeAction
    get() = when (this) {
        MessageSwipeAction.ARCHIVE -> MessageSwipeAction.DELETE
        MessageSwipeAction.DELETE -> MessageSwipeAction.ARCHIVE
    }

/** Go back, then archive, then delete, then go back again. */
private val MessageSwipeAction?.next: MessageSwipeAction?
    get() = when (this) {
        null -> MessageSwipeAction.ARCHIVE
        MessageSwipeAction.ARCHIVE -> MessageSwipeAction.DELETE
        MessageSwipeAction.DELETE -> null
    }
