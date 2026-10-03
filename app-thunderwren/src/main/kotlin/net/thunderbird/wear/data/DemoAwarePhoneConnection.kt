package net.thunderbird.wear.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction

/**
 * Shows the phone's data when there is any, and the demo mailbox while demo mode is on and no phone has published.
 *
 * Data from a real phone wins over a demo started with "Try the demo": when it arrives, demo mode is turned off so the
 * demo mailbox disappears. A demo turned on in the settings stays until it's turned off there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("TooManyFunctions") // PhoneConnection's.
class DemoAwarePhoneConnection(
    private val phone: PhoneConnection,
    private val demo: PhoneConnection,
    private val demoModeStore: DemoModeStore,
) : PhoneConnection {

    private val source: Flow<PhoneConnection?> =
        combine(phone.mailboxes, demoModeStore.mode) { phoneMailboxes, demoMode ->
            when {
                demoMode == DemoMode.ON -> demo

                phoneMailboxes != null -> {
                    if (demoMode == DemoMode.UNTIL_PHONE_CONNECTS) demoModeStore.setMode(DemoMode.OFF)
                    phone
                }

                demoMode == DemoMode.UNTIL_PHONE_CONNECTS -> demo

                else -> null
            }
        }.distinctUntilChanged()

    override val mailboxes: Flow<WearMailboxList?> = source.flatMapLatest { it?.mailboxes ?: flowOf(null) }

    override val isDemo: Flow<Boolean> = source.flatMapLatest { it?.isDemo ?: flowOf(false) }

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> {
        return source.flatMapLatest { it?.inbox(mailboxId) ?: flowOf(null) }
    }

    override fun folders(accountId: String): Flow<List<WearFolder>?> {
        return source.flatMapLatest { it?.folders(accountId) ?: flowOf(null) }
    }

    override fun folder(mailboxId: String): Flow<WearFolder?> {
        return source.flatMapLatest { it?.folder(mailboxId) ?: flowOf(null) }
    }

    override suspend fun loadFolders(accountId: String): PhoneResult = current().loadFolders(accountId)

    override suspend fun loadFolder(folder: FolderRef): PhoneResult = current().loadFolder(folder)

    override suspend fun refresh(): PhoneResult {
        // Always ask the phone, so a phone that appears takes over from the demo.
        val result = phone.refresh()
        return if (result != PhoneResult.Success &&
            demoModeStore.mode.value != DemoMode.OFF
        ) {
            PhoneResult.Success
        } else {
            result
        }
    }

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        return current().performAction(messageId, action)
    }

    override suspend fun markAllRead(mailboxId: String): PhoneResult = current().markAllRead(mailboxId)

    override suspend fun reply(messageId: String, text: String): PhoneResult = current().reply(messageId, text)

    override suspend fun openOnPhone(messageId: String): PhoneResult = current().openOnPhone(messageId)

    override suspend fun loadBody(messageId: String): BodyResult = current().loadBody(messageId)

    private suspend fun current(): PhoneConnection = source.first() ?: phone
}
