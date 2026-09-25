package com.fsck.k9.notification

import android.app.Application
import android.app.Notification
import android.app.PendingIntent
import android.graphics.Bitmap
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import app.k9mail.legacy.message.controller.MessageReference
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.mail.Address
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.MockHelper.mockBuilder
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.featureflag.FeatureFlagResult
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.core.preference.notification.NotificationPreference
import net.thunderbird.core.preference.notification.NotificationPreferenceManager
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SingleMessageNotificationCreatorTest : RobolectricTest() {
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())
    private val notificationPreferenceManager = FakeNotificationPreferenceManager()
    private val resourceProvider = TestAvatarNotificationResourceProvider()
    private val notification = mock<Notification>()
    private val builder = mockBuilder<NotificationCompat.Builder> {
        on { build() } doReturn notification
    }

    private val replyPendingIntent = mock<PendingIntent>()
    private val quickReplyPendingIntent = mock<PendingIntent>()
    private var quickReplyFlag: FeatureFlagResult = FeatureFlagResult.Enabled

    private lateinit var testSubject: SingleMessageNotificationCreator

    @Before
    fun setUp() {
        mainDispatcher.setUp()
        testSubject = SingleMessageNotificationCreator(
            notificationHelper = createNotificationHelper(),
            actionCreator = createNotificationActionCreator(),
            resourceProvider = resourceProvider,
            lockScreenNotificationCreator = mock(),
            notificationPreferenceManager = notificationPreferenceManager,
            featureFlagProvider = { key ->
                when (key) {
                    GeneratedFeatureFlagKey.WEAR_NOTIFICATION_QUICK_REPLY -> quickReplyFlag
                    else -> FeatureFlagResult.Disabled
                }
            },
            application = ApplicationProvider.getApplicationContext<Application>(),
        )
    }

    @After
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `create notification looks up avatar when notification contact pictures are enabled`() = runTest {
        notificationPreferenceManager.setShowContactPictureInNotification(true)

        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(),
        ).join()

        assertThat(resourceProvider.avatarCalls).isEqualTo(1)
    }

    @Test
    fun `create notification skips avatar lookup when notification contact pictures are disabled`() = runTest {
        notificationPreferenceManager.setShowContactPictureInNotification(false)

        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(),
        ).join()

        assertThat(resourceProvider.avatarCalls).isEqualTo(0)
    }

    @Test
    fun `wear reply action offers a quick reply with ready-made replies`() = runTest {
        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(wearActions = listOf(WearNotificationAction.Reply)),
        ).join()

        val replyAction = wearActions().single()
        assertThat(replyAction.actionIntent).isSameInstanceAs(quickReplyPendingIntent)
        val remoteInput = replyAction.remoteInputs.orEmpty().single()
        assertThat(remoteInput.resultKey).isEqualTo(NotificationActionIntents.EXTRA_QUICK_REPLY_TEXT)
        assertThat(remoteInput.choices.orEmpty().map { it.toString() }).containsExactly("OK", "Thanks!")
    }

    @Test
    fun `wear reply action for an encrypted message opens the compose screen instead`() = runTest {
        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(
                wearActions = listOf(WearNotificationAction.Reply),
                isEncrypted = true,
            ),
        ).join()

        val replyAction = wearActions().single()
        assertThat(replyAction.actionIntent).isSameInstanceAs(replyPendingIntent)
        assertThat(replyAction.remoteInputs.orEmpty().toList()).isEmpty()
    }

    @Test
    fun `wear reply action opens the compose screen while quick reply is turned off`() = runTest {
        quickReplyFlag = FeatureFlagResult.Disabled

        testSubject.createSingleNotification(
            baseNotificationData = createBaseNotificationData(),
            singleNotificationData = createSingleNotificationData(wearActions = listOf(WearNotificationAction.Reply)),
        ).join()

        val replyAction = wearActions().single()
        assertThat(replyAction.actionIntent).isSameInstanceAs(replyPendingIntent)
        assertThat(replyAction.remoteInputs.orEmpty().toList()).isEmpty()
    }

    private fun wearActions(): List<NotificationCompat.Action> {
        val captor = argumentCaptor<NotificationCompat.Extender>()
        verify(builder).extend(captor.capture())
        return captor.allValues.filterIsInstance<NotificationCompat.WearableExtender>().single().actions
    }

    private fun createNotificationHelper(): NotificationHelper {
        return mock {
            on { createNotificationBuilder(any(), any()) } doReturn builder
        }
    }

    private fun createNotificationActionCreator(): NotificationActionCreator {
        val pendingIntent = mock<PendingIntent>()
        return mock {
            on { createViewMessagePendingIntent(any()) } doReturn pendingIntent
            on { createDismissMessagePendingIntent(any()) } doReturn pendingIntent
            on { createReplyPendingIntent(any()) } doReturn replyPendingIntent
            on { createQuickReplyPendingIntent(any()) } doReturn quickReplyPendingIntent
        }
    }

    private fun createBaseNotificationData(): BaseNotificationData {
        return BaseNotificationData(
            account = LegacyAccountDto("00000000-0000-0000-0000-000000000000"),
            accountName = "Account name",
            groupKey = "group",
            color = 0,
            newMessagesCount = 1,
            lockScreenNotificationData = LockScreenNotificationData.None,
            appearance = NotificationAppearance(
                ringtone = null,
                vibrationPattern = null,
                ledColor = null,
            ),
        )
    }

    private fun createSingleNotificationData(
        wearActions: List<WearNotificationAction> = emptyList(),
        isEncrypted: Boolean = false,
    ): SingleNotificationData {
        return SingleNotificationData(
            notificationId = 23,
            isSilent = true,
            timestamp = 9000,
            content = NotificationContent(
                messageReference = MessageReference("account", 1, "uid"),
                sender = Address("alice@example.com", "Alice"),
                subject = "Subject",
                preview = "Preview",
                summary = "Summary",
                isEncrypted = isEncrypted,
            ),
            actions = emptyList(),
            wearActions = wearActions,
            addLockScreenNotification = false,
        )
    }

    private class TestAvatarNotificationResourceProvider :
        NotificationResourceProvider by TestNotificationResourceProvider() {
        var avatarCalls = 0

        override suspend fun avatar(address: Address): Bitmap? {
            avatarCalls += 1
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }
    }

    private class FakeNotificationPreferenceManager : NotificationPreferenceManager {
        private val prefs = MutableStateFlow(NotificationPreference())

        override fun save(config: NotificationPreference) = Unit

        override fun getConfig(): NotificationPreference = prefs.value

        override fun getConfigFlow(): Flow<NotificationPreference> = prefs

        fun setShowContactPictureInNotification(isEnabled: Boolean) {
            prefs.update { it.copy(isShowContactPictureInNotification = isEnabled) }
        }
    }
}
