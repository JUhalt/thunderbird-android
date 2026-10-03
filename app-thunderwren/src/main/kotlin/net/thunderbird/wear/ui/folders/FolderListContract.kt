package net.thunderbird.wear.ui.folders

import androidx.annotation.StringRes
import kotlinx.collections.immutable.ImmutableList
import net.thunderbird.core.ui.contract.mvi.UnidirectionalViewModel
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearMailbox

interface FolderListContract {

    interface ViewModel : UnidirectionalViewModel<State, Event, Effect>

    data class State(
        /** The account whose folders are listed, or `null` until the phone's mailboxes are known. */
        val account: WearMailbox? = null,
        /** The folders, or `null` until they have been loaded once. */
        val folders: ImmutableList<WearFolder>? = null,
        val isLoading: Boolean = false,
        @field:StringRes val errorMessage: Int? = null,
    )

    sealed interface Event {
        data class FolderClicked(val folderId: Long) : Event
        data object RetryClicked : Event
    }

    sealed interface Effect {
        data class OpenFolder(val accountId: String, val folderId: Long) : Effect
    }
}
