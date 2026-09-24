package net.thunderbird.wear.ui

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import net.thunderbird.wear.ui.navigation.ThunderWrenNavigation
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun ThunderWrenApp() {
    ThunderWrenTheme {
        AppScaffold {
            ThunderWrenNavigation()
        }
    }
}
