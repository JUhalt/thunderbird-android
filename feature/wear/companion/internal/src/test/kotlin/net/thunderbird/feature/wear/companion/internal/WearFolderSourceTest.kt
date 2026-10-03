package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.message.controller.MessageCounts
import app.k9mail.legacy.message.controller.MessageCountsProvider
import app.k9mail.legacy.ui.folder.DisplayFolder
import app.k9mail.legacy.ui.folder.DisplayFolderRepository
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import com.fsck.k9.helper.MessageHelper
import com.fsck.k9.mail.Address
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.android.account.LegacyAccountManager
import net.thunderbird.feature.account.storage.profile.ProfileDto
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.search.legacy.LocalMessageSearch
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearErrorReason
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearResponse
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalTime::class)
class WearFolderSourceTest {
    private val accountDto = LegacyAccountDto(ACCOUNT_UUID)
    private val profile = mock<ProfileDto> { on { color } doReturn ACCOUNT_COLOR }
    private val account = mock<LegacyAccount> {
        on { uuid } doReturn ACCOUNT_UUID
        on { profile } doReturn profile
        on { sentFolderId } doReturn SENT_FOLDER_ID
    }
    private val accountManager = mock<LegacyAccountManager> {
        on { getAccounts() } doReturn listOf(account)
    }
    private val accountDtoManager = mock<LegacyAccountDtoManager> {
        on { getAccount(ACCOUNT_UUID) } doReturn accountDto
    }
    private val displayFolderRepository = FakeDisplayFolderRepository(
        listOf(
            displayFolder(id = 1, name = "INBOX", type = FolderType.INBOX, unreadCount = 4),
            displayFolder(id = 2, name = "Outbox", type = FolderType.OUTBOX),
            displayFolder(id = 3, name = "Drafts", type = FolderType.DRAFTS),
            displayFolder(id = SENT_FOLDER_ID, name = "Sent", type = FolderType.SENT),
            displayFolder(id = 5, name = "Work/Projects", type = FolderType.REGULAR, unreadCount = 1),
        ),
    )
    private val messageListRepository = FakeMessageListRepository(
        messages = mapOf(
            ACCOUNT_UUID to listOf(
                FakeMessageDetailsAccessor(
                    folderId = SENT_FOLDER_ID,
                    toAddresses = listOf(Address("bo@example.com")),
                ),
            ),
        ),
    )
    private val messageHelper = mock<MessageHelper> {
        on { getSenderDisplayName(any()) } doAnswer { invocation -> (invocation.arguments[0] as Address).address }
    }
    private val messageCountsProvider = mock<MessageCountsProvider> {
        on { getMessageCounts(any<LocalMessageSearch>()) } doReturn
            MessageCounts(unread = 2, starred = 0)
    }

    private val testSubject = DisplayFolderWearFolderSource(
        accountManager = accountManager,
        accountDtoManager = accountDtoManager,
        displayFolderRepository = displayFolderRepository,
        folderName = { folder -> if (folder.type == FolderType.INBOX) "Inbox" else folder.name },
        snapshotLoader = WearSnapshotLoader(
            messageListRepository = messageListRepository,
            messageCountsProvider = messageCountsProvider,
            messageHelper = messageHelper,
        ),
        clock = object : Clock {
            override fun now() = Instant.fromEpochMilliseconds(NOW)
        },
    )

    @Test
    fun `folders are listed as on the phone, without Drafts and the Outbox`() = runTest {
        val response = testSubject.loadFolders(ACCOUNT_UUID)

        assertThat(response).isEqualTo(
            WearResponse.Folders(
                listOf(
                    WearFolder(id = 1, name = "Inbox", type = WearFolderType.INBOX, unreadCount = 4),
                    WearFolder(id = SENT_FOLDER_ID, name = "Sent", type = WearFolderType.SENT, unreadCount = 0),
                    WearFolder(id = 5, name = "Work/Projects", type = WearFolderType.REGULAR, unreadCount = 1),
                ),
            ),
        )
        assertThat(displayFolderRepository.includeHiddenFolders).containsExactly(false)
    }

    @Test
    fun `folders of a removed account aren't found`() = runTest {
        val response = testSubject.loadFolders("00000000-0000-4000-8000-000000000000")

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.MAILBOX_NOT_FOUND))
    }

    @Test
    fun `folder is loaded with its newest messages`() = runTest {
        val response = testSubject.loadFolder(ACCOUNT_UUID, SENT_FOLDER_ID)

        assertThat(response).isInstanceOf<WearResponse.Folder>().all {
            prop(WearResponse.Folder::folder).isEqualTo(
                WearFolder(id = SENT_FOLDER_ID, name = "Sent", type = WearFolderType.SENT, unreadCount = 0),
            )
            prop(WearResponse.Folder::snapshot).all {
                prop(
                    WearInboxSnapshot::mailboxId,
                ).isEqualTo(WearCompanion.folderMailboxId(ACCOUNT_UUID, SENT_FOLDER_ID))
                prop(WearInboxSnapshot::generatedAt).isEqualTo(NOW)
                prop(WearInboxSnapshot::unreadCount).isEqualTo(2)
                transform { snapshot -> snapshot.messages.map { it.senderName to it.isOutgoing } }
                    .containsExactly("bo@example.com" to true)
            }
        }
        // Only this folder's messages.
        assertThat(messageListRepository.queries.single().second).containsExactly(SENT_FOLDER_ID.toString())
    }

    @Test
    fun `folders the watch doesn't show can't be loaded`() = runTest {
        val response = testSubject.loadFolder(ACCOUNT_UUID, folderId = 3)

        assertThat(response).isEqualTo(WearResponse.Error(WearErrorReason.MAILBOX_NOT_FOUND))
        assertThat(messageListRepository.queries.size).isEqualTo(0)
    }

    private class FakeDisplayFolderRepository(private val folders: List<DisplayFolder>) : DisplayFolderRepository {
        val includeHiddenFolders = mutableListOf<Boolean>()

        override fun getDisplayFoldersFlow(
            account: LegacyAccountDto,
            includeHiddenFolders: Boolean,
        ): Flow<List<DisplayFolder>> {
            this.includeHiddenFolders += includeHiddenFolders
            return flowOf(folders)
        }

        override fun getDisplayFoldersFlow(accountUuid: String): Flow<List<DisplayFolder>> = error("not used")
    }

    private companion object {
        const val ACCOUNT_UUID = "5b8f2c1e-3d4a-4b6c-9e7f-0a1b2c3d4e5f"
        const val ACCOUNT_COLOR = 0x112233
        const val SENT_FOLDER_ID = 4L
        const val NOW = 1_700_000_000_000

        fun displayFolder(id: Long, name: String, type: FolderType, unreadCount: Int = 0) = DisplayFolder(
            folder = Folder(id = id, name = name, type = type, isLocalOnly = false),
            isInTopGroup = false,
            unreadMessageCount = unreadCount,
            starredMessageCount = 0,
            pathDelimiter = "/",
        )
    }
}
