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
    fun `folder mailbox ID round trips the account and folder`() {
        val mailboxId = WearCompanion.folderMailboxId("0b1f3c1e-account-uuid", 42)

        assertThat(WearCompanion.parseFolderMailboxId(mailboxId)).isEqualTo("0b1f3c1e-account-uuid" to 42L)
    }

    @Test
    fun `parsing rejects mailbox IDs that aren't folders`() {
        assertThat(WearCompanion.parseFolderMailboxId(WearCompanion.UNIFIED_MAILBOX_ID)).isNull()
        assertThat(WearCompanion.parseFolderMailboxId("0b1f3c1e-account-uuid")).isNull()
        assertThat(WearCompanion.parseFolderMailboxId("folder:x:account")).isNull()
        assertThat(WearCompanion.parseFolderMailboxId("folder:42")).isNull()
        assertThat(WearCompanion.parseFolderMailboxId("folder:42:")).isNull()
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
