package net.thunderbird.wear.provider

import android.R
import androidx.compose.runtime.Composable
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.core.ui.theme.api.ThemeProvider
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

class ThunderWrenThemeProvider : ThemeProvider {
    override val appThemeResourceId = R.style.Theme_DeviceDefault
    override val appLightThemeResourceId = R.style.Theme_DeviceDefault
    override val appDarkThemeResourceId = R.style.Theme_DeviceDefault
    override val dialogThemeResourceId = R.style.Theme_DeviceDefault_Dialog
    override val translucentDialogThemeResourceId = R.style.Theme_DeviceDefault_Dialog
}

class ThunderWrenFeatureThemeProvider : FeatureThemeProvider {
    @Composable
    override fun WithTheme(content: @Composable () -> Unit) {
        ThunderWrenTheme {
            content()
        }
    }

    @Composable
    override fun WithTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
        ThunderWrenTheme {
            content()
        }
    }
}
