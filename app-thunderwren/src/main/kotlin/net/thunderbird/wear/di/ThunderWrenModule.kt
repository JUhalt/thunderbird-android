package net.thunderbird.wear.di

import net.thunderbird.wear.data.DataLayerPhoneConnection
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.data.SharedPreferencesSelectedMailboxStore
import net.thunderbird.wear.ui.inbox.InboxViewModel
import net.thunderbird.wear.ui.mailbox.MailboxPickerViewModel
import net.thunderbird.wear.ui.reader.MessageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val thunderWrenModule = module {
    single<PhoneConnection> { DataLayerPhoneConnection(context = androidContext()) }
    single<SelectedMailboxStore> { SharedPreferencesSelectedMailboxStore(context = androidContext()) }

    viewModel { InboxViewModel(phoneConnection = get(), selectedMailboxStore = get()) }
    viewModel { MailboxPickerViewModel(phoneConnection = get(), selectedMailboxStore = get()) }
    viewModel { parameters ->
        MessageViewModel(
            messageId = parameters.get(),
            phoneConnection = get(),
            selectedMailboxStore = get(),
        )
    }
}
