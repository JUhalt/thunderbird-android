package net.thunderbird.wear.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import net.thunderbird.feature.wear.companion.WearMailbox

/** A small dot, used as the unread indicator. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * The account's monogram on a circle in the account's color, like the account avatars in Thunderbird.
 *
 * It's decorative: whatever shows it also names the account for screen readers.
 */
@Composable
fun AccountMonogram(
    account: WearMailbox,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
) {
    val background = account.color?.let(::Color) ?: MaterialTheme.colorScheme.primary
    val contentColor = if (background.luminance() > HALF_LUMINANCE) Color.Black else Color.White
    // The circle doesn't grow with the font size, so neither may its letters.
    val fontSize = with(LocalDensity.current) { (size * MONOGRAM_TEXT_RATIO).toSp() }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = account.displayMonogram,
            color = contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

/** A button icon with the account's monogram, or no icon for the unified inbox and its views. */
fun accountIcon(mailbox: WearMailbox): (@Composable BoxScope.() -> Unit)? {
    return if (mailbox.isAccount) {
        { AccountMonogram(mailbox) }
    } else {
        null
    }
}

private const val HALF_LUMINANCE = 0.5f
private const val MONOGRAM_TEXT_RATIO = 0.42f
