package com.fsck.k9.message

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.TestCoreResourceProvider
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.mail.Address
import com.fsck.k9.mail.BoundaryGenerator
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.Message.RecipientType
import com.fsck.k9.mail.internet.BinaryTempFileBody
import com.fsck.k9.mail.internet.MessageIdGenerator
import com.fsck.k9.mail.internet.MimeMessage
import com.fsck.k9.mail.internet.MessageExtractor
import com.fsck.k9.message.quote.QuoteDateFormatter
import com.fsck.k9.message.quote.TextQuoteCreator
import java.util.Date
import kotlinx.coroutines.flow.Flow
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.legacy.logging.Log
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.robolectric.RuntimeEnvironment

class QuickReplySenderTest : RobolectricTest() {
    private val account = LegacyAccountDto(ACCOUNT_UUID).apply {
        identities = mutableListOf(
            Identity(name = "Other", email = "other@example.com"),
            Identity(name = "Me", email = "me@example.com", signature = "-- \nMe", signatureUse = true),
        )
        quoteStyle = QuoteStyle.PREFIX
        quotePrefix = "> "
        isDefaultQuotedTextShown = true
        alwaysBcc = "archive@example.com"
    }
    private val accountManager = mock<LegacyAccountDtoManager> {
        on { getAccount(ACCOUNT_UUID) } doReturn account
    }
    private val messagingController = mock<MessagingController>()
    private val settingsManager = FakeGeneralSettingsManager()
    private val messageIdGenerator = mock<MessageIdGenerator> {
        on { generateMessageId(any()) } doReturn "<reply@example.com>"
    }
    private val quoteDateFormatter = mock<QuoteDateFormatter> {
        on { format(any()) } doReturn "Monday"
    }
    private lateinit var original: Message

    private val testSubject = QuickReplySender(
        accountManager = accountManager,
        messagingController = messagingController,
        textQuoteCreator = TextQuoteCreator(quoteDateFormatter, TestCoreResourceProvider()),
        generalSettingsManager = settingsManager,
        logger = TestLogger(),
        createMessageBuilder = {
            SimpleMessageBuilder(
                messageIdGenerator,
                BoundaryGenerator.getInstance(),
                TestCoreResourceProvider(),
                settingsManager,
            ) { html -> html }
        },
        loadMessage = { _, _ -> original },
        now = { Date(SENT_DATE) },
    )

    @Before
    fun setUp() {
        Log.logger = TestLogger()
        BinaryTempFileBody.setTempDirectory(RuntimeEnvironment.getApplication().cacheDir)
        original = parseMessage(PLAIN_MESSAGE)
    }

    @Test
    fun `reply is sent to the sender from the identity the message was sent to`() {
        val result = testSubject.sendReply(MESSAGE_REFERENCE, "On my way")

        assertThat(result).isEqualTo(QuickReplyResult.SENT)
        val reply = sentMessage()
        assertThat(reply.from.toList()).containsExactly(Address("me@example.com", "Me"))
        assertThat(reply.getRecipients(RecipientType.TO).toList())
            .containsExactly(Address("alice@example.org", "Alice"))
        assertThat(reply.getRecipients(RecipientType.BCC).toList()).containsExactly(Address("archive@example.com"))
        assertThat(reply.subject).isEqualTo("Re: Lunch?")
    }

    @Test
    fun `reply continues the thread`() {
        testSubject.sendReply(MESSAGE_REFERENCE, "On my way")

        val reply = sentMessage()
        assertThat(reply.getHeader("In-Reply-To").toList()).containsExactly("<original@example.org>")
        assertThat(reply.getHeader("References").toList())
            .containsExactly("<first@example.org> <original@example.org>")
    }

    @Test
    fun `reply contains the text, the signature, and the quoted message`() {
        testSubject.sendReply(MESSAGE_REFERENCE, "On my way\nSee you")

        val body = MessageExtractor.getTextFromPart(sentMessage())
        assertThat(body.orEmpty()).contains("On my way\r\nSee you")
        assertThat(body.orEmpty()).contains("-- \r\nMe")
        assertThat(body.orEmpty()).contains("> Are you free at noon?")
    }

    @Test
    fun `original message is marked as answered`() {
        testSubject.sendReply(MESSAGE_REFERENCE, "On my way")

        verify(messagingController).setFlag(account, FOLDER_ID, UID, Flag.ANSWERED, true)
    }

