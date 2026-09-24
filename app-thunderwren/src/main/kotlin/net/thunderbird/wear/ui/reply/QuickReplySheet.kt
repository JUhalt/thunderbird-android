package net.thunderbird.wear.ui.reply

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun QuickReplySheet(
    recipientName: String,
    onSendReply: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()

    val voiceLauncher = rememberLauncherForActivityResult(
        contract = VoiceReplyContract(),
    ) { spokenText ->
        if (!spokenText.isNullOrEmpty()) {
            onSendReply(spokenText)
        }
    }

    ScreenScaffold(
        scrollState = listState,
        modifier = modifier.fillMaxSize(),
    ) { contentPadding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            item {
                ListHeader {
                    Text(
                        text = "Reply to $recipientName",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                Button(
                    onClick = { voiceLauncher.launch("Speak your reply") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    Text(text = "🎤 Voice Dictation")
                }
            }

            items(PresetQuickReplies.defaultReplies) { replyText ->
                Button(
                    onClick = { onSendReply(replyText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                ) {
                    Text(text = replyText)
                }
            }
        }
    }
}
