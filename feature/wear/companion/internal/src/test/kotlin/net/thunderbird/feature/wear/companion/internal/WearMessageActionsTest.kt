package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.controller.MessagingController
import kotlin.test.Test
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearResponse
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class WearMessageActionsTest {
    private val account = LegacyAccountDto(ACCOUNT_UUID).apply { archiveFolderId = ARCHIVE_FOLDER_ID }
    private val accountManager = mock<LegacyAccountDtoManager> {
        on { getAccount(ACCOUNT_UUID) } doReturn account
    }
    private val messagingController = mock<MessagingController>()
    private val testSubject = MessagingControllerWearMessageActions(messagingController, accountManager)

    @Test
    fun `archiving moves the message to the Archive folder`() {
        val reference = MessageReference(ACCOUNT_UUID, folderId = 1, uid = "uid")

        val response = testSubject.perform(reference, WearMessageAction.ARCHIVE)

        assertThat(response).isEqualTo(WearResponse.Ok)
        verify(messagingController).archiveMessage(reference)
    }

    @Test
    fun `a message in the Archive folder can't be archived again`() {
        val reference = MessageReference(ACCOUNT_UUID, folderId = ARCHIVE_FOLDER_ID, uid = "uid")

        val response = testSubject.perform(reference, WearMessageAction.ARCHIVE)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE))
        verify(messagingController, never()).archiveMessage(any())
    }

    @Test
    fun `archiving needs an Archive folder`() {
        account.archiveFolderId = null
        val reference = MessageReference(ACCOUNT_UUID, folderId = 1, uid = "uid")

        val response = testSubject.perform(reference, WearMessageAction.ARCHIVE)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.ACTION_NOT_AVAILABLE))
        verify(messagingController, never()).archiveMessage(any())
    }

    private companion object {
        const val ACCOUNT_UUID = "5b8f2c1e-3d4a-4b6c-9e7f-0a1b2c3d4e5f"
        const val ARCHIVE_FOLDER_ID = 9L
    }
}
