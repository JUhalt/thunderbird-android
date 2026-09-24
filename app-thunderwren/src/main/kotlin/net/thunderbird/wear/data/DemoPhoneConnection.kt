package net.thunderbird.wear.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary

/**
 * A made-up mailbox that lives only on the watch, so ThunderWren can be tried without a phone.
 *
 * Actions change the in-memory messages the same way the phone would; nothing is sent anywhere.
 */
class DemoPhoneConnection(
    private val now: () -> Long = System::currentTimeMillis,
) : PhoneConnection {
    private val messages = MutableStateFlow(createMessages())

    override val isDemo: Flow<Boolean> = flowOf(true)

    override val mailboxes: Flow<WearMailboxList?> = messages.map { messages ->
        WearMailboxList(
            generatedAt = now(),
            mailboxes = listOf(
                WearMailbox(
                    id = WearCompanion.UNIFIED_MAILBOX_ID,
                    name = "",
                    email = "",
                    color = null,
                    unreadCount = messages.count { !it.summary.isRead },
                ),
            ) + ACCOUNTS.map { account ->
                WearMailbox(
                    id = account.id,
                    name = account.name,
                    email = account.email,
                    color = account.color,
                    unreadCount = messages.count { it.accountId == account.id && !it.summary.isRead },
                )
            },
        )
    }

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> = messages.map { messages ->
        val inboxMessages = messages
            .filter { mailboxId == WearCompanion.UNIFIED_MAILBOX_ID || it.accountId == mailboxId }
            .map { it.summary }
            .sortedByDescending { it.date }

        WearInboxSnapshot(
            mailboxId = mailboxId,
            generatedAt = now(),
            unreadCount = inboxMessages.count { !it.isRead },
            messages = inboxMessages,
        )
    }

    override suspend fun refresh(): PhoneResult = PhoneResult.Success

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        messages.update { messages ->
            when (action) {
                WearMessageAction.ARCHIVE, WearMessageAction.DELETE -> messages.filterNot { it.summary.id == messageId }

                else -> messages.map { message ->
                    if (message.summary.id ==
                        messageId
                    ) {
                        message.copy(summary = message.summary.apply(action))
                    } else {
                        message
                    }
                }
            }
        }
        return PhoneResult.Success
    }

    override suspend fun openOnPhone(messageId: String): PhoneResult = PhoneResult.NotAvailableInDemo

    private fun WearMessageSummary.apply(action: WearMessageAction): WearMessageSummary = when (action) {
        WearMessageAction.MARK_READ -> copy(isRead = true)
        WearMessageAction.MARK_UNREAD -> copy(isRead = false)
        WearMessageAction.STAR -> copy(isStarred = true)
        WearMessageAction.UNSTAR -> copy(isStarred = false)
        WearMessageAction.ARCHIVE, WearMessageAction.DELETE -> this
    }

    private fun createMessages(): List<DemoMessage> {
        val time = now()
        fun message(
            id: String,
            account: DemoAccount,
            sender: String,
            subject: String,
            preview: String,
            age: Duration,
            isRead: Boolean = false,
            isStarred: Boolean = false,
            isEncrypted: Boolean = false,
        ) = DemoMessage(
            accountId = account.id,
            summary = WearMessageSummary(
                id = "demo-$id",
                senderName = sender,
                senderAddress = "${sender.substringBefore(' ').lowercase()}@example.org",
                subject = subject,
                preview = if (isEncrypted) "" else preview,
                date = time - age.inWholeMilliseconds,
                isRead = isRead,
                isStarred = isStarred,
                hasAttachments = false,
                isEncrypted = isEncrypted,
                accountColor = account.color,
            ),
        )

        return listOf(
            message(
                id = "welcome",
                account = PERSONAL,
                sender = "ThunderWren",
                subject = "Welcome to the demo mailbox",
                preview = "Tap a message to read it, then try Star or Archive. Swipe right to go back. " +
                    "Nothing here is sent anywhere.",
                age = 2.minutes,
            ),
            message(
                id = "standup",
                account = WORK,
                sender = "Priya Natarajan",
                subject = "Standup moved to 10:30",
                preview = "Quick heads-up: the room is booked at 10, so we'll meet at 10:30 today.",
                age = 25.minutes,
            ),
            message(
                id = "switch",
                account = PERSONAL,
                sender = "ThunderWren",
                subject = "Switch between inboxes",
                preview = "Tap \"All inboxes\" at the top of the list to see a single account's inbox instead.",
                age = 1.hours,
                isStarred = true,
            ),
            message(
                id = "encrypted",
                account = WORK,
                sender = "Jonas Weber",
                subject = "Contract draft",
                preview = "",
                age = 3.hours,
                isEncrypted = true,
            ),
            message(
                id = "hike",
                account = PERSONAL,
                sender = "Sam Okafor",
                subject = "Saturday hike?",
                preview = "Weather looks great. Meet at the trailhead at 8? I'll bring snacks.",
                age = 1.days,
                isRead = true,
            ),
            message(
                id = "invoice",
                account = WORK,
                sender = "Billing",
                subject = "Your invoice is ready",
                preview = "Invoice #1042 for September is attached. No action needed.",
                age = 2.days,
                isRead = true,
            ),
        )
    }

    private data class DemoMessage(val accountId: String, val summary: WearMessageSummary)

    private data class DemoAccount(val id: String, val name: String, val email: String, val color: Int)

    private companion object {
        val WORK = DemoAccount(id = "demo-work", name = "Work", email = "you@work.example", color = 0xFF0A84FF.toInt())
        val PERSONAL = DemoAccount(
            id = "demo-personal",
            name = "Personal",
            email = "you@home.example",
            color = 0xFFFFA23A.toInt(),
        )
        val ACCOUNTS = listOf(WORK, PERSONAL)
    }
}
