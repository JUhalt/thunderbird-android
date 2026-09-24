package net.thunderbird.wear.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A small dot, used for account colors and the unread indicator. */
@Composable
fun ColorDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/** A button icon showing the account [color], or no icon if there is none (the unified inbox). */
fun accountColorIcon(color: Int?): (@Composable BoxScope.() -> Unit)? = color?.let { accountColor ->
    { ColorDot(Color(accountColor)) }
}
