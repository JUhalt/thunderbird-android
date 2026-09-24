package net.thunderbird.feature.wear.companion.internal

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.fsck.k9.controller.MessagingController
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearResponse
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class WearMailboxActionsTest {
    private val work = LegacyAccountDto(WORK_UUID).apply { inboxFolderId = 1 }
    private val personal = LegacyAccountDto(PERSONAL_UUID).apply { inboxFolderId = 2 }
    private val accountManager = mock<LegacyAccountDtoManager> {
        on { getAccounts() } doReturn listOf(work, personal)
        on { getAccount(WORK_UUID) } doReturn work
        on { getAccount(PERSONAL_UUID) } doReturn personal
    }
    private val messagingController = mock<MessagingController>()

    @Test
    fun `mark all read in an account marks its unread inbox messages`() {
        val repository = FakeMessageListRepository(
            messages = mapOf(
                WORK_UUID to listOf(FakeMessageDetailsAccessor(id = 10), FakeMessageDetailsAccessor(id = 11)),
            ),
        )

        val response = createActions(repository).markAllRead(WORK_UUID)

        assertThat(response).isEqualTo(WearResponse.Ok)
        verify(messagingController).setFlag(work, listOf(10L, 11L), Flag.SEEN, true)
        // Only this account's inbox, and only unread messages.
        assertThat(repository.queries.map { it.first }).containsExactly(WORK_UUID)
        assertThat(repository.queries.single().second).containsExactly("1", "0")
    }

    @Test
    fun `mark all read in a view of the unified inbox covers every account`() {
        val repository = FakeMessageListRepository(
            messages = mapOf(
                WORK_UUID to listOf(FakeMessageDetailsAccessor(id = 10)),
                PERSONAL_UUID to listOf(FakeMessageDetailsAccessor(id = 20)),
            ),
        )

        val response = createActions(repository).markAllRead(WearCompanion.STARRED_MAILBOX_ID)

        assertThat(response).isEqualTo(WearResponse.Ok)
        verify(messagingController).setFlag(work, listOf(10L), Flag.SEEN, true)
        verify(messagingController).setFlag(personal, listOf(20L), Flag.SEEN, true)
    }

    @Test
    fun `nothing is changed when there are no unread messages`() {
        val response = createActions(FakeMessageListRepository()).markAllRead(WearCompanion.UNIFIED_MAILBOX_ID)

        assertThat(response).isEqualTo(WearResponse.Ok)
        verify(messagingController, never()).setFlag(any(), any<List<Long>>(), any(), any())
    }

    @Test
    fun `removed account is reported`() {
        val response = createActions(FakeMessageListRepository()).markAllRead("00000000-0000-4000-8000-000000000000")

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.MAILBOX_NOT_FOUND))
    }

    private fun createActions(repository: FakeMessageListRepository) = MessagingControllerWearMailboxActions(
        messagingController = messagingController,
        accountManager = accountManager,
        messageListRepository = repository,
    )

    private companion object {
        const val WORK_UUID = "5b8f2c1e-3d4a-4b6c-9e7f-0a1b2c3d4e5f"
        const val PERSONAL_UUID = "8c9d0e1f-2a3b-4c5d-8e6f-7a8b9c0d1e2f"
    }
}
