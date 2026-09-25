package net.thunderbird.wear.ui.inbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RevealValue
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwipeToRevealDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.rememberRevealState
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.common.AccountMonogram
import net.thunderbird.wear.ui.common.ColorDot
import net.thunderbird.wear.ui.common.accountIcon
import net.thunderbird.wear.ui.common.displayName
import net.thunderbird.wear.ui.common.formatMessageDate
import net.thunderbird.wear.ui.inbox.InboxContract.Event
import net.thunderbird.wear.ui.inbox.InboxContract.State
import net.thunderbird.wear.ui.preview.PreviewData
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun InboxScreen(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Start with the first item (the header) at the top instead of centered, so it isn't hidden under the clock.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    var confirmMarkAllRead by remember { mutableStateOf(false) }

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
                State.Loading -> item { Text(text = stringResource(R.string.inbox_loading)) }

                is State.NotConnected -> notConnectedItems(state, onEvent)

                is State.Content -> {
                    contentItems(state, onEvent, listState, onMarkAllReadClick = { confirmMarkAllRead = true })
                    if (state.isDemo) demoItems(onEvent)
                }
            }
        }
    }

    if (state is State.Content) {
        MarkAllReadDialog(
            visible = confirmMarkAllRead,
            mailbox = state.mailbox,
            onConfirm = {
                confirmMarkAllRead = false
                onEvent(Event.MarkAllReadConfirmed(state.mailbox.id))
            },
            onDismiss = { confirmMarkAllRead = false },
        )
    }
}

private fun ScalingLazyListScope.notConnectedItems(state: State.NotConnected, onEvent: (Event) -> Unit) {
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
        RefreshButton(
            isRefreshing = state.isRefreshing,
            label = R.string.retry,
            onClick = { onEvent(Event.RefreshClicked) },
        )
    }
    item {
        FilledTonalButton(
            onClick = { onEvent(Event.StartDemoClicked) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.demo_start)) },
        )
    }
}

private fun ScalingLazyListScope.demoItems(onEvent: (Event) -> Unit) {
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
            onClick = { onEvent(Event.ExitDemoClicked) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.demo_exit)) },
        )
    }
}

