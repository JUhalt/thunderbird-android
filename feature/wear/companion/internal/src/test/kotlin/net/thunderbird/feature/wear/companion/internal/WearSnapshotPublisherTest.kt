package net.thunderbird.feature.wear.companion.internal

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearProtocolCodec

class WearSnapshotPublisherTest {

    private val dataLayer = FakeWearDataLayer()
    private val repository = FakeMessageListRepository()

    @Test
    fun `start publishes once when a watch is paired`() = runTest {
        val testSubject = createTestSubject(unreadCount = 4)

        testSubject.start()
        advanceTimeBy(3.seconds)

        assertThat(dataLayer.published).hasSize(1)
        val items = dataLayer.published.single()
        assertThat(items.keys).containsExactlyInAnyOrder(
            WearCompanion.MAILBOXES_PATH,
            WearCompanion.inboxPath(WearCompanion.UNIFIED_MAILBOX_ID),
            WearCompanion.inboxPath("account-1"),
        )
        val mailboxes = WearProtocolCodec.decodeMailboxes(items.getValue(WearCompanion.MAILBOXES_PATH))
        assertThat(mailboxes?.unifiedUnreadCount).isEqualTo(4)
        val accountInbox = WearProtocolCodec.decodeSnapshot(items.getValue(WearCompanion.inboxPath("account-1")))
        assertThat(accountInbox?.mailboxId).isEqualTo("account-1")
    }

    @Test
    fun `nothing is published without a paired watch`() = runTest {
        dataLayer.isPaired = false
        val testSubject = createTestSubject()

        testSubject.start()
        repository.notifyChanged()
        advanceTimeBy(5.seconds)

        assertThat(dataLayer.published).isEmpty()
    }

    @Test
    fun `bursts of message list changes are coalesced`() = runTest {
        val testSubject = createTestSubject()
        testSubject.start()
        advanceTimeBy(3.seconds)
        dataLayer.published.clear()

        repeat(5) { repository.notifyChanged() }
        advanceTimeBy(3.seconds)

        assertThat(dataLayer.published).hasSize(1)
    }

    @Test
    fun `starting twice registers only one listener`() = runTest {
        val testSubject = createTestSubject()

        testSubject.start()
        testSubject.start()

        assertThat(repository.listeners).hasSize(1)
    }

    @Test
    fun `publishNow publishes even without a paired watch`() = runTest {
        dataLayer.isPaired = false
        val testSubject = createTestSubject()

        val result = testSubject.publishNow()

        assertThat(result).isEqualTo(true)
        assertThat(dataLayer.published).hasSize(1)
    }

    @Test
    fun `publishNow reports failures instead of throwing`() = runTest {
        val testSubject = createTestSubject(source = { error("database unavailable") })

        val result = testSubject.publishNow()

        assertThat(result).isFalse()
        assertThat(dataLayer.published).isEmpty()
    }

    @Test
    fun `changed setting republishes, but the current value doesn't`() = runTest {
        val setting = MutableStateFlow("MESSAGE_COUNT")
        val testSubject = createTestSubject(settingChanges = setting)
        testSubject.start()
        advanceTimeBy(3.seconds)
        dataLayer.published.clear()

        setting.value = "MESSAGE_COUNT"
        advanceTimeBy(3.seconds)
        assertThat(dataLayer.published).isEmpty()

        setting.value = "EVERYTHING"
        advanceTimeBy(3.seconds)
        assertThat(dataLayer.published).hasSize(1)
    }

    private fun TestScope.advanceTimeBy(duration: kotlin.time.Duration) {
        testScheduler.advanceTimeBy(duration)
        testScheduler.runCurrent()
    }

    private fun TestScope.createTestSubject(
        unreadCount: Int = 0,
        source: WearPublicationSource = WearPublicationSource { publication(unreadCount) },
        settingChanges: Flow<Any> = emptyFlow(),
    ): WearSnapshotPublisher {
        val dispatcher = StandardTestDispatcher(testScheduler)
        return WearSnapshotPublisher(
            publicationSource = source,
            dataLayer = dataLayer,
            messageListRepository = repository,
            logger = TestLogger(),
            settingChanges = settingChanges,
            coroutineScope = backgroundScope,
            ioDispatcher = dispatcher,
        )
    }
}
