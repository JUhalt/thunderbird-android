package net.thunderbird.wear

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import net.thunderbird.wear.ui.ThunderWrenApp

class ThunderWrenActivity : ComponentActivity() {
    private var openMessageId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // After a configuration change the message is open already.
        if (savedInstanceState == null) {
            openMessageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
        }

        setContent {
            ThunderWrenApp(
                openMessageId = openMessageId,
                onMessageOpen = { openMessageId = null },
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openMessageId = intent.getStringExtra(EXTRA_MESSAGE_ID)
    }

    companion object {
        /** A message to open, from the unified inbox's unread messages. */
        const val EXTRA_MESSAGE_ID = "net.thunderbird.wear.extra.MESSAGE_ID"

        fun createIntent(context: Context, messageId: String? = null): Intent {
            return Intent(context, ThunderWrenActivity::class.java).putExtra(EXTRA_MESSAGE_ID, messageId)
        }
    }
}
