package net.thunderbird.feature.wear.companion

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class WearCompanionTest {

    @Test
    fun `open-on-phone URI round trips a message ID with reserved characters`() {
        val messageId = "#:YWNj+b3VudA==:MQ==:dW/lk"

        val uri = WearCompanion.openOnPhoneUri(messageId)

        assertThat(WearCompanion.parseOpenOnPhoneUri(uri)).isEqualTo(messageId)
    }

    @Test
    fun `inbox path is the prefix followed by the mailbox ID`() {
        assertThat(WearCompanion.inboxPath("uuid-1")).isEqualTo("/thunderwren/v1/inbox/uuid-1")
    }

    @Test
    fun `parsing rejects URIs that weren't created by openOnPhoneUri`() {
        assertThat(WearCompanion.parseOpenOnPhoneUri("https://example.com/?message=x")).isNull()
        assertThat(WearCompanion.parseOpenOnPhoneUri("thunderwren://open?message=")).isNull()
        assertThat(WearCompanion.parseOpenOnPhoneUri("thunderwren://open?message=a&other=b")).isNull()
    }

    @Test
    fun `only account inboxes are accounts`() {
        fun mailbox(id: String) = WearMailbox(id = id, name = "", email = "", color = null, unreadCount = 0)

        assertThat(mailbox(WearCompanion.UNIFIED_MAILBOX_ID).isAccount).isFalse()
        assertThat(mailbox(WearCompanion.UNREAD_MAILBOX_ID).isAccount).isFalse()
        assertThat(mailbox(WearCompanion.STARRED_MAILBOX_ID).isAccount).isFalse()
        assertThat(mailbox("0b1f3c1e-account-uuid").isAccount).isTrue()
    }
}
