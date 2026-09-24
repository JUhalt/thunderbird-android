package net.thunderbird.wear.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction

/**
 * Shows the phone's data when there is any, and the demo mailbox while demo mode is on and no phone has published.
 *
 * Data from a real phone always wins: when it arrives, demo mode is turned off so the demo mailbox disappears.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DemoAwarePhoneConnection(
    private val phone: PhoneConnection,
    private val demo: PhoneConnection,
    private val demoModeStore: DemoModeStore,
) : PhoneConnection {

    private val source: Flow<PhoneConnection?> =
        combine(phone.mailboxes, demoModeStore.isEnabled) { phoneMailboxes, isDemoEnabled ->
            when {
                phoneMailboxes != null -> {
                    if (isDemoEnabled) demoModeStore.setEnabled(false)
                    phone
                }

                isDemoEnabled -> demo

                else -> null
            }
        }.distinctUntilChanged()

    override val mailboxes: Flow<WearMailboxList?> = source.flatMapLatest { it?.mailboxes ?: flowOf(null) }

    override val isDemo: Flow<Boolean> = source.flatMapLatest { it?.isDemo ?: flowOf(false) }

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> {
        return source.flatMapLatest { it?.inbox(mailboxId) ?: flowOf(null) }
    }

    override suspend fun refresh(): PhoneResult {
        // Always ask the phone, so a phone that appears takes over from the demo.
        val result = phone.refresh()
        return if (result != PhoneResult.Success && demoModeStore.isEnabled.value) PhoneResult.Success else result
    }

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        return current().performAction(messageId, action)
    }

    override suspend fun openOnPhone(messageId: String): PhoneResult = current().openOnPhone(messageId)

    private suspend fun current(): PhoneConnection = source.first() ?: phone
}
