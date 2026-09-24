@file:Suppress("MagicNumber")

package net.thunderbird.wear.ui.model

import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class EmailHeader(
    val id: String,
    val senderName: String,
    val senderAddress: String,
    val subject: String,
    val snippet: String,
    val dateText: String,
    val isUnread: Boolean = false,
    val isStarred: Boolean = false,
    val accountColor: Color = Color(0xFF0A84FF),
)

data class EmailMessage(
    val header: EmailHeader,
    val body: String,
)

object SampleEmailData {
    val sampleHeaders: ImmutableList<EmailHeader> = persistentListOf(
        EmailHeader(
            id = "1",
            senderName = "Thunderbird Security",
            senderAddress = "security@thunderbird.net",
            subject = "New login from Wear OS Watch",
            snippet = "A new login to your Thunderbird account was detected on ThunderWren Watch.",
            dateText = "10:42 AM",
            isUnread = true,
            accountColor = Color(0xFF0A84FF),
        ),
        EmailHeader(
            id = "2",
            senderName = "Mozilla Developer Network",
            senderAddress = "notifications@mdn.mozilla.org",
            subject = "Weekly Tech Newsletter: Jetpack Compose Material 3",
            snippet = "Learn how to build adaptive, battery-efficient Wear OS applications using Compose M3.",
            dateText = "9:15 AM",
            isUnread = true,
            accountColor = Color(0xFFFF9800),
        ),
        EmailHeader(
            id = "3",
            senderName = "K-9 Mail Core Team",
            senderAddress = "dev@k9mail.app",
            subject = "Release 25.0 Changelog & Sync Protocols",
            snippet = "Here are the release notes and backend sync protocol updates for Thunderbird for Android.",
            dateText = "Yesterday",
            isUnread = false,
            isStarred = true,
            accountColor = Color(0xFF4CAF50),
        ),
        EmailHeader(
            id = "4",
            senderName = "GitHub Notifications",
            senderAddress = "notifications@github.com",
            subject = "[JUhalt/thunderbird-android] Issue #6 closed",
            snippet = "Milestone 6: Polishing Wrist Ergonomics & Upstream Readiness has been completed.",
            dateText = "Yesterday",
            isUnread = false,
            accountColor = Color(0xFF9C27B0),
        ),
    )

    fun getSampleMessage(id: String): EmailMessage {
        val header = sampleHeaders.find { it.id == id } ?: sampleHeaders.first()
        return EmailMessage(
            header = header,
            body = """
                Hi JUhalt,

                This is a sample email message previewing the ThunderWren wrist reader screen.

                ${header.snippet}

                ThunderWren leverages Thunderbird's powerful email engine while providing a wrist-first interface optimized for round smartwatches running Wear OS 3+.

                Best regards,
                The ThunderWren Team
            """.trimIndent(),
        )
    }
}
