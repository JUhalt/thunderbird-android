package net.thunderbird.wear.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import net.thunderbird.wear.crypto.PgpMessageHelper
import net.thunderbird.wear.ui.model.EmailMessage
import net.thunderbird.wear.ui.model.SampleEmailData
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun MessageDetailScreen(
    message: EmailMessage,
    onArchiveClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReplyClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()

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
                        text = message.header.senderName,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            item {
                MessageHeaderDetails(message = message)
            }

            item {
                MessageBody(body = message.body)
            }

            item {
                Button(
                    onClick = onReplyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(text = "🎤 Quick Reply")
                }
            }

            item {
                MessageActions(
                    onArchiveClick = onArchiveClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        }
    }
}

@Composable
private fun MessageHeaderDetails(
    message: EmailMessage,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
    ) {
        Text(
            text = message.header.subject,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        if (PgpMessageHelper.isPgpEncrypted(message.body)) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "🔒 OpenPGP Encrypted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = message.header.senderAddress,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = message.header.dateText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MessageBody(
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = {},
        enabled = false,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = PgpMessageHelper.formatPgpSummary(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(4.dp),
        )
    }
}

@Composable
private fun MessageActions(
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onArchiveClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = "Archive")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onDeleteClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(text = "Delete")
        }
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun MessageDetailScreenPreview() {
    ThunderWrenTheme {
        MessageDetailScreen(
            message = SampleEmailData.getSampleMessage("1"),
        )
    }
}
