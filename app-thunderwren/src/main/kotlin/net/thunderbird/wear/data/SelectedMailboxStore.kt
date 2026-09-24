package net.thunderbird.wear.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.thunderbird.feature.wear.companion.WearCompanion

/** Remembers which mailbox the inbox shows: the unified inbox or one account. */
interface SelectedMailboxStore {
    val selectedMailboxId: StateFlow<String>

    fun select(mailboxId: String)
}

class SharedPreferencesSelectedMailboxStore(context: Context) : SelectedMailboxStore {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val selected = MutableStateFlow(
        preferences.getString(KEY_SELECTED_MAILBOX, null) ?: WearCompanion.UNIFIED_MAILBOX_ID,
    )
    override val selectedMailboxId: StateFlow<String> = selected.asStateFlow()

    override fun select(mailboxId: String) {
        preferences.edit { putString(KEY_SELECTED_MAILBOX, mailboxId) }
        selected.value = mailboxId
    }

    private companion object {
        const val PREFERENCES_NAME = "thunderwren"
        const val KEY_SELECTED_MAILBOX = "selected_mailbox"
    }
}
