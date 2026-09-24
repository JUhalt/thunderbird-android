package net.thunderbird.wear

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/** Boots the real [ThunderWrenApplication] and launches the watch activity without a phone. */
@RunWith(AndroidJUnit4::class)
@Config(application = ThunderWrenApplication::class, sdk = [33], qualifiers = "w227dp-h227dp-round-watch")
class AppStartupTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `app starts without a phone`() {
        ActivityScenario.launch(ThunderWrenActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(scenario.state).isEqualTo(Lifecycle.State.RESUMED)
        }
    }
}
