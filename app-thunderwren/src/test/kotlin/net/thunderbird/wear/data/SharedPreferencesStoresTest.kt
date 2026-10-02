package net.thunderbird.wear.data

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [33])
class SharedPreferencesStoresTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences("thunderwren", Context.MODE_PRIVATE)

    @BeforeTest
    fun setUp() {
        preferences.edit { clear() }
    }

    @Test
    fun `swipe settings default to archive on the left and going back on the right`() {
        val settings = SharedPreferencesWatchSettingsStore(context).settings.value

        assertThat(settings.swipeLeft).isEqualTo(MessageSwipeAction.ARCHIVE)
        assertThat(settings.swipeRight).isNull()
    }

    @Test
    fun `swipe settings of beta5 are kept`() {
        preferences.edit { putString("swipe_actions", "LEFT_DELETE_RIGHT_ARCHIVE") }

        val settings = SharedPreferencesWatchSettingsStore(context).settings.value

        assertThat(settings.swipeLeft).isEqualTo(MessageSwipeAction.DELETE)
        assertThat(settings.swipeRight).isEqualTo(MessageSwipeAction.ARCHIVE)
    }

    @Test
    fun `swipe settings are stored`() {
        SharedPreferencesWatchSettingsStore(context).update {
            it.copy(swipeLeft = MessageSwipeAction.DELETE, swipeRight = null)
        }

        val settings = SharedPreferencesWatchSettingsStore(context).settings.value

        assertThat(settings.swipeLeft).isEqualTo(MessageSwipeAction.DELETE)
        assertThat(settings.swipeRight).isNull()
    }

    @Test
    fun `demo started with try the demo in beta5 still gives way to the phone`() {
        preferences.edit { putBoolean("demo_mode", true) }

        assertThat(SharedPreferencesDemoModeStore(context).mode.value).isEqualTo(DemoMode.UNTIL_PHONE_CONNECTS)
    }

    @Test
    fun `demo mode is stored`() {
        SharedPreferencesDemoModeStore(context).setMode(DemoMode.ON)

        assertThat(SharedPreferencesDemoModeStore(context).mode.value).isEqualTo(DemoMode.ON)
    }
}
