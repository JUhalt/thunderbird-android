package net.thunderbird.wear.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What swiping a message in the inbox does. */
enum class MessageSwipeAction {
    ARCHIVE,
    DELETE,
}

/** The watch app's own settings. They stay on the watch; the phone doesn't know them. */
data class WatchSettings(
    /** A full swipe to the left. The other action is offered next to it, unless swiping right does it. */
    val swipeLeft: MessageSwipeAction = MessageSwipeAction.ARCHIVE,
    /**
     * A swipe to the right, or `null` to keep the usual Wear OS swipe to go back. With an action, going back needs a
     * swipe from the screen's left edge.
     */
    val swipeRight: MessageSwipeAction? = null,
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
        val (swipeLeft, swipeRight) = loadSwipeActions(defaults)
        return WatchSettings(
            swipeLeft = swipeLeft,
            swipeRight = swipeRight,
            confirmDelete = preferences.getBoolean(KEY_CONFIRM_DELETE, defaults.confirmDelete),
            markAsReadWhenOpened = preferences.getBoolean(KEY_MARK_AS_READ_WHEN_OPENED, defaults.markAsReadWhenOpened),
            showPreviews = preferences.getBoolean(KEY_SHOW_PREVIEWS, defaults.showPreviews),
        )
    }

    private fun loadSwipeActions(defaults: WatchSettings): Pair<MessageSwipeAction, MessageSwipeAction?> {
        if (preferences.contains(KEY_SWIPE_LEFT)) {
            return Pair(
                preferences.getString(KEY_SWIPE_LEFT, null).toSwipeAction() ?: defaults.swipeLeft,
                preferences.getString(KEY_SWIPE_RIGHT, null).toSwipeAction(),
            )
        }

        // 0.1.0-beta5 stored one of four combinations.
        return when (preferences.getString(KEY_BETA5_SWIPE_ACTIONS, null)) {
            "LEFT_DELETE" -> MessageSwipeAction.DELETE to null
            "LEFT_ARCHIVE_RIGHT_DELETE" -> MessageSwipeAction.ARCHIVE to MessageSwipeAction.DELETE
            "LEFT_DELETE_RIGHT_ARCHIVE" -> MessageSwipeAction.DELETE to MessageSwipeAction.ARCHIVE
            else -> defaults.swipeLeft to defaults.swipeRight
        }
    }

    private fun save(settings: WatchSettings) {
        preferences.edit {
            putString(KEY_SWIPE_LEFT, settings.swipeLeft.name)
            putString(KEY_SWIPE_RIGHT, settings.swipeRight?.name ?: SWIPE_RIGHT_GO_BACK)
            remove(KEY_BETA5_SWIPE_ACTIONS)
            putBoolean(KEY_CONFIRM_DELETE, settings.confirmDelete)
            putBoolean(KEY_MARK_AS_READ_WHEN_OPENED, settings.markAsReadWhenOpened)
            putBoolean(KEY_SHOW_PREVIEWS, settings.showPreviews)
        }
    }

    private fun String?.toSwipeAction(): MessageSwipeAction? = MessageSwipeAction.entries.firstOrNull {
        it.name == this
    }

    private companion object {
        const val PREFERENCES_NAME = "thunderwren"
        const val KEY_SWIPE_LEFT = "swipe_left"
        const val KEY_SWIPE_RIGHT = "swipe_right"
        const val SWIPE_RIGHT_GO_BACK = "GO_BACK"
        const val KEY_BETA5_SWIPE_ACTIONS = "swipe_actions"
        const val KEY_CONFIRM_DELETE = "confirm_delete"
        const val KEY_MARK_AS_READ_WHEN_OPENED = "mark_as_read_when_opened"
        const val KEY_SHOW_PREVIEWS = "show_previews"
    }
}
