package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.message.controller.MessageCountsProvider
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.MessageColumns
import com.fsck.k9.search.getLegacyAccounts
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageSummary

/** Everything the phone publishes for the watch: the mailbox list and one inbox snapshot per mailbox. */
internal data class WearPublication(
    val mailboxes: WearMailboxList,
    val inboxes: List<WearInboxSnapshot>,
)

internal fun interface WearPublicationSource {
    fun load(): WearPublication
}

/**
 * Loads the unified inbox (the same set of folders the message list widget shows) and each account's inbox.
 */
@OptIn(ExperimentalTime::class)
internal class InboxPublicationSource(
    private val accountManager: LegacyAccountManager,
    private val messageListRepository: MessageListRepository,
    private val messageCountsProvider: MessageCountsProvider,
    private val messageHelper: MessageHelper,
    private val clock: Clock,
) : WearPublicationSource {

    override fun load(): WearPublication {
        val generatedAt = clock.now().toEpochMilliseconds()

        // The title and detail are only used for display, which the watch does itself.
        val unifiedInbox = SearchAccount.createUnifiedFoldersSearch(title = "", detail = "")
        val unifiedSearch = unifiedInbox.relatedSearch
        val unifiedSnapshot = WearInboxSnapshot(
            mailboxId = WearCompanion.UNIFIED_MAILBOX_ID,
            generatedAt = generatedAt,
            unreadCount = messageCountsProvider.getMessageCounts(unifiedInbox).unread,
            messages = loadMessages(unifiedSearch.getLegacyAccounts(accountManager), unifiedSearch),
        )

        val accounts = accountManager.getAccounts()
        val accountSnapshots = accounts.map { account -> loadAccountInbox(account, generatedAt) }

        val mailboxes = listOf(
            WearMailbox(
                id = WearCompanion.UNIFIED_MAILBOX_ID,
                name = "",
                email = "",
                color = null,
                unreadCount = unifiedSnapshot.unreadCount,
            ),
        ) + accounts.zip(accountSnapshots) { account, snapshot ->
            WearMailbox(
                id = account.uuid,
                name = account.name?.takeIf { it.isNotBlank() } ?: account.email,
                email = account.email,
                color = account.profile.color,
                unreadCount = snapshot.unreadCount,
            )
        }

        return WearPublication(
            mailboxes = WearMailboxList(generatedAt = generatedAt, mailboxes = mailboxes),
            inboxes = listOf(unifiedSnapshot) + accountSnapshots,
        )
    }

    private fun loadAccountInbox(account: LegacyAccount, generatedAt: Long): WearInboxSnapshot {
        // The inbox folder is unknown until the account's folder list has been synced for the first time.
        val search = account.inboxFolderId?.let { inboxFolderId ->
            LocalMessageSearch().apply {
                addAccountUuid(account.uuid)
                addAllowedFolder(inboxFolderId)
            }
        }

        return WearInboxSnapshot(
            mailboxId = account.uuid,
            generatedAt = generatedAt,
            unreadCount = search?.let { messageCountsProvider.getMessageCounts(it).unread } ?: 0,
            messages = search?.let { loadMessages(listOf(account), it) }.orEmpty(),
        )
    }

    private fun loadMessages(accounts: List<LegacyAccount>, search: LocalMessageSearch): List<WearMessageSummary> {
        val whereClause = SqlWhereClause.Builder()
            .withConditions(search.conditions)
            .build()
        val selectionArgs = whereClause.selectionArgs.toTypedArray()

        return accounts
            .flatMap { account ->
                val mapper = WearMessageSummaryMapper(
                    accountUuid = account.uuid,
                    accountColor = account.profile.color,
                    senderName = { address -> messageHelper.getSenderDisplayName(address).toString() },
                )
                messageListRepository.getMessages(
                    account.uuid,
                    whereClause.selection,
                    selectionArgs,
                    SORT_ORDER,
                    mapper,
                ).filterNotNull()
            }
            .sortedByDescending { it.date }
            .take(WearCompanion.MAX_MESSAGES)
    }

    private companion object {
        const val SORT_ORDER = "${MessageColumns.DATE} DESC, ${MessageColumns.ID} DESC"
    }
}
