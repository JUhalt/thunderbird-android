package net.thunderbird.wear.ui.reply

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ConfirmationDialogDefaults
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SuccessConfirmationDialog
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.curvedText
import net.thunderbird.feature.wear.companion.WearMessageSummary
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.preview.PreviewData
import net.thunderbird.wear.ui.reply.ReplyContract.Event
import net.thunderbird.wear.ui.reply.ReplyContract.State
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

/** Ready-made replies, offered like Wear OS offers them for messaging apps. */
private val QUICK_REPLIES = listOf(
    R.string.quick_reply_ok,
    R.string.quick_reply_thanks,
    R.string.quick_reply_sounds_good,
    R.string.quick_reply_on_my_way,
    R.string.quick_reply_later,
)

/**
 * Lets the user pick a ready-made reply or speak or type one, then review it before sending. Emails aren't chat
 * messages, so nothing is sent without that second tap.
 */
@Composable
fun ReplyScreen(
    state: State,
    onEvent: (Event) -> Unit,
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
            val draft = state.draft
            when {
                state.isLoading -> item { Text(text = stringResource(R.string.inbox_loading)) }

                message == null -> item {
                    Text(text = stringResource(R.string.message_not_found), textAlign = TextAlign.Center)
                }

                draft == null -> chooseReplyItems(message, state, onEvent)

                else -> reviewItems(draft, state, onEvent)
            }
        }
    }

    val sentText = stringResource(R.string.reply_sent)
    val sentTextStyle = ConfirmationDialogDefaults.curvedTextStyle
    SuccessConfirmationDialog(
        visible = state.isSent,
        onDismissRequest = { onEvent(Event.SentConfirmationDismissed) },
        curvedText = { curvedText(text = sentText, style = sentTextStyle) },
    )
}

private fun ScalingLazyListScope.chooseReplyItems(
    message: WearMessageSummary,
    state: State,
    onEvent: (Event) -> Unit,
) {
    item {
        ListHeader {
            Text(
                text = stringResource(R.string.reply_title, message.senderName),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    errorItem(state)

    item {
        Button(
            onClick = { onEvent(Event.SpeakOrTypeClicked) },
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(painterResource(R.drawable.ic_mic), contentDescription = null) },
            label = { Text(text = stringResource(R.string.reply_speak_or_type)) },
        )
    }

    items(QUICK_REPLIES) { quickReply ->
        val text = stringResource(quickReply)
        FilledTonalButton(
            onClick = { onEvent(Event.QuickReplyClicked(text)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = text) },
        )
    }

    item {
        Text(
            text = stringResource(R.string.reply_note),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private fun ScalingLazyListScope.reviewItems(
    draft: String,
    state: State,
    onEvent: (Event) -> Unit,
) {
    item {
        ListHeader {
            Text(text = stringResource(R.string.reply_review_title))
        }
    }

    item {
        Text(
            text = draft,
            style = MaterialTheme.typography.bodyLarge,
            // Round screens cut off the corners, so keep body text away from the edges.
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
        )
    }

    errorItem(state)

    item {
        Button(
            onClick = { onEvent(Event.SendClicked) },
            enabled = !state.isSending && !state.isSent,
            modifier = Modifier.fillMaxWidth(),
            icon = { Icon(painterResource(R.drawable.ic_send), contentDescription = null) },
            label = {
                Text(text = stringResource(if (state.isSending) R.string.reply_sending else R.string.reply_send))
            },
        )
    }

    item {
        FilledTonalButton(
            onClick = { onEvent(Event.ChangeClicked) },
            enabled = !state.isSending && !state.isSent,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(R.string.reply_change)) },
        )
    }
}

private fun ScalingLazyListScope.errorItem(state: State) {
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
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun ReplyScreenPreview() {
    ThunderWrenTheme {
        ReplyScreen(
            state = State(isLoading = false, message = PreviewData.messages.first()),
            onEvent = {},
        )
    }
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun ReplyScreenReviewPreview() {
    ThunderWrenTheme {
        ReplyScreen(
            state = State(
                isLoading = false,
                message = PreviewData.messages.first(),
                draft = "Sounds good, see you at 10:30.",
            ),
            onEvent = {},
        )
    }
}
