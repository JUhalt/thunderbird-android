package net.thunderbird.wear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.DimensionBuilders.sp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import net.thunderbird.wear.R
import net.thunderbird.wear.ThunderWrenActivity
import net.thunderbird.wear.sync.Glance
import net.thunderbird.wear.sync.GlanceMessage

/**
 * The Tile's layout: the unread count and, if Thunderbird's privacy setting allows it, the newest unread messages.
 * Tapping a message opens it; tapping anything else opens the app.
 */
internal class UnreadTileLayout(private val context: Context) {

    fun create(glance: Glance?, screenWidthDp: Int): LayoutElementBuilders.LayoutElement {
        val content = LayoutElementBuilders.Column.Builder()
            .setWidth(dp(screenWidthDp * CONTENT_WIDTH_FRACTION))
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)

        val unreadCount = glance?.unreadCount
        val messages = glance?.latestUnread.orEmpty()
        if (unreadCount == null || messages.isEmpty()) {
            val status = when {
                glance == null -> context.getString(R.string.tile_not_connected)
                unreadCount == null -> context.getString(R.string.tile_open_app)
                else -> unreadCountText(unreadCount)
            }
            content.addContent(summary(status))
        } else {
            content.addContent(unreadHeader(unreadCount))
            // Two lines per message when subjects are shown, one otherwise.
            val maxMessages = if (messages.any { it.subject != null }) MAX_MESSAGES_WITH_SUBJECT else MAX_MESSAGES
            messages.take(maxMessages).forEachIndexed { index, message ->
                content.addContent(LayoutElementBuilders.Spacer.Builder().setHeight(dp(MESSAGE_SPACING_DP)).build())
                content.addContent(messageRow(index, message))
            }
        }

        return LayoutElementBuilders.Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setVerticalAlignment(LayoutElementBuilders.VERTICAL_ALIGN_CENTER)
            .addContent(content.build())
            .build()
    }

    /** The app name and one line of status, opening the app. */
    private fun summary(status: String): LayoutElementBuilders.LayoutElement {
        return LayoutElementBuilders.Column.Builder()
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(openOnTap(CLICKABLE_ID_OPEN_APP, context.getString(R.string.tile_description, status)))
            .addContent(text(context.getString(R.string.app_name), SECONDARY_SIZE_SP, SECONDARY_COLOR))
            .addContent(text(status, TITLE_SIZE_SP, PRIMARY_COLOR, isBold = true, maxLines = 2))
            .build()
    }

    private fun unreadHeader(unreadCount: Int): LayoutElementBuilders.LayoutElement {
        val status = unreadCountText(unreadCount)
        return LayoutElementBuilders.Box.Builder()
            .setModifiers(openOnTap(CLICKABLE_ID_OPEN_APP, context.getString(R.string.tile_description, status)))
            .addContent(text(status, TITLE_SIZE_SP, ACCENT_COLOR, isBold = true))
            .build()
    }

    private fun messageRow(index: Int, message: GlanceMessage): LayoutElementBuilders.LayoutElement {
        val description = context.getString(
            R.string.tile_message_description,
            message.sender,
            message.subject.orEmpty(),
        )
        val row = LayoutElementBuilders.Column.Builder()
            .setWidth(expand())
            .setHorizontalAlignment(LayoutElementBuilders.HORIZONTAL_ALIGN_CENTER)
            .setModifiers(openOnTap("$CLICKABLE_ID_MESSAGE_PREFIX$index", description, messageId = message.id))
            .addContent(text(message.sender, BODY_SIZE_SP, PRIMARY_COLOR, isBold = true))

        message.subject?.let { subject ->
            row.addContent(text(subject, SECONDARY_SIZE_SP, SECONDARY_COLOR))
        }

        return row.build()
    }

    private fun unreadCountText(unreadCount: Int): String {
        return context.resources.getQuantityString(R.plurals.unread_count, unreadCount, unreadCount)
    }

    private fun text(
        text: String,
        sizeSp: Float,
        color: Int,
        isBold: Boolean = false,
        maxLines: Int = 1,
    ): LayoutElementBuilders.Text {
        val fontStyle = LayoutElementBuilders.FontStyle.Builder()
            .setSize(sp(sizeSp))
            .setColor(argb(color))
            .setWeight(
                if (isBold) LayoutElementBuilders.FONT_WEIGHT_BOLD else LayoutElementBuilders.FONT_WEIGHT_NORMAL,
            )
            .build()

        return LayoutElementBuilders.Text.Builder()
            .setText(text)
            .setFontStyle(fontStyle)
            .setMaxLines(maxLines)
            .setOverflow(LayoutElementBuilders.TEXT_OVERFLOW_ELLIPSIZE_END)
            .setMultilineAlignment(LayoutElementBuilders.TEXT_ALIGN_CENTER)
            .build()
    }

    /** Opens the app, or the message with [messageId] in it. */
    private fun openOnTap(
        clickableId: String,
        description: String,
        messageId: String? = null,
    ): ModifiersBuilders.Modifiers {
        val activity = ActionBuilders.AndroidActivity.Builder()
            .setPackageName(context.packageName)
            .setClassName(ThunderWrenActivity::class.java.name)
        if (messageId != null) {
            activity.addKeyToExtraMapping(
                ThunderWrenActivity.EXTRA_MESSAGE_ID,
                ActionBuilders.AndroidStringExtra.Builder().setValue(messageId).build(),
            )
        }

        val clickable = ModifiersBuilders.Clickable.Builder()
            .setId(clickableId)
            .setOnClick(ActionBuilders.LaunchAction.Builder().setAndroidActivity(activity.build()).build())
            .build()

        return ModifiersBuilders.Modifiers.Builder()
            .setClickable(clickable)
            .setSemantics(ModifiersBuilders.Semantics.Builder().setContentDescription(description).build())
            .build()
    }

    companion object {
        /** The most messages the Tile shows. */
        const val MAX_MESSAGES = 3
        const val MAX_MESSAGES_WITH_SUBJECT = 2

        const val CLICKABLE_ID_OPEN_APP = "open_app"
        const val CLICKABLE_ID_MESSAGE_PREFIX = "open_message_"

        // Keep the text clear of a round screen's edges.
        private const val CONTENT_WIDTH_FRACTION = 0.76f
        private const val MESSAGE_SPACING_DP = 6f
        private const val TITLE_SIZE_SP = 16f
        private const val BODY_SIZE_SP = 14f
        private const val SECONDARY_SIZE_SP = 12f
        private const val PRIMARY_COLOR = 0xFFFFFFFF.toInt()
        private const val SECONDARY_COLOR = 0xFFC9D3E6.toInt()

        // The lightning bolt of the ThunderWren logo.
        private const val ACCENT_COLOR = 0xFFFFC83D.toInt()
    }
}
