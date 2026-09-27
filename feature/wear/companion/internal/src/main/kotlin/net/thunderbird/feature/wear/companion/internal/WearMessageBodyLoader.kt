package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.crypto.MessageCryptoStructureDetector
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.SimpleMessageFormat
import com.fsck.k9.message.extractors.BodyTextExtractor
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearResponse

/** Loads the text of a message for reading it on the watch. Must be called off the main thread. */
internal fun interface WearMessageBodyLoader {
    fun load(reference: MessageReference): WearResponse
}

/**
 * Reads the message from the local store, like the phone's message view, and converts it to plain text. HTML-only
 * messages are converted from HTML. Encrypted messages are refused: the phone would have to decrypt them first, and
 * their text shouldn't leave the phone unencrypted anyway.
 */
internal class LocalStoreWearMessageBodyLoader(
    private val accountManager: LegacyAccountDtoManager,
    private val messagingController: MessagingController,
    private val logger: Logger,
    private val loadMessage: (LegacyAccountDto, MessageReference) -> Message = { account, reference ->
        messagingController.loadMessage(account, reference.folderId, reference.uid)
    },
    private val extractText: (Message) -> String? = { message ->
        BodyTextExtractor.getBodyTextFromMessage(message, SimpleMessageFormat.TEXT)
    },
    private val isEncrypted: (Message) -> Boolean = ::isMessageEncrypted,
) : WearMessageBodyLoader {

    override fun load(reference: MessageReference): WearResponse {
        val account = accountManager.getAccount(reference.accountUuid)
            ?: return WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)

        return try {
            toResponse(loadMessage(account, reference))
        } catch (e: IllegalArgumentException) {
            logger.warn(TAG, e) { "Message to read on the watch is gone" }
            WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND)
        } catch (e: MessagingException) {
            logger.warn(TAG, e) { "Couldn't load the message to read on the watch" }
            WearResponse.Error(WearErrorReason.FAILED)
        }
    }

    private fun toResponse(message: Message): WearResponse {
        if (isEncrypted(message)) return WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE)

        val text = extractText(message).orEmpty().trim()
        return WearResponse.Body(
            text = text.take(WearCompanion.MAX_BODY_LENGTH),
            isComplete = text.length <= WearCompanion.MAX_BODY_LENGTH && message.isSet(Flag.X_DOWNLOADED_FULL),
        )
    }

    private companion object {
        const val TAG = "WearMessageBodyLoader"
    }
}

private fun isMessageEncrypted(message: Message): Boolean {
    return (message as? LocalMessage)?.previewType == PreviewType.ENCRYPTED ||
        MessageCryptoStructureDetector.findMultipartEncryptedParts(message).isNotEmpty() ||
        MessageCryptoStructureDetector.findPgpInlineParts(message)
            .any { MessageCryptoStructureDetector.isPartPgpInlineEncrypted(it) }
}
