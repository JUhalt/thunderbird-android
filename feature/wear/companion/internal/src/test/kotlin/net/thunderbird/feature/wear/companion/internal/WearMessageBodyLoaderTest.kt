package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Message
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.exception.MessagingException
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearResponse
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class WearMessageBodyLoaderTest {
    private val account = LegacyAccountDto(ACCOUNT_UUID)
    private val accountManager = mock<LegacyAccountDtoManager> {
        on { getAccount(ACCOUNT_UUID) } doReturn account
    }
    private val reference = MessageReference(ACCOUNT_UUID, 3, "uid")

    @Test
    fun `fully downloaded message is sent in full`() {
        val testSubject = createTestSubject(message(isFullyDownloaded = true), text = "  Hello,\n\nsee you at 10.  ")

        val response = testSubject.load(reference)

        assertThat(response).isEqualTo(WearResponse.Body(text = "Hello,\n\nsee you at 10.", isComplete = true))
    }

    @Test
    fun `partly downloaded message is marked as incomplete`() {
        val testSubject = createTestSubject(message(isFullyDownloaded = false), text = "The first part")

        val response = testSubject.load(reference)

        assertThat(response).isEqualTo(WearResponse.Body(text = "The first part", isComplete = false))
    }

    @Test
    fun `long text is shortened and marked as incomplete`() {
        val longText = "x".repeat(WearCompanion.MAX_BODY_LENGTH + 10)
        val testSubject = createTestSubject(message(isFullyDownloaded = true), text = longText)

        val response = testSubject.load(reference)

        assertThat(response).isEqualTo(
            WearResponse.Body(text = "x".repeat(WearCompanion.MAX_BODY_LENGTH), isComplete = false),
        )
    }

    @Test
    fun `encrypted message is refused`() {
        val testSubject = createTestSubject(message(isFullyDownloaded = true), text = "secret", isEncrypted = true)

        val response = testSubject.load(reference)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE))
    }

    @Test
    fun `message of a removed account is not found`() {
        val testSubject = createTestSubject(message(isFullyDownloaded = true), text = "text")

        val response = testSubject.load(MessageReference("11111111-2222-4333-8444-555555555555", 3, "uid"))

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND))
    }

    @Test
    fun `message that is gone is not found`() {
        val testSubject = createTestSubject(load = { throw IllegalArgumentException("Message not found") })

        val response = testSubject.load(reference)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND))
    }

    @Test
    fun `message that can't be loaded is reported as failed`() {
        val testSubject = createTestSubject(load = { throw MessagingException("Database error") })

        val response = testSubject.load(reference)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.FAILED))
    }

    private fun message(isFullyDownloaded: Boolean): Message = mock {
        on { isSet(Flag.X_DOWNLOADED_FULL) } doReturn isFullyDownloaded
    }

    private fun createTestSubject(
        message: Message? = null,
        text: String = "",
        isEncrypted: Boolean = false,
        load: () -> Message = { checkNotNull(message) },
    ) = LocalStoreWearMessageBodyLoader(
        accountManager = accountManager,
        messagingController = mock<MessagingController>(),
        logger = TestLogger(),
        loadMessage = { _, _ -> load() },
        extractText = { text },
        isEncrypted = { isEncrypted },
    )

    private companion object {
        const val ACCOUNT_UUID = "a0b1c2d3-e4f5-4a6b-8c7d-9e0f1a2b3c4d"
    }
}
