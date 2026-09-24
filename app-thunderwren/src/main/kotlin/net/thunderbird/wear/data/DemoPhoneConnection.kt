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
import net.thunderbird.wear.R

/**
 * A made-up mailbox that lives only on the watch, so ThunderWren can be tried without a phone.
 *
 * Actions change the in-memory messages the same way the phone would; nothing is sent anywhere.
 */
class DemoPhoneConnection(
    private val getString: (Int) -> String,
    private val now: () -> Long = System::currentTimeMillis,
) : PhoneConnection {
    private val work = DemoAccount(
        id = "demo-work",
        name = getString(R.string.demo_account_work),
        email = "you@work.example",
        color = WORK_COLOR,
    )
    private val personal = DemoAccount(
        id = "demo-personal",
        name = getString(R.string.demo_account_personal),
        email = "you@home.example",
        color = PERSONAL_COLOR,
    )
    private val accounts = listOf(work, personal)
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
            ) + accounts.map { account ->
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
                account = personal,
                sender = "ThunderWren",
                subject = getString(R.string.demo_welcome_subject),
                preview = getString(R.string.demo_welcome_preview),
                age = 2.minutes,
            ),
            message(
                id = "standup",
                account = work,
                sender = "Priya Natarajan",
                subject = getString(R.string.demo_standup_subject),
                preview = getString(R.string.demo_standup_preview),
                age = 25.minutes,
            ),
            message(
                id = "switch",
                account = personal,
                sender = "ThunderWren",
                subject = getString(R.string.demo_switch_subject),
                preview = getString(R.string.demo_switch_preview),
                age = 1.hours,
                isStarred = true,
            ),
            message(
                id = "encrypted",
                account = work,
                sender = "Jonas Weber",
                subject = getString(R.string.demo_encrypted_subject),
                preview = "",
                age = 3.hours,
                isEncrypted = true,
            ),
            message(
                id = "hike",
                account = personal,
                sender = "Sam Okafor",
                subject = getString(R.string.demo_hike_subject),
                preview = getString(R.string.demo_hike_preview),
                age = 1.days,
                isRead = true,
            ),
            message(
                id = "invoice",
                account = work,
                sender = getString(R.string.demo_sender_billing),
                subject = getString(R.string.demo_invoice_subject),
                preview = getString(R.string.demo_invoice_preview),
                age = 2.days,
                isRead = true,
            ),
        )
    }

    private data class DemoMessage(val accountId: String, val summary: WearMessageSummary)

    private data class DemoAccount(val id: String, val name: String, val email: String, val color: Int)

    private companion object {
        const val WORK_COLOR = 0xFF0A84FF.toInt()
        const val PERSONAL_COLOR = 0xFFFFA23A.toInt()
    }
}
