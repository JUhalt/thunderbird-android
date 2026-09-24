package net.thunderbird.wear.ui.navigation

object ThunderWrenRoutes {
    const val INBOX = "inbox"
    const val FOLDERS = "folders"
    const val MESSAGE_DETAIL = "message/{messageId}"
    const val QUICK_REPLY = "reply/{messageId}"

    const val ARG_MESSAGE_ID = "messageId"

    fun messageDetail(messageId: String) = "message/$messageId"
    fun quickReply(messageId: String) = "reply/$messageId"
}
