package net.thunderbird.feature.wear.companion.internal

import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.search.legacy.SearchAccount
import net.thunderbird.feature.search.legacy.api.MessageSearchField
import net.thunderbird.feature.search.legacy.api.SearchAttribute
import net.thunderbird.feature.wear.companion.WearCompanion

/** The message searches behind the watch's mailboxes. */
internal object WearMailboxSearches {
    /** The unified inbox and its views, in the order the watch lists them before the accounts. */
    val unifiedViewIds = listOf(
        WearCompanion.UNIFIED_MAILBOX_ID,
        WearCompanion.UNREAD_MAILBOX_ID,
        WearCompanion.STARRED_MAILBOX_ID,
    )

    /**
     * The search for the unified inbox (the same set of folders as the message list widget) or one of its views, or
     * `null` if [mailboxId] isn't one of [unifiedViewIds].
     */
    fun unifiedView(mailboxId: String): LocalMessageSearch? {
        return when (mailboxId) {
            WearCompanion.UNIFIED_MAILBOX_ID -> unifiedInbox()

            WearCompanion.UNREAD_MAILBOX_ID -> unifiedInbox().onlyUnread()

            WearCompanion.STARRED_MAILBOX_ID -> unifiedInbox().apply {
                and(MessageSearchField.FLAGGED, "1", SearchAttribute.EQUALS)
            }

            else -> null
        }
    }

    /** The search for an account's inbox, or `null` until the account's folder list has been synced once. */
    fun accountInbox(accountUuid: String, inboxFolderId: Long?): LocalMessageSearch? {
        return inboxFolderId?.let {
            LocalMessageSearch().apply {
                addAccountUuid(accountUuid)
                addAllowedFolder(inboxFolderId)
            }
        }
    }

    /** Narrows the search to unread messages. */
    fun LocalMessageSearch.onlyUnread(): LocalMessageSearch = apply {
        and(MessageSearchField.READ, "0", SearchAttribute.EQUALS)
    }

    private fun unifiedInbox(): LocalMessageSearch {
        // The title and detail are only used for display, which the watch does itself.
        return SearchAccount.createUnifiedFoldersSearch(title = "", detail = "").relatedSearch
    }
}
