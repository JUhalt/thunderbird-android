package net.thunderbird.wear

import net.thunderbird.app.common.BaseApplication
import net.thunderbird.app.common.appCommonModule
import net.thunderbird.wear.di.thunderWrenModule
import org.koin.core.module.Module
import org.koin.dsl.module

class ThunderWrenApplication : BaseApplication() {
    override fun provideAppModule(): Module {
        return module {
            includes(appCommonModule)
            includes(thunderWrenModule)
        }
    }
}
