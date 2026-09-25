package net.thunderbird.feature.wear.companion.internal

import android.app.Activity
import android.os.Bundle
import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.activity.MessageHomeActivity
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.wear.companion.WearCompanion
import org.koin.android.ext.android.inject

/**
 * Opens a message in the phone app when the watch asks for it with a URI from [WearCompanion.openOnPhoneUri].
 *
 * The activity is exported, so it only opens messages of accounts that exist on this phone, and only while the
 * companion is turned on.
 */
internal class OpenOnPhoneActivity : Activity() {
    private val accountManager: LegacyAccountDtoManager by inject()
    private val feature: WearCompanionFeature by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reference = intent.data?.toString()
            ?.takeIf { feature.isEnabled() }
            ?.let(WearCompanion::parseOpenOnPhoneUri)
            ?.let(MessageReference::parse)
            ?.takeIf { accountManager.getAccount(it.accountUuid) != null }

        if (reference != null) {
            startActivity(
                MessageHomeActivity.actionDisplayMessageIntent(
                    context = this,
                    messageReference = reference,
                    openInUnifiedInbox = true,
                ),
            )
        }

        finish()
    }
}
