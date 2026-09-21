package net.thunderbird.wear

import android.app.Application
import app.k9mail.legacy.di.DI
import com.fsck.k9.Core
import com.fsck.k9.K9
import net.thunderbird.app.common.appCommonModule
import net.thunderbird.core.logging.Logger
import net.thunderbird.legacy.logging.Log
import net.thunderbird.wear.di.thunderWrenModule
import org.koin.android.ext.android.inject

class ThunderWrenApplication : Application() {

    private val logger: Logger by inject()

    override fun onCreate() {
        super.onCreate()

        Core.earlyInit()

        // Start Koin dependency injection for ThunderWren
        DI.start(this, listOf(appCommonModule, thunderWrenModule))
        Log.logger = logger

        K9.init(this)
        Core.init(this)
    }
}
