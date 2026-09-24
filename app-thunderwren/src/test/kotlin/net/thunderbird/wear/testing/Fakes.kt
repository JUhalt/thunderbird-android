package net.thunderbird.wear.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.SelectedMailboxStore

class FakePhoneConnection : PhoneConnection {
    val mailboxList = MutableStateFlow<WearMailboxList?>(null)
    private val inboxes = mutableMapOf<String, MutableStateFlow<WearInboxSnapshot?>>()

    var refreshCount = 0
    var actionResult: PhoneResult = PhoneResult.Success
    var openOnPhoneResult: PhoneResult = PhoneResult.Success
    val performedActions = mutableListOf<Pair<String, WearMessageAction>>()
    val openedOnPhone = mutableListOf<String>()

    override val mailboxes: Flow<WearMailboxList?> = mailboxList

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> = inboxFlow(mailboxId)

    fun publish(mailboxes: List<WearMailbox>, inboxes: Map<String, List<WearMessageSummary>>) {
        mailboxList.value = WearMailboxList(generatedAt = 1, mailboxes = mailboxes)
        for ((mailboxId, messages) in inboxes) {
            inboxFlow(mailboxId).value = WearInboxSnapshot(
                mailboxId = mailboxId,
                generatedAt = 1,
                unreadCount = messages.count { !it.isRead },
                messages = messages,
            )
        }
    }

    override suspend fun refresh(): PhoneResult {
        refreshCount++
        return PhoneResult.Success
    }

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        performedActions += messageId to action
        return actionResult
    }

    override suspend fun openOnPhone(messageId: String): PhoneResult {
        openedOnPhone += messageId
        return openOnPhoneResult
    }

    private fun inboxFlow(mailboxId: String) = inboxes.getOrPut(mailboxId) { MutableStateFlow(null) }
}

class FakeSelectedMailboxStore(initial: String = WearCompanion.UNIFIED_MAILBOX_ID) : SelectedMailboxStore {
    private val selected = MutableStateFlow(initial)
    override val selectedMailboxId: StateFlow<String> = selected

    override fun select(mailboxId: String) {
        selected.value = mailboxId
    }
}

fun mailbox(id: String, name: String = id, unreadCount: Int = 0) = WearMailbox(
    id = id,
    name = if (id == WearCompanion.UNIFIED_MAILBOX_ID) "" else name,
    email = if (id == WearCompanion.UNIFIED_MAILBOX_ID) "" else "$id@example.com",
    color = if (id == WearCompanion.UNIFIED_MAILBOX_ID) null else 0xFF0A84FF.toInt(),
    unreadCount = unreadCount,
)

fun message(
    id: String,
    senderName: String = "Sender $id",
    subject: String = "Subject $id",
    isRead: Boolean = false,
    isStarred: Boolean = false,
) = WearMessageSummary(
    id = id,
    senderName = senderName,
    senderAddress = "$id@example.com",
    subject = subject,
    preview = "Preview $id",
    date = 1_700_000_000_000,
    isRead = isRead,
    isStarred = isStarred,
    hasAttachments = false,
    isEncrypted = false,
    accountColor = 0xFF0A84FF.toInt(),
)

const val UNIFIED = WearCompanion.UNIFIED_MAILBOX_ID