private fun ScalingLazyListScope.contentItems(
    state: State.Content,
    onEvent: (Event) -> Unit,
    listState: ScalingLazyListState,
    onMarkAllReadClick: () -> Unit,
) {
    item {
        MailboxHeader(state = state, onClick = { onEvent(Event.MailboxClicked) })
    }

    state.errorMessage?.let { errorMessage ->
        item {
            Text(
                text = stringResource(errorMessage),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (state.messages.isEmpty()) {
        item { Text(text = stringResource(R.string.inbox_empty)) }
    }

    items(state.messages, key = { it.id }) { message ->
        SwipeableMessageCard(
            message = message,
            account = state.accounts[message.accountId],
            listState = listState,
            onClick = { onEvent(Event.MessageClicked(message.id)) },
            onArchive = { onEvent(Event.ArchiveClicked(message.id)) },
            onDelete = { onEvent(Event.DeleteClicked(message.id)) },
        )
    }

    if (state.mailbox.unreadCount > 0) {
        item {
            FilledTonalButton(
                onClick = onMarkAllReadClick,
                enabled = !state.isMarkingAllRead,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                icon = { Icon(painterResource(R.drawable.ic_done_all), contentDescription = null) },
                label = {
                    val label = if (state.isMarkingAllRead) {
                        R.string.inbox_marking_all_read
                    } else {
                        R.string.inbox_mark_all_read
                    }
                    Text(text = stringResource(label), maxLines = 2)
                },
            )
        }
    }

    item {
        RefreshButton(
            isRefreshing = state.isRefreshing,
            label = R.string.inbox_refresh,
            onClick = { onEvent(Event.RefreshClicked) },
        )
    }
}

@Composable
private fun MailboxHeader(
    state: State.Content,
    onClick: () -> Unit,
) {
    val mailboxName = state.mailbox.displayName()
    val unreadCount = state.mailbox.unreadCount
    val unreadCountText = pluralStringResource(R.plurals.unread_count, unreadCount, unreadCount)
    val unreadText = if (state.isDemo) stringResource(R.string.demo_label, unreadCountText) else unreadCountText
    val switchDescription = stringResource(R.string.mailbox_switch_description, mailboxName)

    FilledTonalButton(
        onClick = onClick,
        enabled = state.canSwitchMailbox,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$switchDescription $unreadText" },
        icon = accountIcon(state.mailbox),
        secondaryLabel = { Text(text = unreadText, maxLines = 1) },
        label = { Text(text = mailboxName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

/** A message that can be swiped to the left to archive or delete it. */
@Composable
private fun SwipeableMessageCard(
    message: WearMessageSummary,
    account: WearMailbox?,
    listState: ScalingLazyListState,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    val revealState = rememberRevealState()
    val archiveLabel = stringResource(R.string.action_archive)
    val deleteLabel = stringResource(R.string.action_delete)

    // The revealed buttons are centered on the visible part of the card, so cover them again when the list scrolls.
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && revealState.currentValue != RevealValue.Covered) {
            revealState.animateTo(RevealValue.Covered)
        }
    }

    // A full swipe archives, which can be undone on the phone. The default colors would mark it as destructive.
    val archiveContainerColor = MaterialTheme.colorScheme.primaryContainer
    val archiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val deleteContainerColor = MaterialTheme.colorScheme.errorContainer
    val deleteContentColor = MaterialTheme.colorScheme.onErrorContainer

    SwipeToReveal(
        primaryAction = {
            PrimaryActionButton(
                onClick = onArchive,
                icon = { Icon(painterResource(R.drawable.ic_archive), contentDescription = archiveLabel) },
                text = { Text(text = archiveLabel) },
                modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                containerColor = archiveContainerColor,
                contentColor = archiveContentColor,
            )
        },
        onSwipePrimaryAction = onArchive,
        secondaryAction = {
            SecondaryActionButton(
                onClick = onDelete,
                icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = deleteLabel) },
                modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                containerColor = deleteContainerColor,
                contentColor = deleteContentColor,
            )
        },
        revealState = revealState,
    ) {
        MessageCard(
            message = message,
            account = account,
            onClick = onClick,
            // Swiping isn't possible with a screen reader, so offer the same actions there.
            modifier = Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction(archiveLabel) {
                        onArchive()
                        true
                    },
                    CustomAccessibilityAction(deleteLabel) {
                        onDelete()
                        true
                    },
                )
            },
        )
    }
}

@Composable
private fun MessageCard(
    message: WearMessageSummary,
    account: WearMailbox?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unreadDescription = stringResource(R.string.message_unread_indicator)
    val starredDescription = stringResource(R.string.message_starred_indicator)
    val accountDescription = account?.let { stringResource(R.string.message_account, it.name) }

    TitleCard(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = listOfNotNull(
                    unreadDescription.takeIf { !message.isRead },
                    starredDescription.takeIf { message.isStarred },
                    message.senderName,
                    message.subject,
                    formatMessageDate(message.date),
                    accountDescription,
                ).joinToString(". ")
            },
        title = {
            if (account != null) {
                AccountMonogram(account = account, size = 18.dp)
                Spacer(modifier = Modifier.width(6.dp))
            }
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
private fun MarkAllReadDialog(
    visible: Boolean,
    mailbox: WearMailbox,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = stringResource(R.string.inbox_mark_all_read_confirmation, mailbox.displayName())

    AlertDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        confirmButton = { AlertDialogDefaults.ConfirmButton(onClick = onConfirm) },
        icon = { Icon(painterResource(R.drawable.ic_done_all), contentDescription = null) },
        title = { Text(text = title, textAlign = TextAlign.Center) },
    )
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
            onEvent = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun InboxScreenNotConnectedPreview() {
    ThunderWrenTheme {
        InboxScreen(
            state = State.NotConnected(isRefreshing = false),
            onEvent = {},
        )
    }
}
