@file:Suppress("MagicNumber")

package net.thunderbird.wear.ui.preview

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.ui.inbox.InboxContract

/** Sample data for Compose previews only. */
internal object PreviewData {
    private const val WORK_COLOR = 0xFF0A84FF.toInt()
    private const val HOME_COLOR = 0xFFFF9800.toInt()

    private val work = WearMailbox(
        id = "work",
        name = "Work",
        email = "me@work.example",
        color = WORK_COLOR,
        unreadCount = 2,
        monogram = "WO",
    )
    private val home = WearMailbox(
        id = "home",
        name = "Home",
        email = "me@home.example",
        color = HOME_COLOR,
        unreadCount = 1,
        monogram = "HO",
    )

    val mailboxes = persistentListOf(
        WearMailbox(id = WearCompanion.UNIFIED_MAILBOX_ID, name = "", email = "", color = null, unreadCount = 3),
        WearMailbox(id = WearCompanion.UNREAD_MAILBOX_ID, name = "", email = "", color = null, unreadCount = 3),
        WearMailbox(id = WearCompanion.STARRED_MAILBOX_ID, name = "", email = "", color = null, unreadCount = 1),
        work,
        home,
    )

    val messages = persistentListOf(
        WearMessageSummary(
            id = "1",
            senderName = "Ada Lovelace",
            senderAddress = "ada@example.com",
            subject = "Notes on the Analytical Engine",
            preview = "I've attached my notes. The engine might act upon other things besides number.",
            date = 1_700_000_000_000,
            isRead = false,
            isStarred = true,
            hasAttachments = true,
            isEncrypted = false,
            accountColor = WORK_COLOR,
            accountId = work.id,
        ),
        WearMessageSummary(
            id = "2",
            senderName = "Charles Babbage",
            senderAddress = "charles@example.com",
            subject = "Re: Difference Engine",
            preview = "",
            date = 1_699_900_000_000,
            isRead = true,
            isStarred = false,
            hasAttachments = false,
            isEncrypted = true,
            accountColor = HOME_COLOR,
            accountId = home.id,
        ),
    )

    val folders = persistentListOf(
        WearFolder(id = 1, name = "Inbox", type = WearFolderType.INBOX, unreadCount = 1),
        WearFolder(id = 2, name = "Sent", type = WearFolderType.SENT, unreadCount = 0),
        WearFolder(id = 3, name = "Archive", type = WearFolderType.ARCHIVE, unreadCount = 0),
        WearFolder(id = 4, name = "Trash", type = WearFolderType.TRASH, unreadCount = 0),
        WearFolder(id = 5, name = "Receipts", type = WearFolderType.REGULAR, unreadCount = 2),
    )

    val inboxContent = InboxContract.State.Content(
        mailbox = mailboxes.first(),
        canSwitchMailbox = true,
        messages = messages,
        isRefreshing = false,
        accounts = listOf(work, home).associateBy { it.id }.toImmutableMap(),
    )
}
