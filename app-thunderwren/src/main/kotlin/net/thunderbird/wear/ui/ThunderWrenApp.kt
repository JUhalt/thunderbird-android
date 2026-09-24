package net.thunderbird.wear.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import net.thunderbird.wear.ui.navigation.ThunderWrenNavigation
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

/**
 * @param openMessageId A message to open, for example one tapped on the Tile. [onMessageOpen] is called once it
 * has been opened.
 */
@Composable
fun ThunderWrenApp(
    openMessageId: String? = null,
    onMessageOpen: () -> Unit = {},
) {
    ThunderWrenTheme {
        AppScaffold {
            ThunderWrenNavigation(openMessageId = openMessageId, onMessageOpen = onMessageOpen)
        }
    }
}
