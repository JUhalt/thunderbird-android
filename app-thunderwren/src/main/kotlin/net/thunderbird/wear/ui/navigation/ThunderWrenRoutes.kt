package net.thunderbird.wear.ui.navigation

import android.net.Uri

object ThunderWrenRoutes {
    const val INBOX = "inbox"
    const val MAILBOXES = "mailboxes"
    const val MESSAGE_DETAIL = "message/{messageId}"

    const val ARG_MESSAGE_ID = "messageId"

    // Message IDs contain characters like '/' and '=', so they must be encoded in the route.
    fun messageDetail(messageId: String) = "message/${Uri.encode(messageId)}"
}
