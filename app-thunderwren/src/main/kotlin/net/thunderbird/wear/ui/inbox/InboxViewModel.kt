package net.thunderbird.wear.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Preferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.wear.ui.model.EmailHeader
import net.thunderbird.wear.ui.model.SampleEmailData

sealed interface InboxUiState {
    data object Loading : InboxUiState
    data class Success(val headers: List<EmailHeader>, val hasAccounts: Boolean) : InboxUiState
    data class Empty(val message: String) : InboxUiState
}

class InboxViewModel(
    private val preferences: Preferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InboxUiState>(InboxUiState.Loading)
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        loadInbox()
    }

    fun loadInbox() {
        viewModelScope.launch {
            _uiState.value = InboxUiState.Loading
            val accounts: List<LegacyAccountDto> = preferences.getAccounts()
            if (accounts.isNotEmpty()) {
                val account = accounts.first()
                val senderName = account.name?.takeIf { it.isNotEmpty() } ?: account.email
                val email = account.email

                val realHeaders = listOf(
                    EmailHeader(
                        id = "real_1",
                        senderName = senderName,
                        senderAddress = email,
                        subject = "Connected to $email",
                        snippet = "Your Thunderbird email account is synced with ThunderWren.",
                        dateText = "Now",
                        isUnread = true,
                    ),
                ) + SampleEmailData.sampleHeaders
                _uiState.value = InboxUiState.Success(headers = realHeaders, hasAccounts = true)
            } else {
                // Demo / sample mode when no accounts are configured yet on device
                _uiState.value = InboxUiState.Success(
                    headers = SampleEmailData.sampleHeaders,
                    hasAccounts = false,
                )
            }
        }
    }
}
