@file:Suppress("MagicNumber")

package net.thunderbird.wear.ui.preview

import kotlinx.collections.immutable.persistentListOf
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.ui.inbox.InboxUiState
import net.thunderbird.wear.ui.reader.MessageActions

/** Sample data for Compose previews only. */
internal object PreviewData {
    val mailboxes = persistentListOf(
        WearMailbox(id = WearCompanion.UNIFIED_MAILBOX_ID, name = "", email = "", color = null, unreadCount = 3),
        WearMailbox(id = "work", name = "Work", email = "me@work.example", color = 0xFF0A84FF.toInt(), unreadCount = 2),
        WearMailbox(id = "home", name = "Home", email = "me@home.example", color = 0xFFFF9800.toInt(), unreadCount = 1),
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
            accountColor = 0xFF0A84FF.toInt(),
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
            accountColor = 0xFFFF9800.toInt(),
        ),
    )

    val inboxContent = InboxUiState.Content(
        mailbox = mailboxes.first(),
        canSwitchMailbox = true,
        messages = messages,
        isRefreshing = false,
    )

    val noOpMessageActions = object : MessageActions {
        override fun onOpenOnPhone() = Unit
        override fun onToggleRead() = Unit
        override fun onToggleStar() = Unit
        override fun onArchive() = Unit
        override fun onDelete() = Unit
        override fun onDismissOpenOnPhoneConfirmation() = Unit
    }
}
