package net.thunderbird.feature.wear.companion

import net.thunderbird.feature.wear.companion.internal.InboxPublicationSource
import net.thunderbird.feature.wear.companion.internal.MessagingControllerWearMessageActions
import net.thunderbird.feature.wear.companion.internal.PlayServicesWearDataLayer
import net.thunderbird.feature.wear.companion.internal.WearCompanionStarter
import net.thunderbird.feature.wear.companion.internal.WearDataLayer
import net.thunderbird.feature.wear.companion.internal.WearInboxPublisher
import net.thunderbird.feature.wear.companion.internal.WearMessageActions
import net.thunderbird.feature.wear.companion.internal.WearPublicationSource
import net.thunderbird.feature.wear.companion.internal.WearRequestHandler
import net.thunderbird.feature.wear.companion.internal.WearSnapshotPublisher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

/** Wear OS companion: publishes the inbox to a paired watch and handles its requests. See RFC 0010. */
val featureWearCompanionModule = module {
    factory<WearPublicationSource> {
        InboxPublicationSource(
            accountManager = get(),
            messageListRepository = get(),
            messageCountsProvider = get(),
            messageHelper = get(),
            clock = get(),
        )
    }
    single<WearDataLayer> { PlayServicesWearDataLayer(context = androidContext()) }
    single {
        WearSnapshotPublisher(
            publicationSource = get(),
            dataLayer = get(),
            messageListRepository = get(),
            logger = get(),
        )
    } binds arrayOf(WearInboxPublisher::class)
    factory<WearMessageActions> {
        MessagingControllerWearMessageActions(
            messagingController = get(),
            accountManager = get(),
        )
    }
    factory { WearRequestHandler(publisher = get(), messageActions = get()) }
    single(createdAtStart = true) { WearCompanionStarter().apply { scheduleStart() } }
}
