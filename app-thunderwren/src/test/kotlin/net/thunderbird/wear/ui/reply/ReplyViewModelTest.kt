package net.thunderbird.wear.ui.reply

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasLength
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message

@OptIn(ExperimentalCoroutinesApi::class)
class ReplyViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection()

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", senderName = "Ada"))),
        )
    }

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `shows the message being replied to`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.message?.senderName).isEqualTo("Ada")
        assertThat(viewModel.uiState.value.draft).isNull()
    }

    @Test
    fun `chosen reply is reviewed before it's sent`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setDraft("  Sounds good.  ")
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.draft).isEqualTo("Sounds good.")
        assertThat(phone.replies).isEmpty()

        viewModel.send()
        advanceUntilIdle()

        assertThat(phone.replies).containsExactly("m1" to "Sounds good.")
        assertThat(viewModel.uiState.value.isSent).isTrue()
    }

    @Test
    fun `cancelled voice input is ignored`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setDraft("   ")

        assertThat(viewModel.uiState.value.draft).isNull()
    }

    @Test
    fun `overly long replies are shortened to what the phone accepts`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setDraft("x".repeat(WearCompanion.MAX_REPLY_LENGTH + 10))

        assertThat(viewModel.uiState.value.draft.orEmpty()).hasLength(WearCompanion.MAX_REPLY_LENGTH)
    }

    @Test
    fun `changing the reply goes back to choosing one`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.setDraft("OK")

        viewModel.clearDraft()

        assertThat(viewModel.uiState.value.draft).isNull()
    }

    @Test
    fun `reply the phone can't send is explained and can be retried`() = runTest {
        phone.replyResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.setDraft("OK")

        viewModel.send()
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.isSent).isFalse()
        assertThat(viewModel.uiState.value.isSending).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo(R.string.error_reply_unavailable)
        assertThat(viewModel.uiState.value.draft).isEqualTo("OK")
    }

    private fun createViewModel() = ReplyViewModel(messageId = "m1", mailboxId = UNIFIED, phoneConnection = phone)
}
