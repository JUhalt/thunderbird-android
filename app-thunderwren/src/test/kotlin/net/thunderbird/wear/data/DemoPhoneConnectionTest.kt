package net.thunderbird.wear.data

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageAction

class DemoPhoneConnectionTest {
    private val demo = DemoPhoneConnection(now = { 1_700_000_000_000 })

    @Test
    fun `offers the unified inbox and two accounts, with consistent unread counts`() = runTest {
        val mailboxes = demo.mailboxes.first()?.mailboxes.orEmpty()

        assertThat(
            mailboxes.map {
                it.id
            },
        ).containsExactly(WearCompanion.UNIFIED_MAILBOX_ID, "demo-work", "demo-personal")

        val unified = demo.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()
        val work = demo.inbox("demo-work").first()
        val personal = demo.inbox("demo-personal").first()
        assertThat(unified?.messages?.size).isEqualTo((work?.messages?.size ?: 0) + (personal?.messages?.size ?: 0))
        assertThat(mailboxes.first().unreadCount).isEqualTo(unified?.unreadCount)
    }

    @Test
    fun `messages are newest first`() = runTest {
        val dates = demo.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()?.messages.orEmpty().map { it.date }

        assertThat(dates).isEqualTo(dates.sortedDescending())
    }

    @Test
    fun `mark read and star change the message and the unread count`() = runTest {
        val message = demo.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()!!.messages.first { !it.isRead }
        val unreadBefore = demo.mailboxes.first()!!.unifiedUnreadCount

        demo.performAction(message.id, WearMessageAction.MARK_READ)
        demo.performAction(message.id, WearMessageAction.STAR)

        val updated = demo.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()!!.messages.first { it.id == message.id }
        assertThat(updated.isRead).isTrue()
        assertThat(updated.isStarred).isTrue()
        assertThat(demo.mailboxes.first()!!.unifiedUnreadCount).isEqualTo(unreadBefore - 1)
    }

    @Test
    fun `archive removes the message from every inbox`() = runTest {
        val message = demo.inbox("demo-work").first()!!.messages.first()

        val result = demo.performAction(message.id, WearMessageAction.ARCHIVE)

        assertThat(result).isEqualTo(PhoneResult.Success)
        assertThat(demo.inbox("demo-work").first()!!.messages.map { it.id }).doesNotContain(message.id)
        assertThat(demo.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()!!.messages.map { it.id })
            .doesNotContain(message.id)
    }

    @Test
    fun `open on phone isn't available`() = runTest {
        assertThat(demo.openOnPhone("demo-welcome")).isEqualTo(PhoneResult.NotAvailableInDemo)
    }

    @Test
    fun `account mailboxes carry names and colors`() = runTest {
        val work = demo.mailboxes.first()?.mailboxes?.first { it.id == "demo-work" }

        assertThat(work).isNotNull().all {
            prop(WearMailbox::name).isEqualTo("Work")
            prop(WearMailbox::color).isNotNull()
        }
    }
}
