package net.thunderbird.wear.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DemoMode {
    OFF,

    /** Chosen while no phone had published ("Try the demo"). Real data from a phone replaces the demo. */
    UNTIL_PHONE_CONNECTS,

    /** Turned on in the settings: the demo is shown even when a phone has published, for example for screenshots. */
    ON,
}

/** Remembers whether the user chose to see the demo mailbox. */
interface DemoModeStore {
    val mode: StateFlow<DemoMode>

    fun setMode(mode: DemoMode)
}

class SharedPreferencesDemoModeStore(context: Context) : DemoModeStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val current = MutableStateFlow(load())
    override val mode: StateFlow<DemoMode> = current.asStateFlow()

    override fun setMode(mode: DemoMode) {
        preferences.edit {
            putString(KEY_DEMO_MODE, mode.name)
            remove(KEY_BETA5_DEMO_MODE)
        }
        current.value = mode
    }

    private fun load(): DemoMode {
        val stored = preferences.getString(KEY_DEMO_MODE, null)
        return DemoMode.entries.firstOrNull { it.name == stored }
            // Up to 0.1.0-beta5, only "Try the demo" existed.
            ?: if (preferences.getBoolean(KEY_BETA5_DEMO_MODE, false)) DemoMode.UNTIL_PHONE_CONNECTS else DemoMode.OFF
    }

    private companion object {
        const val PREFERENCES_NAME = "thunderwren"
        const val KEY_DEMO_MODE = "demo"
        const val KEY_BETA5_DEMO_MODE = "demo_mode"
    }
}
