package net.thunderbird.wear.ui.mailbox

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeSelectedMailboxStore
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.ui.mailbox.MailboxPickerContract.Effect
import net.thunderbird.wear.ui.mailbox.MailboxPickerContract.Event

@OptIn(ExperimentalCoroutinesApi::class)
class MailboxPickerViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection()
    private val selectedMailbox = FakeSelectedMailboxStore()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `lists the unified inbox and every account, and remembers the selection`() = runTest {
        phone.publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("work"), mailbox("home")), inboxes = emptyMap())
        val testSubject = MailboxPickerViewModel(phoneConnection = phone, selectedMailboxStore = selectedMailbox)
        advanceUntilIdle()

        assertThat(testSubject.state.value.mailboxes.map { it.id }).containsExactly(UNIFIED, "work", "home")
        assertThat(testSubject.state.value.selectedMailboxId).isEqualTo(UNIFIED)

        testSubject.effect.test {
            testSubject.event(Event.MailboxClicked("home"))
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(Effect.Close)
        }

        assertThat(selectedMailbox.selectedMailboxId.value).isEqualTo("home")
        assertThat(testSubject.state.value.selectedMailboxId).isEqualTo("home")
    }
}
