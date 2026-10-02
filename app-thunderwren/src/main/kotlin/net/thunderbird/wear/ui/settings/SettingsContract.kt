package net.thunderbird.wear.ui.settings

import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.wear.data.WatchSettings

interface SettingsContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        val settings: WatchSettings = WatchSettings(),
        /** The demo mailbox is shown instead of the phone's mail. */
        val isDemoOn: Boolean = false,
        /** The watch app's version, shown so people can say which one they use. */
        val appVersion: String = "",
    )

    sealed interface Event {
        /** Switches what swiping left does: archive or delete. */
        data object SwipeLeftClicked : Event

        /** Switches what swiping right does: go back, archive, or delete. */
        data object SwipeRightClicked : Event
        data class ConfirmDeleteChanged(val enabled: Boolean) : Event
        data class MarkAsReadWhenOpenedChanged(val enabled: Boolean) : Event
        data class ShowPreviewsChanged(val enabled: Boolean) : Event
        data class DemoChanged(val enabled: Boolean) : Event
    }

    /** Settings take effect right away, so there's nothing to navigate to. */
    sealed interface Effect
}
