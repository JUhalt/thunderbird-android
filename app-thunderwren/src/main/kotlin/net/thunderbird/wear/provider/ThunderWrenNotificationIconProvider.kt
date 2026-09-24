package net.thunderbird.wear.provider

import app.k9mail.core.android.common.provider.NotificationIconResourceProvider
import net.thunderbird.wear.R

class ThunderWrenNotificationIconProvider : NotificationIconResourceProvider {
    override val pushNotificationIcon: Int = R.drawable.ic_notification
}
