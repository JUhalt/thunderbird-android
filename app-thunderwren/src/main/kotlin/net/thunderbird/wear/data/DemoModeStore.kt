package net.thunderbird.wear.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Remembers whether the user chose to try the demo mailbox. */
interface DemoModeStore {
    val isEnabled: StateFlow<Boolean>

    fun setEnabled(enabled: Boolean)
}

class SharedPreferencesDemoModeStore(context: Context) : DemoModeStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val enabled = MutableStateFlow(preferences.getBoolean(KEY_DEMO_MODE, false))
    override val isEnabled: StateFlow<Boolean> = enabled.asStateFlow()

    override fun setEnabled(enabled: Boolean) {
        preferences.edit { putBoolean(KEY_DEMO_MODE, enabled) }
        this.enabled.value = enabled
    }

    private companion object {
        const val PREFERENCES_NAME = "thunderwren"
        const val KEY_DEMO_MODE = "demo_mode"
    }
}
