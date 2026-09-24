package net.thunderbird.wear.ui.reader

/** User actions on the message screen. */
interface MessageActions {
    fun onOpenOnPhone()
    fun onToggleRead()
    fun onToggleStar()
    fun onArchive()
    fun onDelete()
    fun onDismissOpenOnPhoneConfirmation()
}
