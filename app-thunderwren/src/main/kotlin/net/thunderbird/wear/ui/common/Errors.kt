package net.thunderbird.wear.ui.common

import androidx.annotation.StringRes
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneResult

/**
 * The message to show for a failed request, or `null` if it succeeded.
 *
 * @param notAvailableMessage What to say when the phone can't do this for this message or mailbox, for example
 * archive in an account without an Archive folder.
 */
@StringRes
fun PhoneResult.errorMessage(@StringRes notAvailableMessage: Int = R.string.error_failed): Int? = when (this) {
    PhoneResult.Success -> null

    PhoneResult.NoPhone -> R.string.error_no_phone

    PhoneResult.NotAvailableInDemo -> R.string.error_not_available_in_demo

    is PhoneResult.Failed -> when (reason) {
        WearErrorReason.ACTION_NOT_AVAILABLE -> notAvailableMessage

        WearErrorReason.MESSAGE_NOT_FOUND -> R.string.message_not_found

        WearErrorReason.MAILBOX_NOT_FOUND -> R.string.error_mailbox_not_found

        // An older Thunderbird on the phone doesn't know the request.
        WearErrorReason.UNSUPPORTED_REQUEST -> R.string.error_update_phone_app

        WearErrorReason.FAILED -> R.string.error_failed
    }
}
