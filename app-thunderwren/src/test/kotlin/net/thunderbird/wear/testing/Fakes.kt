package net.thunderbird.wear.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.data.DemoModeStore
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.data.SelectedMailboxStore

class FakePhoneConnection(isDemo: Boolean = false) : PhoneConnection {
    val mailboxList = MutableStateFlow<WearMailboxList?>(null)
    private val inboxes = mutableMapOf<String, MutableStateFlow<WearInboxSnapshot?>>()

    var refreshCount = 0
    var actionResult: PhoneResult = PhoneResult.Success
    var openOnPhoneResult: PhoneResult = PhoneResult.Success
    var markAllReadResult: PhoneResult = PhoneResult.Success
    var replyResult: PhoneResult = PhoneResult.Success
    val performedActions = mutableListOf<Pair<String, WearMessageAction>>()
    val markedAllRead = mutableListOf<String>()
    val replies = mutableListOf<Pair<String, String>>()
    val openedOnPhone = mutableListOf<String>()

    override val mailboxes: Flow<WearMailboxList?> = mailboxList

    override val isDemo: Flow<Boolean> = MutableStateFlow(isDemo)

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> = inboxFlow(mailboxId)

    fun publish(
        mailboxes: List<WearMailbox>,
        inboxes: Map<String, List<WearMessageSummary>>,
        glanceVisibility: WearGlanceVisibility = WearGlanceVisibility.EVERYTHING,
    ) {
        mailboxList.value = WearMailboxList(generatedAt = 1, mailboxes = mailboxes, glanceVisibility = glanceVisibility)
        for ((mailboxId, messages) in inboxes) {
            inboxFlow(mailboxId).value = WearInboxSnapshot(
                mailboxId = mailboxId,
                generatedAt = 1,
                unreadCount = messages.count { !it.isRead },
                messages = messages,
            )
        }
    }

    var refreshResult: PhoneResult = PhoneResult.Success

    override suspend fun refresh(): PhoneResult {
        refreshCount++
        return refreshResult
    }

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        performedActions += messageId to action
        return actionResult
    }

    override suspend fun markAllRead(mailboxId: String): PhoneResult {
        markedAllRead += mailboxId
        return markAllReadResult
    }

    override suspend fun reply(messageId: String, text: String): PhoneResult {
        replies += messageId to text
        return replyResult
    }

    override suspend fun openOnPhone(messageId: String): PhoneResult {
        openedOnPhone += messageId
        return openOnPhoneResult
    }

    private fun inboxFlow(mailboxId: String) = inboxes.getOrPut(mailboxId) { MutableStateFlow(null) }
}

class FakeDemoModeStore(initial: Boolean = false) : DemoModeStore {
    private val enabled = MutableStateFlow(initial)
    override val isEnabled: StateFlow<Boolean> = enabled

    override fun setEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}

class FakeSelectedMailboxStore(initial: String = WearCompanion.UNIFIED_MAILBOX_ID) : SelectedMailboxStore {
    private val selected = MutableStateFlow(initial)
    override val selectedMailboxId: StateFlow<String> = selected

    override fun select(mailboxId: String) {
        selected.value = mailboxId
    }
}

/** A mailbox as the phone publishes it: named with a color and monogram for accounts, unnamed for views. */
fun mailbox(
    id: String,
    name: String = id,
    unreadCount: Int = 0,
    monogram: String = name.take(2).uppercase(),
): WearMailbox {
    val isAccount = id !in listOf(UNIFIED, UNREAD, STARRED)
    return WearMailbox(
        id = id,
        name = if (isAccount) name else "",
        email = if (isAccount) "$id@example.com" else "",
        color = if (isAccount) 0xFF0A84FF.toInt() else null,
        unreadCount = unreadCount,
        monogram = if (isAccount) monogram else "",
    )
}

fun message(
    id: String,
    senderName: String = "Sender $id",
    subject: String = "Subject $id",
    isRead: Boolean = false,
    isStarred: Boolean = false,
    isEncrypted: Boolean = false,
    accountId: String = "",
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
    isEncrypted = isEncrypted,
    accountColor = 0xFF0A84FF.toInt(),
    accountId = accountId,
)

const val UNIFIED = WearCompanion.UNIFIED_MAILBOX_ID
const val UNREAD = WearCompanion.UNREAD_MAILBOX_ID
const val STARRED = WearCompanion.STARRED_MAILBOX_ID
