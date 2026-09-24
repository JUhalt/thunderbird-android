package net.thunderbird.wear

import android.app.Application
import net.thunderbird.wear.di.thunderWrenModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ThunderWrenApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@ThunderWrenApplication)
            modules(thunderWrenModule)
        }
    }
}
