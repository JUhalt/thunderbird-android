package com.fsck.k9.notification

import app.k9mail.legacy.message.controller.MessageReference
import com.fsck.k9.mail.Address

internal data class NotificationContent(
    val messageReference: MessageReference,
    val sender: Address,
    val subject: String,
    val preview: CharSequence,
    val summary: CharSequence,
    /** Encrypted messages can't be answered with a quick reply, which would be sent unencrypted. */
    val isEncrypted: Boolean = false,
)
