package net.thunderbird.wear

import android.app.Application
import android.content.Context
import app.k9mail.legacy.di.DI
import com.fsck.k9.Core
import com.fsck.k9.K9
import net.thunderbird.core.logging.Logger
import net.thunderbird.legacy.logging.Log
import net.thunderbird.wear.di.thunderWrenAppModule
import org.koin.android.ext.android.inject

class ThunderWrenApplication : Application() {

    private val logger: Logger by inject()

    override fun attachBaseContext(base: Context?) {
        Core.earlyInit()

        // Start Koin early so it is ready by the time content providers are initialized.
        DI.start(this, listOf(thunderWrenAppModule))
        Log.logger = logger

        super.attachBaseContext(base)
    }

    override fun onCreate() {
        super.onCreate()

        K9.init(this)
        Core.init(this)
    }
}
