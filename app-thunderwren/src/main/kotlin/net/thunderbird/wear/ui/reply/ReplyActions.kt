package net.thunderbird.wear.ui.reply

/** User actions on the reply screen. */
interface ReplyActions {
    /** Opens the system's voice and keyboard input. */
    fun onSpeakOrType()
    fun onQuickReply(text: String)
    fun onSend()
    fun onChange()

    /** The "sent" confirmation has been shown; the screen should close. */
    fun onSent()
}
