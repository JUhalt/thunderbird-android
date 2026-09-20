package net.thunderbird.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

// Official Thunderbird Brand Palette adapted for Wear OS OLED displays
val ThunderbirdBlue = Color(0xFF0A84FF)
val ThunderbirdSkyBlue = Color(0xFF72A3FF)
val ThunderbirdDarkBlue = Color(0xFF004589)
val ThunderbirdOledBlack = Color(0xFF000000)

val ThunderWrenColorScheme = ColorScheme(
    primary = ThunderbirdBlue,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = ThunderbirdDarkBlue,
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = ThunderbirdSkyBlue,
    onSecondary = Color(0xFF00227B),
    secondaryContainer = Color(0xFF003B90),
    onSecondaryContainer = Color(0xFFD6E3FF),
    surfaceContainer = Color(0xFF161B22),
    onSurface = Color(0xFFE6EDF3),
    onSurfaceVariant = Color(0xFF8B949E),
    background = ThunderbirdOledBlack, // OLED true black for watch battery efficiency
    onBackground = Color(0xFFE6EDF3),
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
