package net.thunderbird.wear.ui.mailbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.common.accountColorIcon
import net.thunderbird.wear.ui.common.displayName
import net.thunderbird.wear.ui.preview.PreviewData
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun MailboxPickerScreen(
    state: MailboxPickerUiState,
    onMailboxClick: (String) -> Unit,
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
                    Text(text = stringResource(R.string.mailbox_picker_title))
                }
            }

            items(state.mailboxes, key = { it.id }) { mailbox ->
                MailboxButton(
                    mailbox = mailbox,
                    isSelected = mailbox.id == state.selectedMailboxId,
                    onClick = { onMailboxClick(mailbox.id) },
                )
            }
        }
    }
}

@Composable
private fun MailboxButton(
    mailbox: WearMailbox,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = if (isSelected) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
        icon = accountColorIcon(mailbox.color),
        secondaryLabel = {
            Text(
                text = listOfNotNull(
                    mailbox.email.takeIf { it.isNotEmpty() && it != mailbox.name },
                    stringResource(R.string.inbox_unread_count, mailbox.unreadCount),
                ).joinToString(" · "),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        label = { Text(text = mailbox.displayName(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun MailboxPickerScreenPreview() {
    ThunderWrenTheme {
        MailboxPickerScreen(
            state = MailboxPickerUiState(
                mailboxes = PreviewData.mailboxes,
                selectedMailboxId = PreviewData.mailboxes.first().id,
            ),
            onMailboxClick = {},
        )
    }
}
