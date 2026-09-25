package net.thunderbird.wear.ui.reply

import app.cash.turbine.test
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
import net.thunderbird.wear.ui.reply.ReplyContract.Effect
import net.thunderbird.wear.ui.reply.ReplyContract.Event

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
        val testSubject = createTestSubject()
        advanceUntilIdle()

        assertThat(testSubject.state.value.message?.senderName).isEqualTo("Ada")
        assertThat(testSubject.state.value.draft).isNull()
    }

    @Test
    fun `chosen reply is reviewed before it's sent`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        testSubject.event(Event.QuickReplyClicked("  Sounds good.  "))
        advanceUntilIdle()

        assertThat(testSubject.state.value.draft).isEqualTo("Sounds good.")
        assertThat(phone.replies).isEmpty()

        testSubject.event(Event.SendClicked)
        advanceUntilIdle()

        assertThat(phone.replies).containsExactly("m1" to "Sounds good.")
        assertThat(testSubject.state.value.isSent).isTrue()
    }

    @Test
    fun `blank replies are ignored`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        testSubject.event(Event.QuickReplyClicked("   "))

        assertThat(testSubject.state.value.draft).isNull()
    }

    @Test
    fun `overly long replies are shortened to what the phone accepts`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        testSubject.event(Event.QuickReplyClicked("x".repeat(WearCompanion.MAX_REPLY_LENGTH + 10)))

        assertThat(testSubject.state.value.draft.orEmpty()).hasLength(WearCompanion.MAX_REPLY_LENGTH)
    }

    @Test
    fun `changing the reply goes back to choosing one`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()
        testSubject.event(Event.QuickReplyClicked("OK"))

        testSubject.event(Event.ChangeClicked)

        assertThat(testSubject.state.value.draft).isNull()
    }

    @Test
    fun `reply the phone can't send is explained and can be retried`() = runTest {
        phone.replyResult = PhoneResult.Failed(WearErrorReason.ACTION_NOT_AVAILABLE)
        val testSubject = createTestSubject()
        advanceUntilIdle()
        testSubject.event(Event.QuickReplyClicked("OK"))

        testSubject.event(Event.SendClicked)
        advanceUntilIdle()

        assertThat(testSubject.state.value.isSent).isFalse()
        assertThat(testSubject.state.value.isSending).isFalse()
        assertThat(testSubject.state.value.errorMessage).isEqualTo(R.string.error_reply_unavailable)
        assertThat(testSubject.state.value.draft).isEqualTo("OK")
    }

    @Test
    fun `cancelled voice input is ignored`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        testSubject.event(Event.ReplyInputReceived(text = null))

        assertThat(testSubject.state.value.draft).isNull()
    }

    @Test
    fun `speak or type opens the system input and its text is reviewed`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        testSubject.effect.test {
            testSubject.event(Event.SpeakOrTypeClicked)

            assertThat(awaitItem()).isEqualTo(Effect.OpenReplyInput)
        }
        testSubject.event(Event.ReplyInputReceived(text = "See you there"))

        assertThat(testSubject.state.value.draft).isEqualTo("See you there")
    }

    @Test
    fun `screen closes after the sent confirmation`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        testSubject.effect.test {
            testSubject.event(Event.SentConfirmationDismissed)

            assertThat(awaitItem()).isEqualTo(Effect.Close)
        }
    }

    private fun createTestSubject() = ReplyViewModel(messageId = "m1", mailboxId = UNIFIED, phoneConnection = phone)
}
