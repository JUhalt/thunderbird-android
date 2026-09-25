package net.thunderbird.feature.wear.companion.internal

import android.os.Handler
import android.os.Looper
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Starts [WearSnapshotPublisher] once the app has finished starting up, if the companion is turned on.
 *
 * Koin creates this eagerly while the app is still starting, before the mail engine is initialized. Posting to the
 * main thread defers the start until after `Application.onCreate()`.
 */
internal class WearCompanionStarter : KoinComponent {
    fun scheduleStart() {
        Handler(Looper.getMainLooper()).post {
            if (get<WearCompanionFeature>().isEnabled()) {
                get<WearSnapshotPublisher>().start()
            }
        }
    }
}
