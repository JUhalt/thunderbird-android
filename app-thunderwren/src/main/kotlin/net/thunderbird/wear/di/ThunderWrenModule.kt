package net.thunderbird.wear.di

import com.fsck.k9.AppConfig
import com.fsck.k9.DefaultAppConfig
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.featureflag.inject.featureFlagModule
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.wear.BuildConfig
import net.thunderbird.wear.ui.inbox.InboxViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val thunderWrenModule = module {
    includes(featureFlagModule)

    factory {
        FlagRegistryOverrideSerializer(
            k9Factory = EmptyAppVariantOverride,
            thunderbirdFactory = EmptyAppVariantOverride,
        )
    }

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { DefaultAppConfig(componentsToDisable = emptyList()) }
    single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }

    viewModel { InboxViewModel(preferences = get()) }
}
