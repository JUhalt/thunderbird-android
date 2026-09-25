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
import net.thunderbird.feature.wear.companion.WearGlanceVisibility
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.wear.R

class DemoPhoneConnectionTest {
    private val testSubject = DemoPhoneConnection(
        getString = { id -> if (id == R.string.demo_account_work) "Work" else "text $id" },
        now = { 1_700_000_000_000 },
    )

    @Test
    fun `offers the unified inbox, its views, and two accounts, with consistent unread counts`() = runTest {
        val mailboxes = testSubject.mailboxes.first()?.mailboxes.orEmpty()

        assertThat(mailboxes.map { it.id }).containsExactly(
            WearCompanion.UNIFIED_MAILBOX_ID,
            WearCompanion.UNREAD_MAILBOX_ID,
            WearCompanion.STARRED_MAILBOX_ID,
            "demo-work",
            "demo-personal",
        )

        val unified = testSubject.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()
        val work = testSubject.inbox("demo-work").first()
        val personal = testSubject.inbox("demo-personal").first()
        assertThat(unified?.messages?.size).isEqualTo((work?.messages?.size ?: 0) + (personal?.messages?.size ?: 0))
        assertThat(mailboxes.first().unreadCount).isEqualTo(unified?.unreadCount)
    }

    @Test
    fun `messages are newest first`() = runTest {
        val dates = testSubject.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()?.messages.orEmpty().map { it.date }

        assertThat(dates).isEqualTo(dates.sortedDescending())
    }

    @Test
    fun `mark read and star change the message and the unread count`() = runTest {
        val message = testSubject.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()!!.messages.first { !it.isRead }
        val unreadBefore = testSubject.mailboxes.first()!!.unifiedUnreadCount

        testSubject.performAction(message.id, WearMessageAction.MARK_READ)
        testSubject.performAction(message.id, WearMessageAction.STAR)

        val updated = testSubject.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()!!.messages.first {
            it.id == message.id
        }
        assertThat(updated.isRead).isTrue()
        assertThat(updated.isStarred).isTrue()
        assertThat(testSubject.mailboxes.first()!!.unifiedUnreadCount).isEqualTo(unreadBefore - 1)
    }

    @Test
    fun `archive removes the message from every inbox`() = runTest {
        val message = testSubject.inbox("demo-work").first()!!.messages.first()

        val result = testSubject.performAction(message.id, WearMessageAction.ARCHIVE)

        assertThat(result).isEqualTo(PhoneResult.Success)
        assertThat(testSubject.inbox("demo-work").first()!!.messages.map { it.id }).doesNotContain(message.id)
        assertThat(testSubject.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()!!.messages.map { it.id })
            .doesNotContain(message.id)
    }

    @Test
    fun `open on phone isn't available`() = runTest {
        assertThat(testSubject.openOnPhone("demo-welcome")).isEqualTo(PhoneResult.NotAvailableInDemo)
    }

    @Test
    fun `account mailboxes carry names, colors, and monograms`() = runTest {
        val work = testSubject.mailboxes.first()?.mailboxes?.first { it.id == "demo-work" }

        assertThat(work).isNotNull().all {
            prop(WearMailbox::name).isEqualTo("Work")
            prop(WearMailbox::color).isNotNull()
            prop(WearMailbox::monogram).isEqualTo("WO")
        }
    }

    @Test
    fun `unread and starred views contain only unread and starred messages`() = runTest {
        val unread = testSubject.inbox(WearCompanion.UNREAD_MAILBOX_ID).first()?.messages.orEmpty()
        val starred = testSubject.inbox(WearCompanion.STARRED_MAILBOX_ID).first()?.messages.orEmpty()

        assertThat(unread.all { !it.isRead }).isTrue()
        assertThat(unread.size).isEqualTo(testSubject.mailboxes.first()!!.unifiedUnreadCount)
        assertThat(starred.all { it.isStarred }).isTrue()
        assertThat(starred.isNotEmpty()).isTrue()
    }

    @Test
    fun `mark all read in an account leaves other accounts alone`() = runTest {
        val result = testSubject.markAllRead("demo-work")

        val mailboxes = testSubject.mailboxes.first()!!.mailboxes
        assertThat(result).isEqualTo(PhoneResult.Success)
        assertThat(mailboxes.first { it.id == "demo-work" }.unreadCount).isEqualTo(0)
        assertThat(mailboxes.first { it.id == "demo-personal" }.unreadCount > 0).isTrue()
    }

    @Test
    fun `messages know their account and the Tile may show everything`() = runTest {
        val messages = testSubject.inbox(WearCompanion.UNIFIED_MAILBOX_ID).first()?.messages.orEmpty()

        assertThat(messages.map { it.accountId }.toSet()).isEqualTo(setOf("demo-work", "demo-personal"))
        assertThat(testSubject.mailboxes.first()?.glanceVisibility).isEqualTo(WearGlanceVisibility.EVERYTHING)
    }

    @Test
    fun `replies can be tried`() = runTest {
        assertThat(testSubject.reply("demo-welcome", "Thanks!")).isEqualTo(PhoneResult.Success)
    }
}
