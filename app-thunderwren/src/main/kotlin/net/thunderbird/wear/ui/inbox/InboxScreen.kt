package net.thunderbird.wear.ui.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.common.ColorDot
import net.thunderbird.wear.ui.common.accountColorIcon
import net.thunderbird.wear.ui.common.displayName
import net.thunderbird.wear.ui.common.formatMessageDate
import net.thunderbird.wear.ui.preview.PreviewData
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun InboxScreen(
    state: InboxUiState,
    onMailboxClick: () -> Unit,
    onMessageClick: (String) -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier,
    onStartDemoClick: () -> Unit = {},
    onExitDemoClick: () -> Unit = {},
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
            when (state) {
                InboxUiState.Loading -> item { Text(text = stringResource(R.string.inbox_loading)) }

                is InboxUiState.NotConnected -> notConnectedItems(state, onRefreshClick, onStartDemoClick)

                is InboxUiState.Content -> {
                    contentItems(state, onMailboxClick, onMessageClick, onRefreshClick)
                    if (state.isDemo) demoItems(onExitDemoClick)
                }
            }
        }
    }
}

private fun ScalingLazyListScope.notConnectedItems(
    state: InboxUiState.NotConnected,
    onRefreshClick: () -> Unit,
    onStartDemoClick: () -> Unit,
) {
    item {
        ListHeader {
            Text(text = stringResource(R.string.not_connected_title))
        }
    }
    item {
        Text(
            text = stringResource(R.string.not_connected_text),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        RefreshButton(isRefreshing = state.isRefreshing, label = R.string.retry, onClick = onRefreshClick)
    }
    item {
        FilledTonalButton(
            onClick = onStartDemoClick,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.demo_start)) },
        )
    }
}

private fun ScalingLazyListScope.demoItems(onExitDemoClick: () -> Unit) {
    item {
        Text(
            text = stringResource(R.string.demo_explanation),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    item {
        FilledTonalButton(
            onClick = onExitDemoClick,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.demo_exit)) },
        )
    }
}

private fun ScalingLazyListScope.contentItems(
    state: InboxUiState.Content,
    onMailboxClick: () -> Unit,
    onMessageClick: (String) -> Unit,
    onRefreshClick: () -> Unit,
) {
    item {
        MailboxHeader(state = state, onClick = onMailboxClick)
    }

    if (state.messages.isEmpty()) {
        item { Text(text = stringResource(R.string.inbox_empty)) }
    }

    items(state.messages, key = { it.id }) { message ->
        MessageCard(message = message, onClick = { onMessageClick(message.id) })
    }

    item {
        RefreshButton(isRefreshing = state.isRefreshing, label = R.string.inbox_refresh, onClick = onRefreshClick)
    }
}

@Composable
private fun MailboxHeader(
    state: InboxUiState.Content,
    onClick: () -> Unit,
) {
    val mailboxName = state.mailbox.displayName()
    val unreadCountText = stringResource(R.string.inbox_unread_count, state.mailbox.unreadCount)
    val unreadText = if (state.isDemo) stringResource(R.string.demo_label, unreadCountText) else unreadCountText
    val switchDescription = stringResource(R.string.mailbox_switch_description, mailboxName)

    FilledTonalButton(
        onClick = onClick,
        enabled = state.canSwitchMailbox,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$switchDescription $unreadText" },
        icon = accountColorIcon(state.mailbox.color),
        secondaryLabel = { Text(text = unreadText, maxLines = 1) },
        label = { Text(text = mailboxName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
private fun MessageCard(
    message: WearMessageSummary,
    onClick: () -> Unit,
) {
    val unreadDescription = stringResource(R.string.message_unread_indicator)
    val starredDescription = stringResource(R.string.message_starred_indicator)

    TitleCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(
                    unreadDescription.takeIf { !message.isRead },
                    starredDescription.takeIf { message.isStarred },
                    message.senderName,
                    message.subject,
                    formatMessageDate(message.date),
                ).joinToString(". ")
            },
        title = {
            if (!message.isRead) {
                ColorDot(MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = message.senderName,
                fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        time = { Text(text = formatMessageDate(message.date)) },
    ) {
        Column {
            Text(
                text = message.subject.ifEmpty { stringResource(R.string.message_no_subject) },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (message.isRead) FontWeight.Normal else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (message.isEncrypted) stringResource(R.string.message_encrypted) else message.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RefreshButton(
    isRefreshing: Boolean,
    label: Int,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = !isRefreshing,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        label = {
            Text(text = stringResource(if (isRefreshing) R.string.inbox_refreshing else label))
        },
    )
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun InboxScreenPreview() {
    ThunderWrenTheme {
        InboxScreen(
            state = PreviewData.inboxContent,
            onMailboxClick = {},
            onMessageClick = {},
            onRefreshClick = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun InboxScreenNotConnectedPreview() {
    ThunderWrenTheme {
        InboxScreen(
            state = InboxUiState.NotConnected(isRefreshing = false),
            onMailboxClick = {},
            onMessageClick = {},
            onRefreshClick = {},
        )
    }
}
