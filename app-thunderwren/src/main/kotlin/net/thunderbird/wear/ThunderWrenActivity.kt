package net.thunderbird.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import net.thunderbird.wear.ui.ThunderWrenApp

class ThunderWrenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThunderWrenApp()
        }
    }
}
