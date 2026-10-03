package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.message.controller.MessageCountsProvider
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.MessageColumns
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMessageSummary

/** Loads the newest messages of a mailbox or folder, as the watch shows them. Must be called off the main thread. */
internal class WearSnapshotLoader(
    private val messageListRepository: MessageListRepository,
    private val messageCountsProvider: MessageCountsProvider,
    private val messageHelper: MessageHelper,
) {

    /** [search] is `null` for an account whose inbox isn't known yet; its snapshot is empty. */
    fun load(
        mailboxId: String,
        accounts: List<LegacyAccount>,
        search: LocalMessageSearch?,
        generatedAt: Long,
    ): WearInboxSnapshot {
        return WearInboxSnapshot(
            mailboxId = mailboxId,
            generatedAt = generatedAt,
            unreadCount = search?.let { messageCountsProvider.getMessageCounts(it).unread } ?: 0,
            messages = search?.let { loadMessages(accounts, it) }.orEmpty(),
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
                    sentFolderId = account.sentFolderId,
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

    companion object {
        /** Newest first, like the message list. */
        const val SORT_ORDER = "${MessageColumns.DATE} DESC, ${MessageColumns.ID} DESC"
    }
}
