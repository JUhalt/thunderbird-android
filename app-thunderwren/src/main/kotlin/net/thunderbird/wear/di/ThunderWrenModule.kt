package net.thunderbird.wear.di

import net.thunderbird.core.logging.DefaultLogger
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.logging.console.ConsoleLogSink
import net.thunderbird.wear.BuildConfig
import net.thunderbird.wear.data.DataLayerPhoneConnection
import net.thunderbird.wear.data.DemoAwarePhoneConnection
import net.thunderbird.wear.data.DemoModeStore
import net.thunderbird.wear.data.DemoPhoneConnection
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.data.SharedPreferencesDemoModeStore
import net.thunderbird.wear.data.SharedPreferencesSelectedMailboxStore
import net.thunderbird.wear.ui.inbox.InboxViewModel
import net.thunderbird.wear.ui.mailbox.MailboxPickerViewModel
import net.thunderbird.wear.ui.reader.MessageViewModel
import net.thunderbird.wear.ui.reply.ReplyViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val thunderWrenModule = module {
    single<Logger> {
        DefaultLogger(sink = ConsoleLogSink(level = if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.INFO))
    }
    single<DemoModeStore> { SharedPreferencesDemoModeStore(context = androidContext()) }
    single<PhoneConnection> {
        DemoAwarePhoneConnection(
            phone = DataLayerPhoneConnection(context = androidContext(), logger = get()),
            demo = DemoPhoneConnection(getString = androidContext()::getString),
            demoModeStore = get(),
        )
    }
    single<SelectedMailboxStore> { SharedPreferencesSelectedMailboxStore(context = androidContext()) }

    viewModel { InboxViewModel(phoneConnection = get(), selectedMailboxStore = get(), demoModeStore = get()) }
    viewModel { MailboxPickerViewModel(phoneConnection = get(), selectedMailboxStore = get()) }
    viewModel { (messageId: String, mailboxId: String) ->
        MessageViewModel(messageId = messageId, mailboxId = mailboxId, phoneConnection = get())
    }
    viewModel { (messageId: String, mailboxId: String) ->
        ReplyViewModel(messageId = messageId, mailboxId = mailboxId, phoneConnection = get())
    }
}
