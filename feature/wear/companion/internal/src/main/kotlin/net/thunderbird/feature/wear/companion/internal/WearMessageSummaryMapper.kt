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
 */
internal class WearMessageSummaryMapper(
    private val accountUuid: String,
    private val accountColor: Int,
    private val senderName: (Address?) -> String,
    private val limit: Int = WearCompanion.MAX_MESSAGES,
) : MessageMapper<WearMessageSummary?> {
    private var mappedCount = 0

    override fun map(message: MessageDetailsAccessor): WearMessageSummary? {
        if (mappedCount >= limit) return null
        mappedCount++

        val sender = message.fromAddresses.firstOrNull()
        val preview = message.preview

        return WearMessageSummary(
            id = MessageReference(accountUuid, message.folderId, message.messageServerId).toIdentityString(),
            senderName = senderName(sender),
            senderAddress = sender?.address.orEmpty(),
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
        )
    }
}