    @Test
    fun `encrypted message is refused`() {
        original = parseMessage(ENCRYPTED_MESSAGE)

        val result = testSubject.sendReply(MESSAGE_REFERENCE, "On my way")

        assertThat(result).isEqualTo(QuickReplyResult.NOT_AVAILABLE)
        verify(messagingController, never()).sendMessage(any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `message of a removed account isn't found`() {
        val removedAccountMessage = MessageReference("00000000-0000-4000-8000-000000000000", FOLDER_ID, UID)

        val result = testSubject.sendReply(removedAccountMessage, "On my way")

        assertThat(result).isEqualTo(QuickReplyResult.MESSAGE_NOT_FOUND)
    }

    @Test
    fun `deleted message isn't found`() {
        val testSubject = QuickReplySender(
            accountManager = accountManager,
            messagingController = messagingController,
            textQuoteCreator = TextQuoteCreator(quoteDateFormatter, TestCoreResourceProvider()),
            generalSettingsManager = settingsManager,
            logger = TestLogger(),
            loadMessage = { _, _ -> throw IllegalArgumentException("Message not found") },
        )

        val result = testSubject.sendReply(MESSAGE_REFERENCE, "On my way")

        assertThat(result).isEqualTo(QuickReplyResult.MESSAGE_NOT_FOUND)
    }

    @Test
    fun `reply subject gets one Re prefix`() {
        assertThat(QuickReplySender.replySubject("Lunch?")).isEqualTo("Re: Lunch?")
        assertThat(QuickReplySender.replySubject("Re: Lunch?")).isEqualTo("Re: Lunch?")
        assertThat(QuickReplySender.replySubject("RE: Lunch?")).isEqualTo("RE: Lunch?")
        assertThat(QuickReplySender.replySubject("AW: Mittagessen?")).isEqualTo("Re: Mittagessen?")
        assertThat(QuickReplySender.replySubject(null)).isEqualTo("")
    }

    private fun sentMessage(): Message {
        val captor = argumentCaptor<Message>()
        verify(messagingController).sendMessage(eq(account), captor.capture(), anyOrNull(), anyOrNull())
        return captor.firstValue
    }

    private fun parseMessage(source: String): Message {
        return MimeMessage.parseMimeMessage(source.replace("\n", "\r\n").byteInputStream(), false)
    }

    private class FakeGeneralSettingsManager : GeneralSettingsManager {
        private val settings = GeneralSettings(platformConfigProvider = mock())

        @Deprecated("Use getConfig() instead")
        override fun getSettings(): GeneralSettings = settings

        @Deprecated("Use getConfigFlow() instead")
        override fun getSettingsFlow(): Flow<GeneralSettings> = throw UnsupportedOperationException()

        override fun getConfig(): GeneralSettings = settings

        override fun getConfigFlow(): Flow<GeneralSettings> = throw UnsupportedOperationException()

        override fun save(config: GeneralSettings) = throw UnsupportedOperationException()
    }

    private companion object {
        const val ACCOUNT_UUID = "5b8f2c1e-3d4a-4b6c-9e7f-0a1b2c3d4e5f"
        const val FOLDER_ID = 7L
        const val UID = "uid-1"
        const val SENT_DATE = 1_700_000_000_000L
        val MESSAGE_REFERENCE = MessageReference(ACCOUNT_UUID, FOLDER_ID, UID)

        val PLAIN_MESSAGE = """
            From: Alice <alice@example.org>
            To: Me <me@example.com>
            Subject: Lunch?
            Date: Mon, 13 Nov 2023 12:00:00 +0000
            Message-ID: <original@example.org>
            References: <first@example.org>
            MIME-Version: 1.0
            Content-Type: text/plain; charset=utf-8

            Are you free at noon?
        """.trimIndent()

        val ENCRYPTED_MESSAGE = """
            From: Alice <alice@example.org>
            To: Me <me@example.com>
            Subject: Secret
            Message-ID: <secret@example.org>
            MIME-Version: 1.0
            Content-Type: multipart/encrypted; protocol="application/pgp-encrypted"; boundary="b"

            --b
            Content-Type: application/pgp-encrypted

            Version: 1
            --b
            Content-Type: application/octet-stream

            -----BEGIN PGP MESSAGE-----
            hQEMA
            -----END PGP MESSAGE-----
            --b--
        """.trimIndent()
    }
}
