package net.thunderbird.feature.wear.companion

import kotlinx.coroutines.flow.map
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.feature.wear.companion.internal.InboxPublicationSource
import net.thunderbird.feature.wear.companion.internal.MessagingControllerWearMailboxActions
import net.thunderbird.feature.wear.companion.internal.MessagingControllerWearMessageActions
import net.thunderbird.feature.wear.companion.internal.PlayServicesWearDataLayer
import net.thunderbird.feature.wear.companion.internal.QuickReplyWearReplySender
import net.thunderbird.feature.wear.companion.internal.WearCompanionStarter
import net.thunderbird.feature.wear.companion.internal.WearDataLayer
import net.thunderbird.feature.wear.companion.internal.WearInboxPublisher
import net.thunderbird.feature.wear.companion.internal.WearMailboxActions
import net.thunderbird.feature.wear.companion.internal.WearMessageActions
import net.thunderbird.feature.wear.companion.internal.WearPublicationSource
import net.thunderbird.feature.wear.companion.internal.WearReplySender
import net.thunderbird.feature.wear.companion.internal.WearRequestHandler
import net.thunderbird.feature.wear.companion.internal.WearSnapshotPublisher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.binds
import org.koin.dsl.module

/** Wear OS companion: publishes the inbox to a paired watch and handles its requests. See RFC 0010. */
val featureWearCompanionModule = module {
    factory<WearPublicationSource> {
        val generalSettingsManager = get<GeneralSettingsManager>()
        InboxPublicationSource(
            accountManager = get(),
            messageListRepository = get(),
            messageCountsProvider = get(),
            messageHelper = get(),
            monogramCreator = get(),
            lockScreenNotificationVisibility = {
                generalSettingsManager.getConfig().notification.lockScreenNotificationVisibility
            },
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
            settingChanges = get<GeneralSettingsManager>().getConfigFlow()
                .map { settings -> settings.notification.lockScreenNotificationVisibility },
        )
    } binds arrayOf(WearInboxPublisher::class)
    factory<WearMessageActions> {
        MessagingControllerWearMessageActions(
            messagingController = get(),
            accountManager = get(),
        )
    }
    factory<WearMailboxActions> {
        MessagingControllerWearMailboxActions(
            messagingController = get(),
            accountManager = get(),
            messageListRepository = get(),
        )
    }
    factory<WearReplySender> { QuickReplyWearReplySender(quickReplySender = get()) }
    factory {
        WearRequestHandler(
            publisher = get(),
            messageActions = get(),
            mailboxActions = get(),
            replySender = get(),
        )
    }
    single(createdAtStart = true) { WearCompanionStarter().apply { scheduleStart() } }
}
