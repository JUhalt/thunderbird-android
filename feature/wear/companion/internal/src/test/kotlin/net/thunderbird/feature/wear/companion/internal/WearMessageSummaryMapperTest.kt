package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.extractors.PreviewResult
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMessageSummary

class WearMessageSummaryMapperTest {

    private val testSubject = WearMessageSummaryMapper(
        accountUuid = "account-1",
        accountColor = 0x112233,
        senderName = { address -> "Name of ${address?.address}" },
        limit = 2,
    )

    @Test
    fun `maps message details`() {
        val result = testSubject.map(
            FakeMessageDetailsAccessor(
                folderId = 7,
                messageServerId = "uid-1",
                subject = "Hello",
                preview = PreviewResult.text("Preview text"),
                messageDate = 1_234,
                isRead = true,
                isStarred = true,
                hasAttachments = true,
            ),
        )

        assertThat(result).isEqualTo(
            WearMessageSummary(
                id = MessageReference("account-1", 7, "uid-1").toIdentityString(),
                senderName = "Name of ada@example.com",
                senderAddress = "ada@example.com",
                subject = "Hello",
                preview = "Preview text",
                date = 1_234,
                isRead = true,
                isStarred = true,
                hasAttachments = true,
                isEncrypted = false,
                accountColor = 0x112233,
                accountId = "account-1",
            ),
        )
    }

    @Test
    fun `message ID can be parsed back into the message reference`() {
        val result = testSubject.map(FakeMessageDetailsAccessor(folderId = 3, messageServerId = "abc"))

        assertThat(MessageReference.parse(result?.id)).isEqualTo(MessageReference("account-1", 3, "abc"))
    }

    @Test
    fun `encrypted message is flagged and has no preview`() {
        val result = testSubject.map(FakeMessageDetailsAccessor(preview = PreviewResult.encrypted()))

        assertThat(result).isNotNull().prop(WearMessageSummary::isEncrypted).isTrue()
        assertThat(result).isNotNull().prop(WearMessageSummary::preview).isEqualTo("")
    }

    @Test
    fun `long preview is truncated and missing subject is empty`() {
        val longPreview = "x".repeat(WearCompanion.MAX_PREVIEW_LENGTH + 50)

        val result = testSubject.map(
            FakeMessageDetailsAccessor(preview = PreviewResult.text(longPreview), subject = null),
        )

        assertThat(
            result,
        ).isNotNull().prop(WearMessageSummary::preview).isEqualTo("x".repeat(WearCompanion.MAX_PREVIEW_LENGTH))
        assertThat(result).isNotNull().prop(WearMessageSummary::subject).isEqualTo("")
        assertThat(result).isNotNull().prop(WearMessageSummary::isEncrypted).isFalse()
    }

    @Test
    fun `messages past the limit are skipped`() {
        val results = List(3) { testSubject.map(FakeMessageDetailsAccessor(messageServerId = "uid-$it")) }

        assertThat(results[0]).isNotNull()
        assertThat(results[1]).isNotNull()
        assertThat(results[2]).isNull()
    }
}
