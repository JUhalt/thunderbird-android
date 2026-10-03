package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageDetailsAccessor
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.extractors.PreviewResult.PreviewType
import com.fsck.k9.mail.Address
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMessageSummary

/**
 * Maps the first [limit] messages of a query to [WearMessageSummary]s and skips the rest by returning `null`.
 *
 * Queries are sorted newest first, so this keeps the newest messages without mapping the whole folder.
 *
 * Messages in the account's Sent folder ([sentFolderId]) name their recipients instead of the sender, like the phone's
 * message list does.
 */
internal class WearMessageSummaryMapper(
    private val accountUuid: String,
    private val accountColor: Int,
    private val senderName: (Address?) -> String,
    private val sentFolderId: Long? = null,
    private val limit: Int = WearCompanion.MAX_MESSAGES,
) : MessageMapper<WearMessageSummary?> {
    private var mappedCount = 0

    override fun map(message: MessageDetailsAccessor): WearMessageSummary? {
        if (mappedCount >= limit) return null
        mappedCount++

        val isOutgoing = sentFolderId != null && message.folderId == sentFolderId
        val sender = message.fromAddresses.firstOrNull()
        val recipients = message.toAddresses
        val preview = message.preview

        return WearMessageSummary(
            id = MessageReference(accountUuid, message.folderId, message.messageServerId).toIdentityString(),
            senderName = if (isOutgoing && recipients.isNotEmpty()) {
                recipients.take(MAX_RECIPIENT_NAMES).joinToString(", ") { senderName(it) }
            } else {
                senderName(sender)
            },
            senderAddress = if (isOutgoing) recipients.firstOrNull()?.address.orEmpty() else sender?.address.orEmpty(),
            subject = message.subject.orEmpty(),
            preview = if (preview.isPreviewTextAvailable) {
                preview.previewText.take(WearCompanion.MAX_PREVIEW_LENGTH)
            } else {
                ""
            },
            date = message.messageDate,
            isRead = message.isRead,
            isStarred = message.isStarred,
            hasAttachments = message.hasAttachments,
            isEncrypted = preview.previewType == PreviewType.ENCRYPTED,
            accountColor = accountColor,
            accountId = accountUuid,
            isOutgoing = isOutgoing,
        )
    }

    private companion object {
        /** A watch shows one line of names, so a few are plenty. */
        const val MAX_RECIPIENT_NAMES = 3
    }
}
