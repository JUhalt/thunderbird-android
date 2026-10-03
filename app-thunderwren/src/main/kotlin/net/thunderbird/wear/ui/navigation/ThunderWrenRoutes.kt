package net.thunderbird.wear.ui.navigation

import android.net.Uri

object ThunderWrenRoutes {
    const val INBOX = "inbox"
    const val MAILBOXES = "mailboxes"
    const val SETTINGS = "settings"
    const val FOLDERS = "folders/{accountId}"
    const val FOLDER = "folder/{accountId}/{folderId}"
    const val MESSAGE_DETAIL = "message/{mailboxId}/{messageId}"
    const val REPLY = "reply/{mailboxId}/{messageId}"

    const val ARG_MAILBOX_ID = "mailboxId"
    const val ARG_MESSAGE_ID = "messageId"
    const val ARG_ACCOUNT_ID = "accountId"
    const val ARG_FOLDER_ID = "folderId"

    /** The folders of the account with [accountId]. */
    fun folders(accountId: String) = "folders/${Uri.encode(accountId)}"

    /** The messages of the folder with [folderId] of the account with [accountId]. */
    fun folder(accountId: String, folderId: Long) = "folder/${Uri.encode(accountId)}/$folderId"

    /** The message with [messageId], opened from the mailbox with [mailboxId]. */
    fun messageDetail(mailboxId: String, messageId: String) = "message/${encode(mailboxId, messageId)}"

    fun reply(mailboxId: String, messageId: String) = "reply/${encode(mailboxId, messageId)}"

    // Message IDs contain characters like '/' and '=', so they must be encoded in the route.
    private fun encode(mailboxId: String, messageId: String) = "${Uri.encode(mailboxId)}/${Uri.encode(messageId)}"
}
