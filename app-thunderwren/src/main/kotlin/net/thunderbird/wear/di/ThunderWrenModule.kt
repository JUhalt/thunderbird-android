package net.thunderbird.wear.di

import net.thunderbird.core.featureflag.inject.featureFlagModule
import net.thunderbird.core.featureflag.model.EmptyAppVariantOverride
import net.thunderbird.core.featureflag.serialization.FlagRegistryOverrideSerializer
import net.thunderbird.wear.ui.inbox.InboxViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val thunderWrenModule = module {
    includes(featureFlagModule)

    factory {
        FlagRegistryOverrideSerializer(
            k9Factory = EmptyAppVariantOverride,
            thunderbirdFactory = EmptyAppVariantOverride,
        )
    }

    viewModel { InboxViewModel(preferences = get()) }
}
