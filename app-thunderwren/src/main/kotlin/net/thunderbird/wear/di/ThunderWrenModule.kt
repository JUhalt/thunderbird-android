package net.thunderbird.wear.di

import net.thunderbird.wear.ui.inbox.InboxViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val thunderWrenModule = module {
    viewModel { InboxViewModel(preferences = get()) }
}
