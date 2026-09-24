package com.fsck.k9.message

import org.koin.dsl.module

val messageModule = module {
    factory {
        QuickReplySender(
            accountManager = get(),
            messagingController = get(),
            textQuoteCreator = get(),
            generalSettingsManager = get(),
        )
    }
}
