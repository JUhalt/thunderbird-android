package net.thunderbird.wear

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.k9mail.legacy.mailstore.MessageStoreManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.fsck.k9.Preferences
import com.fsck.k9.backend.BackendManager
import com.fsck.k9.controller.MessagingController
import com.fsck.k9.notification.NotificationController
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

/**
 * Boots the real [ThunderWrenApplication] and launches the watch activity, catching startup crashes
 * (missing Koin definitions, engine initialization failures, Compose errors) without an emulator.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = ThunderWrenApplication::class, sdk = [33], qualifiers = "w227dp-h227dp-round-watch")
class AppStartupTest {

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `app starts and shows the inbox`() {
        ActivityScenario.launch(ThunderWrenActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)

            assertThat(scenario.state).isEqualTo(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun `mail engine components used in the background can be created`() {
        // These are created lazily on background threads after startup, where a failure crashes the app
        // without failing the launch test above.
        val koin = GlobalContext.get()

        assertThat(koin.get<Preferences>()).isNotNull()
        assertThat(koin.get<MessageStoreManager>()).isNotNull()
        assertThat(koin.get<BackendManager>()).isNotNull()
        assertThat(koin.get<NotificationController>()).isNotNull()
        assertThat(koin.get<MessagingController>()).isNotNull()
    }
}
