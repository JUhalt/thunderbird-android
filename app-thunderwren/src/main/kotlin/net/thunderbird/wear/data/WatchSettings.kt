package net.thunderbird.wear.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What swiping a message in the inbox does. */
enum class SwipeActions {
    /** Swipe left: a full swipe archives, and Delete is offered next to Archive. */
    LEFT_ARCHIVE,

    /** Swipe left: a full swipe deletes, and Archive is offered next to Delete. */
    LEFT_DELETE,

    /** Swipe left to archive, swipe right to delete. Going back then needs a swipe from the screen's left edge. */
    LEFT_ARCHIVE_RIGHT_DELETE,

    /** Swipe left to delete, swipe right to archive. Going back then needs a swipe from the screen's left edge. */
    LEFT_DELETE_RIGHT_ARCHIVE,
}

/** The watch app's own settings. They stay on the watch; the phone doesn't know them. */
data class WatchSettings(
    val swipeActions: SwipeActions = SwipeActions.LEFT_ARCHIVE,
    /** Ask before deleting a message, like Thunderbird's "Confirm actions" setting on the phone. */
    val confirmDelete: Boolean = false,
    /** Opening a message marks it as read, as on the phone. */
    val markAsReadWhenOpened: Boolean = true,
    /** Show the first lines of each message in the inbox. */
    val showPreviews: Boolean = true,
)

interface WatchSettingsStore {
    val settings: StateFlow<WatchSettings>

    fun update(transform: (WatchSettings) -> WatchSettings)
}

class SharedPreferencesWatchSettingsStore(context: Context) : WatchSettingsStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val current = MutableStateFlow(load())
    override val settings: StateFlow<WatchSettings> = current.asStateFlow()

    override fun update(transform: (WatchSettings) -> WatchSettings) {
        current.update(transform)
        save(current.value)
    }

    private fun load(): WatchSettings {
        val defaults = WatchSettings()
        return WatchSettings(
            swipeActions = preferences.getString(KEY_SWIPE_ACTIONS, null)
                ?.let { name -> SwipeActions.entries.firstOrNull { it.name == name } }
                ?: defaults.swipeActions,
            confirmDelete = preferences.getBoolean(KEY_CONFIRM_DELETE, defaults.confirmDelete),
            markAsReadWhenOpened = preferences.getBoolean(KEY_MARK_AS_READ_WHEN_OPENED, defaults.markAsReadWhenOpened),
            showPreviews = preferences.getBoolean(KEY_SHOW_PREVIEWS, defaults.showPreviews),
        )
    }

    private fun save(settings: WatchSettings) {
        preferences.edit {
            putString(KEY_SWIPE_ACTIONS, settings.swipeActions.name)
            putBoolean(KEY_CONFIRM_DELETE, settings.confirmDelete)
            putBoolean(KEY_MARK_AS_READ_WHEN_OPENED, settings.markAsReadWhenOpened)
            putBoolean(KEY_SHOW_PREVIEWS, settings.showPreviews)
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "thunderwren"
        const val KEY_SWIPE_ACTIONS = "swipe_actions"
        const val KEY_CONFIRM_DELETE = "confirm_delete"
        const val KEY_MARK_AS_READ_WHEN_OPENED = "mark_as_read_when_opened"
        const val KEY_SHOW_PREVIEWS = "show_previews"
    }
}
