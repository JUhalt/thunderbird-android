package net.thunderbird.wear.di

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import app.k9mail.feature.migration.launcher.featureMigrationModule
import app.k9mail.feature.telemetry.telemetryModule
import com.fsck.k9.AppConfig
import com.fsck.k9.DefaultAppConfig
import com.fsck.k9.preferences.FilePrefixProvider
import net.thunderbird.android.feature.mail.message.reader.api.css.DefaultCssClassNameProvider
import net.thunderbird.app.common.appCommonModule
import net.thunderbird.backend.api.BackendFactory
import net.thunderbird.core.common.oauth.OAuthConfigurationFactory
import net.thunderbird.core.common.provider.AppNameProvider
import net.thunderbird.core.common.provider.BrandNameProvider
import net.thunderbird.core.featureflag.inject.featureFlagModule
import net.thunderbird.core.featureflag.model.AppVariantOverrides
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.api.ThemeProvider
import net.thunderbird.feature.mail.message.list.internal.featureMessageListModule
import net.thunderbird.feature.mail.message.reader.api.css.CssClassNameProvider
import net.thunderbird.wear.BuildConfig
import net.thunderbird.wear.provider.ThunderWrenAppNameProvider
import net.thunderbird.wear.provider.ThunderWrenFeatureThemeProvider
import net.thunderbird.wear.provider.ThunderWrenNotificationIconProvider
import net.thunderbird.wear.provider.ThunderWrenThemeProvider
import net.thunderbird.wear.ui.inbox.InboxViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.binds
import org.koin.dsl.module

val thunderWrenModule = module {
    includes(featureFlagModule)
    includes(telemetryModule)
    includes(featureMigrationModule)
    // Provides engine definitions the mail store and MessagingController rely on (e.g. local UID prefix).
    includes(featureMessageListModule)

    factory<AppVariantOverrides.Factory> { EmptyAppVariantOverride }
    factory {
        FlagRegistryOverrideSerializer(
            k9Factory = EmptyAppVariantOverride,
            thunderbirdFactory = get(),
        )
    }

    single(named("ClientInfoAppName")) { BuildConfig.CLIENT_INFO_APP_NAME }
    single(named("ClientInfoAppVersion")) { BuildConfig.VERSION_NAME }
    single<AppConfig> { DefaultAppConfig(componentsToDisable = emptyList()) }
    single<OAuthConfigurationFactory> { OAuthConfigurationFactory { emptyMap() } }

    // Same as the phone app's release configuration: no demo backends or extra auto-discoveries.
    single<Map<String, BackendFactory>>(named("developmentBackends")) { emptyMap() }
    single<List<AutoDiscovery>>(named("extraAutoDiscoveries")) { emptyList() }

    single<CssClassNameProvider> {
        DefaultCssClassNameProvider(
            featureFlagProvider = get(),
            defaultNamespaceClassName = BuildConfig.APPLICATION_ID.replace(oldValue = ".", newValue = "-"),
        )
    }

    single {
        ThunderWrenAppNameProvider(androidContext())
    } binds arrayOf(AppNameProvider::class, BrandNameProvider::class, FilePrefixProvider::class)

    single<ThemeProvider> { ThunderWrenThemeProvider() }
    single<FeatureThemeProvider> { ThunderWrenFeatureThemeProvider() }
    single<NotificationIconResourceProvider> { ThunderWrenNotificationIconProvider() }

    viewModel { InboxViewModel(preferences = get()) }
}

/**
 * Complete Koin graph for the watch app: the shared Thunderbird engine plus the watch-specific definitions.
 */
val thunderWrenAppModule = module {
    includes(appCommonModule)
    includes(thunderWrenModule)
}
