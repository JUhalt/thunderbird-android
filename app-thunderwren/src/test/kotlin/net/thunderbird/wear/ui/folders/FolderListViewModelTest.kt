package net.thunderbird.wear.ui.folders

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.wear.R
import net.thunderbird.wear.data.PhoneResult
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.ui.folders.FolderListContract.Effect
import net.thunderbird.wear.ui.folders.FolderListContract.Event

@OptIn(ExperimentalCoroutinesApi::class)
class FolderListViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val phone = FakePhoneConnection().apply {
        publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("work", name = "Work")), inboxes = emptyMap())
        phoneFolders["work"] = listOf(
            WearFolder(id = 1, name = "Inbox", type = WearFolderType.INBOX, unreadCount = 2),
            WearFolder(id = 5, name = "Receipts", type = WearFolderType.REGULAR, unreadCount = 0),
        )
    }

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `loads the account's folders when opened`() = runTest {
        val testSubject = FolderListViewModel(accountId = "work", phoneConnection = phone)
        advanceUntilIdle()

        val state = testSubject.state.value
        assertThat(state.account?.name).isEqualTo("Work")
        assertThat(state.folders?.map { it.name }).isEqualTo(listOf("Inbox", "Receipts"))
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isNull()
    }

    @Test
    fun `failure is shown and loading can be retried`() = runTest {
        phone.loadFoldersResult = PhoneResult.NoPhone
        val testSubject = FolderListViewModel(accountId = "work", phoneConnection = phone)
        advanceUntilIdle()

        assertThat(testSubject.state.value.folders).isNull()
        assertThat(testSubject.state.value.errorMessage).isEqualTo(R.string.error_no_phone)

        phone.loadFoldersResult = PhoneResult.Success
        testSubject.event(Event.RetryClicked)
        advanceUntilIdle()

        assertThat(testSubject.state.value.folders?.size).isEqualTo(2)
        assertThat(testSubject.state.value.errorMessage).isNull()
    }

    @Test
    fun `tapping a folder opens it`() = runTest {
        val testSubject = FolderListViewModel(accountId = "work", phoneConnection = phone)

        testSubject.effect.test {
            testSubject.event(Event.FolderClicked(folderId = 5))

            assertThat(awaitItem()).isEqualTo(Effect.OpenFolder(accountId = "work", folderId = 5))
        }
    }
}
