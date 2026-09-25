package net.thunderbird.wear.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.wear.testing.FakeDemoModeStore
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message

@OptIn(ExperimentalCoroutinesApi::class)
class DemoAwarePhoneConnectionTest {
    private val phone = FakePhoneConnection()
    private val demo = FakePhoneConnection(isDemo = true)
    private val demoMode = FakeDemoModeStore()
    private val testSubject = DemoAwarePhoneConnection(phone = phone, demo = demo, demoModeStore = demoMode)

    @Test
    fun `nothing is shown without a phone or demo mode`() = runTest {
        demo.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = emptyMap())

        assertThat(testSubject.mailboxes.first()).isNull()
    }

    @Test
    fun `demo mailbox is shown in demo mode while no phone has published`() = runTest {
        demo.publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("demo-work")), inboxes = emptyMap())
        demoMode.setEnabled(true)

        assertThat(testSubject.mailboxes.first()?.mailboxes?.map { it.id }).isEqualTo(listOf(UNIFIED, "demo-work"))
    }

    @Test
    fun `real phone data replaces the demo and turns demo mode off`() = runTest {
        demo.publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("demo-work")), inboxes = emptyMap())
        demoMode.setEnabled(true)
        val seen = mutableListOf<List<String>?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            testSubject.mailboxes.collect { list -> seen += list?.mailboxes?.map { it.id } }
        }

        phone.publish(mailboxes = listOf(mailbox(UNIFIED), mailbox("real-account")), inboxes = emptyMap())
        advanceUntilIdle()

        assertThat(seen.last()).isEqualTo(listOf(UNIFIED, "real-account"))
        assertThat(demoMode.isEnabled.value).isFalse()
    }

    @Test
    fun `actions go to the demo while it is shown, and to the phone otherwise`() = runTest {
        demo.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = mapOf(UNIFIED to listOf(message("d1"))))
        demoMode.setEnabled(true)

        testSubject.performAction("d1", WearMessageAction.STAR)

        assertThat(demo.performedActions).containsExactly("d1" to WearMessageAction.STAR)
        assertThat(phone.performedActions).isEmpty()

        demoMode.setEnabled(false)
        testSubject.performAction("p1", WearMessageAction.STAR)

        assertThat(phone.performedActions).containsExactly("p1" to WearMessageAction.STAR)
    }

    @Test
    fun `refresh always asks the phone and doesn't report a failure in demo mode`() = runTest {
        phone.refreshResult = PhoneResult.NoPhone
        demoMode.setEnabled(true)

        val result = testSubject.refresh()

        assertThat(phone.refreshCount).isEqualTo(1)
        assertThat(result).isEqualTo(PhoneResult.Success)
    }

    @Test
    fun `isDemo follows the source`() = runTest {
        assertThat(testSubject.isDemo.first()).isFalse()

        demo.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = emptyMap())
        demoMode.setEnabled(true)

        assertThat(testSubject.isDemo.first()).isTrue()
    }

    @Test
    fun `replies and mark all read go to the demo while it's shown, and to the phone after`() = runTest {
        demo.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = emptyMap())
        demoMode.setEnabled(true)

        testSubject.reply("demo-1", "Thanks!")
        testSubject.markAllRead(UNIFIED)

        assertThat(demo.replies).containsExactly("demo-1" to "Thanks!")
        assertThat(demo.markedAllRead).containsExactly(UNIFIED)

        phone.publish(mailboxes = listOf(mailbox(UNIFIED)), inboxes = emptyMap())
        testSubject.reply("real-1", "OK")
        testSubject.markAllRead("work")

        assertThat(phone.replies).containsExactly("real-1" to "OK")
        assertThat(phone.markedAllRead).containsExactly("work")
    }
}
