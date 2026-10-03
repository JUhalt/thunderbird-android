package net.thunderbird.wear.data

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R

/**
 * A made-up mailbox that lives only on the watch, so ThunderWren can be tried without a phone.
 *
 * Actions change the in-memory messages the same way the phone would, for example archiving moves a message to the
 * Archive folder. Nothing is sent anywhere.
 */
@Suppress("TooManyFunctions") // Mostly PhoneConnection's.
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

    /** The same folders in each account, plus one of the account's own. */
    private fun foldersOf(accountId: String): List<DemoFolder> = listOf(
        DemoFolder(INBOX_FOLDER_ID, getString(R.string.demo_folder_inbox), WearFolderType.INBOX),
        DemoFolder(SENT_FOLDER_ID, getString(R.string.demo_folder_sent), WearFolderType.SENT),
        DemoFolder(ARCHIVE_FOLDER_ID, getString(R.string.demo_folder_archive), WearFolderType.ARCHIVE),
        DemoFolder(TRASH_FOLDER_ID, getString(R.string.demo_folder_trash), WearFolderType.TRASH),
        if (accountId == work.id) {
            DemoFolder(OWN_FOLDER_ID, getString(R.string.demo_folder_projects), WearFolderType.REGULAR)
        } else {
            DemoFolder(OWN_FOLDER_ID, getString(R.string.demo_folder_travel), WearFolderType.REGULAR)
        },
    )

    override val isDemo: Flow<Boolean> = flowOf(true)

    override val mailboxes: Flow<WearMailboxList?> = messages.map { messages ->
        val unifiedViews = UNIFIED_VIEW_IDS.map { mailboxId ->
            WearMailbox(
                id = mailboxId,
                name = "",
                email = "",
                color = null,
                unreadCount = messages.count { it.isIn(mailboxId) && !it.summary.isRead },
            )
        }
        val accountInboxes = accounts.map { account ->
            WearMailbox(
                id = account.id,
                name = account.name,
                email = account.email,
                color = account.color,
                unreadCount = messages.count { it.isIn(account.id) && !it.summary.isRead },
                // The same monogram Thunderbird creates for a new account: the first two letters of its name.
                monogram = account.name.take(2).uppercase(),
            )
        }

        WearMailboxList(
            generatedAt = now(),
            mailboxes = unifiedViews + accountInboxes,
            // Show everything the Tile can do; there is nothing private in the demo.
            glanceVisibility = WearGlanceVisibility.EVERYTHING,
        )
    }

    override fun inbox(mailboxId: String): Flow<WearInboxSnapshot?> = messages.map { messages ->
        val inboxMessages = messages
            .filter { it.isIn(mailboxId) }
            .map { it.summary }
            .sortedByDescending { it.date }

        WearInboxSnapshot(
            mailboxId = mailboxId,
            generatedAt = now(),
            unreadCount = inboxMessages.count { !it.isRead },
            messages = inboxMessages,
        )
    }

    override fun folders(accountId: String): Flow<List<WearFolder>?> = messages.map { messages ->
        if (accounts.none { it.id == accountId }) return@map null

        foldersOf(accountId).map { folder ->
            val mailboxId = WearCompanion.folderMailboxId(accountId, folder.id)
            WearFolder(
                id = folder.id,
                name = folder.name,
                type = folder.type,
                unreadCount = messages.count { it.isIn(mailboxId) && !it.summary.isRead },
            )
        }
    }.distinctUntilChanged()

    override fun folder(mailboxId: String): Flow<WearFolder?> {
        val (accountId, folderId) = WearCompanion.parseFolderMailboxId(mailboxId) ?: return flowOf(null)
        return folders(accountId).map { folders -> folders?.firstOrNull { it.id == folderId } }.distinctUntilChanged()
    }

    // Everything is in memory already.
    override suspend fun loadFolders(accountId: String): PhoneResult = folderResult(accountId, folderId = null)

    override suspend fun loadFolder(folder: FolderRef): PhoneResult = folderResult(folder.accountId, folder.folderId)

    override suspend fun refresh(): PhoneResult = PhoneResult.Success

    // The demo messages are short, so their preview is the whole text.
    override suspend fun loadBody(messageId: String): BodyResult {
        val message = messages.value.firstOrNull { it.summary.id == messageId }
            ?: return BodyResult.Failed(PhoneResult.Failed(WearErrorReason.MESSAGE_NOT_FOUND))
        return BodyResult.Loaded(text = message.summary.preview, isComplete = true)
    }

    override suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult {
        val message = messages.value.firstOrNull { it.summary.id == messageId }

        return when {
            message == null -> PhoneResult.Failed(WearErrorReason.MESSAGE_NOT_FOUND)

            action == WearMessageAction.ARCHIVE && message.folderId == ARCHIVE_FOLDER_ID -> {
                PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
            }

            else -> {
                messages.update { messages ->
                    messages.mapNotNull { other -> if (other.summary.id == messageId) other.after(action) else other }
                }
                PhoneResult.Success
            }
        }
    }

    override suspend fun markAllRead(mailboxId: String): PhoneResult {
        messages.update { messages ->
            messages.map { message ->
                if (message.isIn(mailboxId)) message.copy(summary = message.summary.copy(isRead = true)) else message
            }
        }
        return PhoneResult.Success
    }

    // Nothing is sent, but the reply screens can be tried.
    override suspend fun reply(messageId: String, text: String): PhoneResult = PhoneResult.Success

    override suspend fun openOnPhone(messageId: String): PhoneResult = PhoneResult.NotAvailableInDemo

    private fun folderResult(accountId: String, folderId: Long?): PhoneResult {
        val folderExists = accounts.any { it.id == accountId } &&
            (folderId == null || foldersOf(accountId).any { it.id == folderId })
        return if (folderExists) PhoneResult.Success else PhoneResult.Failed(WearErrorReason.MAILBOX_NOT_FOUND)
    }

    private fun DemoMessage.isIn(mailboxId: String): Boolean {
        val folder = WearCompanion.parseFolderMailboxId(mailboxId)
        return when {
            folder != null -> accountId == folder.first && folderId == folder.second

            // The inboxes and their views only show the inbox folders, like on the phone.
            folderId != INBOX_FOLDER_ID -> false

            mailboxId == WearCompanion.UNIFIED_MAILBOX_ID -> true

            mailboxId == WearCompanion.UNREAD_MAILBOX_ID -> !summary.isRead

            mailboxId == WearCompanion.STARRED_MAILBOX_ID -> summary.isStarred

            else -> accountId == mailboxId
        }
    }

    /** The message after [action]: moved by archiving and deleting, like on the phone, or gone from the Trash. */
    private fun DemoMessage.after(action: WearMessageAction): DemoMessage? = when (action) {
        WearMessageAction.ARCHIVE -> copy(folderId = ARCHIVE_FOLDER_ID)
        WearMessageAction.DELETE -> if (folderId == TRASH_FOLDER_ID) null else copy(folderId = TRASH_FOLDER_ID)
        else -> copy(summary = summary.apply(action))
    }

    private fun createMessages(): List<DemoMessage> {
        val time = now()
        return createInboxMessages(time) + createFolderMessages(time)
    }

    private fun createInboxMessages(time: Long) = listOf(
        message(
            time = time,
            id = "welcome",
            account = personal,
            sender = "ThunderWren",
            subject = getString(R.string.demo_welcome_subject),
            preview = getString(R.string.demo_welcome_preview),
            age = 2.minutes,
        ),
        message(
            time = time,
            id = "standup",
            account = work,
            sender = "Priya Natarajan",
            subject = getString(R.string.demo_standup_subject),
            preview = getString(R.string.demo_standup_preview),
            age = 25.minutes,
        ),
        message(
            time = time,
            id = "switch",
            account = personal,
            sender = "ThunderWren",
            subject = getString(R.string.demo_switch_subject),
            preview = getString(R.string.demo_switch_preview),
            age = 1.hours,
            isStarred = true,
        ),
        message(
            time = time,
            id = "encrypted",
            account = work,
            sender = "Jonas Weber",
            subject = getString(R.string.demo_encrypted_subject),
            preview = "",
            age = 3.hours,
            isEncrypted = true,
        ),
        message(
            time = time,
            id = "hike",
            account = personal,
            sender = "Sam Okafor",
            subject = getString(R.string.demo_hike_subject),
            preview = getString(R.string.demo_hike_preview),
            age = 1.days,
            isRead = true,
        ),
        message(
            time = time,
            id = "invoice",
            account = work,
            sender = getString(R.string.demo_sender_billing),
            subject = getString(R.string.demo_invoice_subject),
            preview = getString(R.string.demo_invoice_preview),
            age = 2.days,
            isRead = true,
        ),
    )

    /** Messages in the other folders, so there's something to see when opening them. */
    private fun createFolderMessages(time: Long) = listOf(
        message(
            time = time,
            id = "standup-reply",
            account = work,
            sender = "Priya Natarajan",
            subject = getString(R.string.demo_standup_reply_subject),
            preview = getString(R.string.demo_standup_reply_preview),
            age = 20.minutes,
            isRead = true,
            folderId = SENT_FOLDER_ID,
        ),
        message(
            time = time,
            id = "hike-reply",
            account = personal,
            sender = "Sam Okafor",
            subject = getString(R.string.demo_hike_reply_subject),
            preview = getString(R.string.demo_hike_reply_preview),
            age = 23.hours,
            isRead = true,
            folderId = SENT_FOLDER_ID,
        ),
        message(
            time = time,
            id = "mockups",
            account = work,
            sender = "Mateo Rossi",
            subject = getString(R.string.demo_mockups_subject),
            preview = getString(R.string.demo_mockups_preview),
            age = 4.hours,
            folderId = OWN_FOLDER_ID,
        ),
        message(
            time = time,
            id = "report",
            account = work,
            sender = "Lena Fischer",
            subject = getString(R.string.demo_report_subject),
            preview = getString(R.string.demo_report_preview),
            age = 6.days,
            isRead = true,
            folderId = ARCHIVE_FOLDER_ID,
        ),
        message(
            time = time,
            id = "booking",
            account = personal,
            sender = "Hotel Aurora",
            subject = getString(R.string.demo_booking_subject),
            preview = getString(R.string.demo_booking_preview),
            age = 3.days,
            isStarred = true,
            isRead = true,
            folderId = OWN_FOLDER_ID,
        ),
    )

    @Suppress("LongParameterList")
    private fun message(
        time: Long,
        id: String,
        account: DemoAccount,
        sender: String,
        subject: String,
        preview: String,
        age: Duration,
        isRead: Boolean = false,
        isStarred: Boolean = false,
        isEncrypted: Boolean = false,
        folderId: Long = INBOX_FOLDER_ID,
    ) = DemoMessage(
        accountId = account.id,
        folderId = folderId,
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
            accountId = account.id,
            // In the Sent folder, the "sender" is who the message went to, as the phone sends it.
            isOutgoing = folderId == SENT_FOLDER_ID,
        ),
    )

    private data class DemoMessage(val accountId: String, val folderId: Long, val summary: WearMessageSummary)

    private data class DemoFolder(val id: Long, val name: String, val type: WearFolderType)

    private data class DemoAccount(val id: String, val name: String, val email: String, val color: Int)

    private companion object {
        val UNIFIED_VIEW_IDS = listOf(
            WearCompanion.UNIFIED_MAILBOX_ID,
            WearCompanion.UNREAD_MAILBOX_ID,
            WearCompanion.STARRED_MAILBOX_ID,
        )
        const val INBOX_FOLDER_ID = 1L
        const val SENT_FOLDER_ID = 2L
        const val ARCHIVE_FOLDER_ID = 3L
        const val TRASH_FOLDER_ID = 4L
        const val OWN_FOLDER_ID = 5L
        const val WORK_COLOR = 0xFF0A84FF.toInt()
        const val PERSONAL_COLOR = 0xFFFFA23A.toInt()
    }
}
