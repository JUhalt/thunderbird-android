package net.thunderbird.feature.wear.companion

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class WearProtocolCodecTest {

    @Test
    fun `snapshot survives a round trip`() {
        val snapshot = WearInboxSnapshot(
            generatedAt = 1_700_000_000_000,
            unreadCount = 42,
            messages = listOf(MESSAGE),
        )

        val result = WearProtocolCodec.decodeSnapshot(WearProtocolCodec.encodeSnapshot(snapshot))

        assertThat(result).isEqualTo(snapshot)
    }

    @Test
    fun `snapshot with unknown fields is still decoded`() {
        val json = """
            {"version":1,"generatedAt":5,"unreadCount":0,"messages":[],"addedInAFutureVersion":true}
        """.trimIndent()

        val result = WearProtocolCodec.decodeSnapshot(json.encodeToByteArray())

        assertThat(result).isEqualTo(WearInboxSnapshot(generatedAt = 5, unreadCount = 0, messages = emptyList()))
    }

    @Test
    fun `snapshot from another protocol version is rejected`() {
        val json = """{"version":2,"generatedAt":5,"unreadCount":0,"messages":[]}"""

        val result = WearProtocolCodec.decodeSnapshot(json.encodeToByteArray())

        assertThat(result).isNull()
    }

    @Test
    fun `invalid snapshot bytes decode to null`() {
        val result = WearProtocolCodec.decodeSnapshot("not json".encodeToByteArray())

        assertThat(result).isNull()
    }

    @Test
    fun `requests survive a round trip`() {
        val requests = listOf(
            WearRequest.Refresh,
            WearRequest.PerformAction(messageId = "#:abc", action = WearMessageAction.ARCHIVE),
        )

        for (request in requests) {
            val result = WearProtocolCodec.decodeRequest(WearProtocolCodec.encodeRequest(request))

            assertThat(result).isEqualTo(request)
        }
    }

    @Test
    fun `request with an unknown action decodes to null`() {
        val json = """{"type":"action","messageId":"x","action":"SNOOZE"}"""

        val result = WearProtocolCodec.decodeRequest(json.encodeToByteArray())

        assertThat(result).isNull()
    }

    @Test
    fun `responses survive a round trip`() {
        val responses = listOf(WearResponse.Ok, WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND))

        for (response in responses) {
            val result = WearProtocolCodec.decodeResponse(WearProtocolCodec.encodeResponse(response))

            assertThat(result).isEqualTo(response)
        }
    }

    private companion object {
        val MESSAGE = WearMessageSummary(
            id = "#:YWNjb3VudA==:MQ==:dWlk",
            senderName = "Ada Lovelace",
            senderAddress = "ada@example.com",
            subject = "Hello from the phone",
            preview = "Préview with ünïcode 🐦",
            date = 1_699_999_999_000,
            isRead = false,
            isStarred = true,
            hasAttachments = true,
            isEncrypted = false,
            accountColor = 0xFF0A84FF.toInt(),
        )
    }
}
