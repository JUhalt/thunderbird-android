package com.fsck.k9.message

import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.crypto.MessageCryptoStructureDetector
import com.fsck.k9.helper.IdentityHelper
import com.fsck.k9.helper.ReplyToParser
import com.fsck.k9.helper.toCrLf
import com.fsck.k9.helper.toLf
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.message.extractors.BodyTextExtractor
import com.fsck.k9.message.quote.TextQuoteCreator
import com.fsck.k9.message.signature.TextSignatureRemover
import java.util.Date
import java.util.Locale
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.legacy.logging.Log

/**
 * Sends a short plain-text reply without opening the compose screen, for example from a watch.
 *
 * The reply is built like one written on the phone: it goes to the sender (or the Reply-To address), from the identity
 * the message was sent to, with the account's quoting, signature, Bcc, and read receipt settings. The original message
 * is marked as answered. Encrypted messages are refused, because a plain-text reply could leak what they say.
 *
 * Must be called off the main thread.
 */
class QuickReplySender(
    private val accountManager: LegacyAccountDtoManager,
    private val messagingController: MessagingController,
    private val textQuoteCreator: TextQuoteCreator,
    private val generalSettingsManager: GeneralSettingsManager,
    private val replyToParser: ReplyToParser = ReplyToParser(),
    private val createMessageBuilder: () -> MessageBuilder = SimpleMessageBuilder::newInstance,
    private val loadMessage: (LegacyAccountDto, MessageReference) -> Message = { account, reference ->
        messagingController.loadMessage(account, reference.folderId, reference.uid)
    },
    private val now: () -> Date = ::Date,
) {

    fun sendReply(messageReference: MessageReference, text: String): QuickReplyResult {
        val account = accountManager.getAccount(messageReference.accountUuid)
        val original = account?.let { loadOriginal(it, messageReference) }

        return if (account == null || original == null) {
            QuickReplyResult.MESSAGE_NOT_FOUND
        } else {
            reply(account, messageReference, original, text)
        }
    }

    private fun loadOriginal(account: LegacyAccountDto, messageReference: MessageReference): Message? {
        return try {
            loadMessage(account, messageReference)
        } catch (e: IllegalArgumentException) {
            Log.w(e, "Message to reply to is gone")
            null
        } catch (e: MessagingException) {
            Log.w(e, "Couldn't load the message to reply to")
            null
        }
    }

    private fun reply(
        account: LegacyAccountDto,
        messageReference: MessageReference,
        original: Message,
        text: String,
    ): QuickReplyResult {
        val recipients = replyToParser.getRecipientsToReplyTo(original, account).to
        if (original.isEncrypted() || recipients.isEmpty()) return QuickReplyResult.NOT_AVAILABLE

        val reply = try {
            buildReply(account, original, recipients, text)
        } catch (e: MessagingException) {
            Log.e(e, "Couldn't build the reply")
            null
        }

        reply?.let {
            messagingController.sendMessage(account, it, null, null)
            messagingController.setFlag(account, messageReference.folderId, messageReference.uid, Flag.ANSWERED, true)
        }

        return if (reply != null) QuickReplyResult.SENT else QuickReplyResult.FAILED
    }

    private fun buildReply(
        account: LegacyAccountDto,
        original: Message,
        recipients: Array<Address>,
        text: String,
    ): Message {
        val identity = IdentityHelper.getRecipientIdentityFromMessage(account, original)
        val quotedText = createQuotedText(account, original)

        return createMessageBuilder()
            .setSubject(replySubject(original.subject))
            .setSentDate(now())
            .setHideTimeZone(generalSettingsManager.getConfig().privacy.isHideTimeZone)
            .setTo(recipients.toList())
            .setCc(emptyList())
            .setBcc(Address.parse(account.alwaysBcc).toList())
            .setReplyTo(Address.parse(identity.replyTo))
            .setInReplyTo(original.messageId)
            .setReferences(references(original))
            .setRequestReadReceipt(account.isMessageReadReceipt)
            .setIdentity(identity)
            .setMessageFormat(SimpleMessageFormat.TEXT)
            .setText(text.withCrLf())
            .setAttachments(emptyList())
            .setInlineAttachments(emptyMap())
            .setSignature(identity.signature.withCrLf())
            .setSignatureBeforeQuotedText(account.isSignatureBeforeQuotedText)
            .setQuoteStyle(account.quoteStyle)
            .setQuotedTextMode(if (account.isDefaultQuotedTextShown) QuotedTextMode.SHOW else QuotedTextMode.HIDE)
            .setQuotedText(quotedText)
            .setReplyAfterQuote(account.isReplyAfterQuote)
            .setDraft(false)
            .build()
    }

    private fun createQuotedText(account: LegacyAccountDto, original: Message): String {
        val body = BodyTextExtractor.getBodyTextFromMessage(original, SimpleMessageFormat.TEXT).orEmpty()
        val content = if (account.isStripSignature) TextSignatureRemover.stripSignature(body) else body

        val quotedText = textQuoteCreator.quoteOriginalTextMessage(
            originalMessage = original,
            messageBody = content,
            quoteStyle = account.quoteStyle,
            prefix = account.quotePrefix.orEmpty(),
        )
        return quotedText.withCrLf()
    }

    /** Messages use CRLF line breaks; the compose screen converts its text the same way. */
    private fun String?.withCrLf(): String = toLf().toCrLf().orEmpty()

    private fun Message.isEncrypted(): Boolean {
        return (this as? LocalMessage)?.previewType == PreviewType.ENCRYPTED ||
            MessageCryptoStructureDetector.findMultipartEncryptedParts(this).isNotEmpty() ||
            MessageCryptoStructureDetector.findPgpInlineParts(this)
                .any { MessageCryptoStructureDetector.isPartPgpInlineEncrypted(it) }
    }

    internal companion object {
        /** A localized "Re:" prefix that is replaced, as on the compose screen: German "AW:". */
        private val LOCALIZED_REPLY_PREFIX = Regex("^AW[:\\s]\\s*", RegexOption.IGNORE_CASE)

        fun replySubject(subject: String?): String {
            if (subject == null) return ""

            val strippedSubject = subject.replaceFirst(LOCALIZED_REPLY_PREFIX, "")
            val hasReplyPrefix = strippedSubject.lowercase(Locale.US).startsWith("re:")
            return if (hasReplyPrefix) strippedSubject else "Re: $strippedSubject"
        }

        /** The original's references followed by its message ID, so the reply threads correctly. */
        fun references(original: Message): String? {
            val messageId = original.messageId?.takeIf { it.isNotEmpty() } ?: return null
            val references = original.references.orEmpty()
            return if (references.isEmpty()) messageId else references.joinToString("") + " " + messageId
        }
    }
}

enum class QuickReplyResult {
    /** The reply was put in the Outbox, which sends it as soon as possible. */
    SENT,

    /** The message or its account no longer exists. */
    MESSAGE_NOT_FOUND,

    /** A quick reply isn't possible, for example because the message is encrypted or has nobody to reply to. */
    NOT_AVAILABLE,

    /** The reply couldn't be created. */
    FAILED,
}
