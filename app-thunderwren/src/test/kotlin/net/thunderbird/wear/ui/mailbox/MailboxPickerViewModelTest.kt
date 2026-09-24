package net.thunderbird.wear.ui.mailbox

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeSelectedMailboxStore
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.mailbox

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
        val viewModel = MailboxPickerViewModel(phoneConnection = phone, selectedMailboxStore = selectedMailbox)
        backgroundScope.launch { viewModel.uiState.collect {} }
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.mailboxes.map { it.id }).containsExactly(UNIFIED, "work", "home")
        assertThat(viewModel.uiState.value.selectedMailboxId).isEqualTo(UNIFIED)

        viewModel.select("home")
        advanceUntilIdle()

        assertThat(selectedMailbox.selectedMailboxId.value).isEqualTo("home")
        assertThat(viewModel.uiState.value.selectedMailboxId).isEqualTo("home")
    }
}
