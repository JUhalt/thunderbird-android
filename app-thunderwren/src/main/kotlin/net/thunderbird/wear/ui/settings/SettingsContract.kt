package net.thunderbird.wear.ui.settings

import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.wear.data.SwipeActions
import net.thunderbird.wear.data.WatchSettings

interface SettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val settings: WatchSettings = WatchSettings(),
        /** The watch app's version, shown so people can say which one they use. */
        val appVersion: String = "",
    )

    sealed interface Event {
        data class SwipeActionsSelected(val swipeActions: SwipeActions) : Event
        data class ConfirmDeleteChanged(val enabled: Boolean) : Event
        data class MarkAsReadWhenOpenedChanged(val enabled: Boolean) : Event
        data class ShowPreviewsChanged(val enabled: Boolean) : Event
    }

    /** Settings take effect right away, so there's nothing to navigate to. */
    sealed interface Effect
}
