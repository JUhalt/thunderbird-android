package net.thunderbird.wear

import net.thunderbird.app.common.FeatureFlagApplication
import net.thunderbird.app.common.appCommonModule
import net.thunderbird.wear.di.thunderWrenModule
import org.koin.core.module.Module
import org.koin.dsl.module

class ThunderWrenApplication : FeatureFlagApplication() {
    override val appName: String = "thunderwren"
    override val appVersion: String = "0.1.0"

    override fun provideAppModule(): Module {
        return module {
            includes(appCommonModule)
            includes(thunderWrenModule)
        }
    }
}
