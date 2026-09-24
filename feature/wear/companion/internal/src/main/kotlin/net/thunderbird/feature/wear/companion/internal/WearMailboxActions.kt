package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearResponse
import net.thunderbird.feature.wear.companion.internal.WearMailboxSearches.onlyUnread

/** Applies a watch action to a whole mailbox. Must be called off the main thread. */
internal fun interface WearMailboxActions {
    fun markAllRead(mailboxId: String): WearResponse
}

internal class MessagingControllerWearMailboxActions(
    private val messagingController: MessagingController,
    private val accountManager: LegacyAccountDtoManager,
    private val messageListRepository: MessageListRepository,
) : WearMailboxActions {

    override fun markAllRead(mailboxId: String): WearResponse {
        val mailbox = findMailbox(mailboxId) ?: return WearResponse.Error(WearErrorReason.MAILBOX_NOT_FOUND)

        // Without a known inbox there is nothing to mark.
        mailbox.search?.let { search ->
            val whereClause = SqlWhereClause.Builder()
                .withConditions(search.onlyUnread().conditions)
                .build()
            for (account in mailbox.accounts) {
                markRead(account, whereClause)
            }
        }

        return WearResponse.Ok
    }

    private fun findMailbox(mailboxId: String): Mailbox? {
        val unifiedView = WearMailboxSearches.unifiedView(mailboxId)
        return if (unifiedView != null) {
            Mailbox(accountManager.getAccounts(), unifiedView)
        } else {
            accountManager.getAccount(mailboxId)?.let { account ->
                Mailbox(listOf(account), WearMailboxSearches.accountInbox(account.uuid, account.inboxFolderId))
            }
        }
    }

    /** Marks the messages matching [whereClause] as read, like selecting them all in the message list. */
    private fun markRead(account: LegacyAccountDto, whereClause: SqlWhereClause) {
        val messageIds = messageListRepository.getMessages(
            account.uuid,
            whereClause.selection,
            whereClause.selectionArgs.toTypedArray(),
            InboxPublicationSource.SORT_ORDER,
            MessageMapper { message -> message.id },
        )

        if (messageIds.isNotEmpty()) {
            messagingController.setFlag(account, messageIds, Flag.SEEN, true)
        }
    }

    /** The accounts a mailbox covers, and its search, which is `null` for an account whose inbox isn't known yet. */
    private class Mailbox(val accounts: List<LegacyAccountDto>, val search: LocalMessageSearch?)
}
