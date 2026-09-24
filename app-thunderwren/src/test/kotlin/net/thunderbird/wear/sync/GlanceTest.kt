package net.thunderbird.wear.sync

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.UNREAD
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message

class GlanceTest {
    private val phone = FakePhoneConnection()

    @Test
    fun `nothing to show before the phone has published`() = runTest {
        assertThat(phone.currentGlance(maxMessages = 3)).isNull()
    }

    @Test
    fun `everything shows senders and subjects of the newest unread messages`() = runTest {
        publish(WearGlanceVisibility.EVERYTHING)

        val glance = phone.currentGlance(maxMessages = 2)

        assertThat(glance?.unreadCount).isEqualTo(3)
        assertThat(glance?.latestUnread.orEmpty()).containsExactly(
            GlanceMessage(id = "m1", sender = "Ada", subject = "Lunch?"),
            GlanceMessage(id = "m2", sender = "Charles", subject = "Engine"),
        )
    }

    @Test
    fun `senders hides subjects`() = runTest {
        publish(WearGlanceVisibility.SENDERS)

        val glance = phone.currentGlance(maxMessages = 3)

        assertThat(glance?.latestUnread.orEmpty().map { it.sender }).containsExactly("Ada", "Charles", "Grace")
        assertThat(glance?.latestUnread.orEmpty().mapNotNull { it.subject }).isEmpty()
    }

    @Test
    fun `count shows only the unread count`() = runTest {
        publish(WearGlanceVisibility.COUNT)

        val glance = phone.currentGlance(maxMessages = 3)

        assertThat(glance?.unreadCount).isEqualTo(3)
        assertThat(glance?.latestUnread.orEmpty()).isEmpty()
    }

    @Test
    fun `nothing hides even the unread count`() = runTest {
        publish(WearGlanceVisibility.NOTHING)

        val glance = phone.currentGlance(maxMessages = 3)

        assertThat(glance?.unreadCount).isNull()
        assertThat(glance?.latestUnread.orEmpty()).isEmpty()
    }

    @Test
    fun `unread messages come from the unified inbox if the phone doesn't publish the unread view`() = runTest {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED, unreadCount = 1)),
            inboxes = mapOf(UNIFIED to listOf(message("read", isRead = true), message("new", senderName = "Ada"))),
            glanceVisibility = WearGlanceVisibility.SENDERS,
        )

        val glance = phone.currentGlance(maxMessages = 3)

        assertThat(glance?.latestUnread.orEmpty().map { it.id }).containsExactly("new")
    }

    private fun publish(visibility: WearGlanceVisibility) {
        val unread = listOf(
            message("m1", senderName = "Ada", subject = "Lunch?"),
            message("m2", senderName = "Charles", subject = "Engine"),
            message("m3", senderName = "Grace", subject = "Compilers"),
        )
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED, unreadCount = 3), mailbox(UNREAD, unreadCount = 3)),
            inboxes = mapOf(UNIFIED to unread, UNREAD to unread),
            glanceVisibility = visibility,
        )
    }
}
