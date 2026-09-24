package net.thunderbird.wear.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OpenOnPhoneDialog
import androidx.wear.compose.material3.OpenOnPhoneDialogDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.common.formatMessageDate
import net.thunderbird.wear.ui.preview.PreviewData
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun MessageDetailScreen(
    state: MessageUiState,
    actions: MessageActions,
    modifier: Modifier = Modifier,
) {
    // Start with the first item (the header) at the top instead of centered, so it isn't hidden under the clock.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)

    ScreenScaffold(
        scrollState = listState,
        modifier = modifier.fillMaxSize(),
    ) { contentPadding ->
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
        ) {
            val message = state.message
            when {
                state.isLoading -> item { Text(text = stringResource(R.string.inbox_loading)) }

                message == null -> item {
                    Text(text = stringResource(R.string.message_not_found), textAlign = TextAlign.Center)
                }

                else -> messageItems(message, state, actions)
            }
        }
    }

    val openOnPhoneText = OpenOnPhoneDialogDefaults.text
    val openOnPhoneTextStyle = OpenOnPhoneDialogDefaults.curvedTextStyle
    OpenOnPhoneDialog(
        visible = state.showOpenOnPhoneConfirmation,
        onDismissRequest = actions::onDismissOpenOnPhoneConfirmation,
        curvedText = { curvedText(text = openOnPhoneText, style = openOnPhoneTextStyle) },
    )
}

private fun ScalingLazyListScope.messageItems(
    message: WearMessageSummary,
    state: MessageUiState,
    actions: MessageActions,
) {
    item {
        ListHeader {
            Text(text = message.senderName, color = MaterialTheme.colorScheme.primary)
        }
    }

    item { MessageHeader(message) }

    item {
        Text(
            text = if (message.isEncrypted) stringResource(R.string.message_encrypted) else message.preview,
            style = MaterialTheme.typography.bodyMedium,
            // Round screens cut off the corners, so keep body text away from the edges.
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
        )
    }

    state.errorMessage?.let { errorMessage ->
        item {
            Text(
                text = stringResource(errorMessage),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
    }

    item {
        Button(
            onClick = actions::onOpenOnPhone,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.action_open_on_phone)) },
        )
    }
    item {
        ActionButton(
            label = if (message.isRead) R.string.action_mark_unread else R.string.action_mark_read,
            enabled = !state.isBusy,
            onClick = actions::onToggleRead,
        )
    }
    item {
        ActionButton(
            label = if (message.isStarred) R.string.action_unstar else R.string.action_star,
            enabled = !state.isBusy,
            onClick = actions::onToggleStar,
        )
    }
    item { ActionButton(label = R.string.action_archive, enabled = !state.isBusy, onClick = actions::onArchive) }
    item { ActionButton(label = R.string.action_delete, enabled = !state.isBusy, onClick = actions::onDelete) }
}

@Composable
private fun MessageHeader(message: WearMessageSummary) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Text(
            text = message.subject.ifEmpty { stringResource(R.string.message_no_subject) },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.senderAddress,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMessageDate(message.date),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionButton(label: Int, enabled: Boolean, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(label)) },
    )
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun MessageDetailScreenPreview() {
    ThunderWrenTheme {
        MessageDetailScreen(
            state = MessageUiState(isLoading = false, message = PreviewData.messages.first()),
            actions = PreviewData.noOpMessageActions,
        )
    }
}
