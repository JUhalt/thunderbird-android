package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.message.controller.MessageCountsProvider
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mailstore.MessageColumns
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.LockScreenNotificationVisibility
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.sql.SqlWhereClause
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageSummary

/** Everything the phone publishes for the watch: the mailbox list and one snapshot per mailbox. */
internal data class WearPublication(
    val mailboxes: WearMailboxList,
    val inboxes: List<WearInboxSnapshot>,
)

internal fun interface WearPublicationSource {
    fun load(): WearPublication
}

/**
 * Loads the unified inbox, its unread and starred views, and each account's inbox.
 */
@OptIn(ExperimentalTime::class)
@Suppress("LongParameterList")
internal class InboxPublicationSource(
    private val accountManager: LegacyAccountManager,
    private val messageListRepository: MessageListRepository,
    private val messageCountsProvider: MessageCountsProvider,
    private val messageHelper: MessageHelper,
    private val monogramCreator: AvatarMonogramCreator,
    private val lockScreenNotificationVisibility: () -> LockScreenNotificationVisibility,
    private val clock: Clock,
) : WearPublicationSource {

    override fun load(): WearPublication {
        val generatedAt = clock.now().toEpochMilliseconds()
        val accounts = accountManager.getAccounts()

        val viewSnapshots = WearMailboxSearches.unifiedViewIds.map { mailboxId ->
            loadSnapshot(mailboxId, accounts, WearMailboxSearches.unifiedView(mailboxId), generatedAt)
        }
        val accountSnapshots = accounts.map { account ->
            val search = WearMailboxSearches.accountInbox(account.uuid, account.inboxFolderId)
            loadSnapshot(account.uuid, listOf(account), search, generatedAt)
        }

        val mailboxes = viewSnapshots.map { snapshot ->
            WearMailbox(
                id = snapshot.mailboxId,
                name = "",
                email = "",
                color = null,
                unreadCount = snapshot.unreadCount,
            )
        } + accounts.zip(accountSnapshots) { account, snapshot ->
            WearMailbox(
                id = account.uuid,
                name = account.displayName,
                email = account.email,
                color = account.profile.color,
                unreadCount = snapshot.unreadCount,
                monogram = account.monogram(),
            )
        }

        return WearPublication(
            mailboxes = WearMailboxList(
                generatedAt = generatedAt,
                mailboxes = mailboxes,
                glanceVisibility = lockScreenNotificationVisibility().toGlanceVisibility(),
            ),
            inboxes = viewSnapshots + accountSnapshots,
        )
    }

    /** [search] is `null` for an account whose inbox isn't known yet; its snapshot is empty. */
    private fun loadSnapshot(
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

    private val LegacyAccount.displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: email

    /** The monogram the phone shows for the account, or one made from its name if it shows a picture or icon. */
    private fun LegacyAccount.monogram(): String {
        val avatar = profile.avatar
        return avatar.avatarMonogram?.takeIf { avatar.avatarType == AvatarTypeDto.MONOGRAM && it.isNotBlank() }
            ?: monogramCreator.create(name, email)
    }

    companion object {
        /** Newest first, like the message list. */
        const val SORT_ORDER = "${MessageColumns.DATE} DESC, ${MessageColumns.ID} DESC"
    }
}

/**
 * The watch's Tile and complication can be seen by people nearby, like a phone's lock screen, so they follow the lock
 * screen notification setting.
 */
internal fun LockScreenNotificationVisibility.toGlanceVisibility(): WearGlanceVisibility = when (this) {
    LockScreenNotificationVisibility.EVERYTHING -> WearGlanceVisibility.EVERYTHING
    LockScreenNotificationVisibility.SENDERS -> WearGlanceVisibility.SENDERS
    LockScreenNotificationVisibility.MESSAGE_COUNT -> WearGlanceVisibility.COUNT
    LockScreenNotificationVisibility.APP_NAME, LockScreenNotificationVisibility.NOTHING -> WearGlanceVisibility.NOTHING
}
