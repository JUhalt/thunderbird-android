package net.thunderbird.feature.wear.companion.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.preference.LockScreenNotificationVisibility
import net.thunderbird.feature.wear.companion.WearGlanceVisibility

class GlanceVisibilityTest {

    @Test
    fun `glance visibility follows the lock screen notification setting`() {
        val expected = mapOf(
            LockScreenNotificationVisibility.EVERYTHING to WearGlanceVisibility.EVERYTHING,
            LockScreenNotificationVisibility.SENDERS to WearGlanceVisibility.SENDERS,
            LockScreenNotificationVisibility.MESSAGE_COUNT to WearGlanceVisibility.COUNT,
            LockScreenNotificationVisibility.APP_NAME to WearGlanceVisibility.NOTHING,
            LockScreenNotificationVisibility.NOTHING to WearGlanceVisibility.NOTHING,
        )

        for ((setting, glanceVisibility) in expected) {
            assertThat(setting.toGlanceVisibility()).isEqualTo(glanceVisibility)
        }
    }
}
