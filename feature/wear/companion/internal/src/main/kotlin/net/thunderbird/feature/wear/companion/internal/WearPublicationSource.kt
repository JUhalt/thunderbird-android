package net.thunderbird.feature.wear.companion.internal

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.core.preference.LockScreenNotificationVisibility
import net.thunderbird.feature.account.avatar.AvatarMonogramCreator
import net.thunderbird.feature.account.storage.profile.AvatarTypeDto
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList

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
internal class InboxPublicationSource(
    private val accountManager: LegacyAccountManager,
    private val snapshotLoader: WearSnapshotLoader,
    private val monogramCreator: AvatarMonogramCreator,
    private val lockScreenNotificationVisibility: () -> LockScreenNotificationVisibility,
    private val clock: Clock,
) : WearPublicationSource {

    override fun load(): WearPublication {
        val generatedAt = clock.now().toEpochMilliseconds()
        val accounts = accountManager.getAccounts()

        val viewSnapshots = WearMailboxSearches.unifiedViewIds.map { mailboxId ->
            snapshotLoader.load(mailboxId, accounts, WearMailboxSearches.unifiedView(mailboxId), generatedAt)
        }
        val accountSnapshots = accounts.map { account ->
            val search = WearMailboxSearches.accountInbox(account.uuid, account.inboxFolderId)
            snapshotLoader.load(account.uuid, listOf(account), search, generatedAt)
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

    private val LegacyAccount.displayName: String
        get() = name?.takeIf { it.isNotBlank() } ?: email

    /** The monogram the phone shows for the account, or one made from its name if it shows a picture or icon. */
    private fun LegacyAccount.monogram(): String {
        val avatar = profile.avatar
        return avatar.avatarMonogram?.takeIf { avatar.avatarType == AvatarTypeDto.MONOGRAM && it.isNotBlank() }
            ?: monogramCreator.create(name, email)
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
