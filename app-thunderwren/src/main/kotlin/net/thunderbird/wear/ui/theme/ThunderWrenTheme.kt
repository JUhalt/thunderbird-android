package net.thunderbird.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

val ThunderWrenColorScheme = ColorScheme(
    primary = Color(0xFF72A3FF),
    onPrimary = Color(0xFF003062),
    primaryContainer = Color(0xFF004589),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBDC7DC),
    onSecondary = Color(0xFF273142),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFD9E3F9),
    surfaceContainer = Color(0xFF1E2024),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFC4C6CF),
    background = Color(0xFF000000), // OLED true black for battery savings
    onBackground = Color(0xFFE2E2E6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun ThunderWrenTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ThunderWrenColorScheme,
        content = content,
    )
}
