package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearProtocolCodec
import net.thunderbird.feature.wear.companion.WearRequest
import net.thunderbird.feature.wear.companion.WearResponse

class WearRequestHandlerTest {

    private val publisher = FakeWearInboxPublisher()
    private val actions = FakeWearMessageActions()

    @Test
    fun `undecodable request is rejected`() = runTest {
        val response = handle("garbage".encodeToByteArray())

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.UNSUPPORTED_REQUEST))
    }

    @Test
    fun `refresh publishes the inbox`() = runTest {
        val response = handle(WearRequest.Refresh)

        assertThat(response).isEqualTo(WearResponse.Ok)
        assertThat(publisher.publishNowCount).isEqualTo(1)
    }

    @Test
    fun `refresh reports a failed publish`() = runTest {
        publisher.publishResult = false

        val response = handle(WearRequest.Refresh)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.FAILED))
    }

    @Test
    fun `action is applied to the referenced message and the inbox is republished`() = runTest {
        val reference = MessageReference("account", 3, "uid")

        val response = handle(WearRequest.PerformAction(reference.toIdentityString(), WearMessageAction.ARCHIVE))

        assertThat(response).isEqualTo(WearResponse.Ok)
        assertThat(actions.performed).containsExactly(reference to WearMessageAction.ARCHIVE)
        assertThat(publisher.requestPublishCount).isEqualTo(1)
    }

    @Test
    fun `action with an invalid message ID is rejected`() = runTest {
        val response = handle(WearRequest.PerformAction("not a reference", WearMessageAction.DELETE))

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.MESSAGE_NOT_FOUND))
        assertThat(actions.performed).isEmpty()
    }

    @Test
    fun `failed action is reported and nothing is republished`() = runTest {
        actions.response = WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE)
        val reference = MessageReference("account", 3, "uid")

        val response = handle(WearRequest.PerformAction(reference.toIdentityString(), WearMessageAction.ARCHIVE))

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE))
        assertThat(publisher.requestPublishCount).isEqualTo(0)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.handle(request: WearRequest): WearResponse? {
        return handle(WearProtocolCodec.encodeRequest(request))
    }

    private suspend fun kotlinx.coroutines.test.TestScope.handle(requestData: ByteArray): WearResponse? {
        val handler = WearRequestHandler(
            publisher = publisher,
            messageActions = actions,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        return WearProtocolCodec.decodeResponse(handler.handle(requestData))
    }
}
