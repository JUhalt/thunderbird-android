package net.thunderbird.feature.wear.companion

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class WearCompanionTest {

    @Test
    fun `open-on-phone URI round trips a message ID with reserved characters`() {
        val messageId = "#:YWNj+b3VudA==:MQ==:dW/lk"

        val uri = WearCompanion.openOnPhoneUri(messageId)

        assertThat(WearCompanion.parseOpenOnPhoneUri(uri)).isEqualTo(messageId)
    }

    @Test
    fun `parsing rejects URIs that weren't created by openOnPhoneUri`() {
        assertThat(WearCompanion.parseOpenOnPhoneUri("https://example.com/?message=x")).isNull()
        assertThat(WearCompanion.parseOpenOnPhoneUri("thunderwren://open?message=")).isNull()
        assertThat(WearCompanion.parseOpenOnPhoneUri("thunderwren://open?message=a&other=b")).isNull()
    }
}
