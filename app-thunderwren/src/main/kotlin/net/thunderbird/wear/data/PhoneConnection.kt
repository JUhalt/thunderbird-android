package net.thunderbird.wear.data

import kotlinx.coroutines.flow.Flow
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction

/** The watch's view of Thunderbird on the paired phone. See RFC 0010. */
interface PhoneConnection {
    /** The last mailbox list the phone published, or `null` if none has arrived yet. */
    val mailboxes: Flow<WearMailboxList?>

    /** Whether the data comes from the built-in demo mailbox instead of a phone. */
    val isDemo: Flow<Boolean>

    /** The last inbox snapshot the phone published for [mailboxId], or `null` if none has arrived yet. */
    fun inbox(mailboxId: String): Flow<WearInboxSnapshot?>

    /** Asks the phone to publish fresh data. */
    suspend fun refresh(): PhoneResult

    suspend fun performAction(messageId: String, action: WearMessageAction): PhoneResult

    /** Opens the message on the phone so it can be read in full or replied to. */
    suspend fun openOnPhone(messageId: String): PhoneResult
}

sealed interface PhoneResult {
    data object Success : PhoneResult

    /** No phone with Thunderbird is reachable right now. */
    data object NoPhone : PhoneResult

    data class Failed(val reason: WearErrorReason) : PhoneResult

    /** The demo mailbox can't do this, for example open a message on the phone. */
    data object NotAvailableInDemo : PhoneResult
}
