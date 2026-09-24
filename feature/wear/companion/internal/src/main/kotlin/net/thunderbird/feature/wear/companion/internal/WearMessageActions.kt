package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.controller.MessagingController
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearResponse

/** Applies a watch action to a message. Must be called off the main thread. */
internal fun interface WearMessageActions {
    fun perform(reference: MessageReference, action: WearMessageAction): WearResponse
}

internal class MessagingControllerWearMessageActions(
    private val messagingController: MessagingController,
    private val accountManager: LegacyAccountDtoManager,
) : WearMessageActions {

    override fun perform(reference: MessageReference, action: WearMessageAction): WearResponse {
        val account = accountManager.getAccount(reference.accountUuid)

        return when {
            account == null -> WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)

            action == WearMessageAction.ARCHIVE && account.archiveFolderId == null -> {
                WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE)
            }

            else -> {
                apply(account, reference, action)
                WearResponse.Ok
            }
        }
    }

    private fun apply(account: LegacyAccountDto, reference: MessageReference, action: WearMessageAction) {
        when (action) {
            WearMessageAction.MARK_READ -> setFlag(account, reference, Flag.SEEN, true)
            WearMessageAction.MARK_UNREAD -> setFlag(account, reference, Flag.SEEN, false)
            WearMessageAction.STAR -> setFlag(account, reference, Flag.FLAGGED, true)
            WearMessageAction.UNSTAR -> setFlag(account, reference, Flag.FLAGGED, false)
            WearMessageAction.ARCHIVE -> messagingController.archiveMessage(reference)
            WearMessageAction.DELETE -> messagingController.deleteMessages(listOf(reference))
        }
    }

    private fun setFlag(account: LegacyAccountDto, reference: MessageReference, flag: Flag, value: Boolean) {
        messagingController.setFlag(account, reference.folderId, reference.uid, flag, value)
    }
}
