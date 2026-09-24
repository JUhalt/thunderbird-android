package net.thunderbird.wear.ui.inbox

/** User actions on the inbox screen. */
interface InboxActions {
    fun onMailboxClick()
    fun onMessageClick(messageId: String)
    fun onArchive(messageId: String)
    fun onDelete(messageId: String)
    fun onMarkAllRead(mailboxId: String)
    fun onRefresh()
    fun onStartDemo()
    fun onExitDemo()
}
