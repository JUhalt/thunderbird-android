package net.thunderbird.wear.ui.folders

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import net.thunderbird.feature.wear.companion.WearFolder
import net.thunderbird.feature.wear.companion.WearFolderType
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.folders.FolderListContract.Event
import net.thunderbird.wear.ui.folders.FolderListContract.State
import net.thunderbird.wear.ui.preview.PreviewData
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun FolderListScreen(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Start with the first item (the header) at the top instead of centered, so it isn't hidden under the clock.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val folders = state.folders

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
                        text = state.account?.name ?: stringResource(R.string.mailbox_picker_folders),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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

            when {
                folders == null && state.isLoading -> item { Text(text = stringResource(R.string.inbox_loading)) }

                folders == null -> item {
                    Button(
                        onClick = { onEvent(Event.RetryClicked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(text = stringResource(R.string.retry)) },
                    )
                }

                folders.isEmpty() -> item { Text(text = stringResource(R.string.folders_empty)) }

                else -> items(folders, key = { it.id }) { folder ->
                    FolderButton(folder = folder, onClick = { onEvent(Event.FolderClicked(folder.id)) })
                }
            }
        }
    }
}

@Composable
private fun FolderButton(folder: WearFolder, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        icon = { Icon(painterResource(folder.type.icon), contentDescription = null) },
        label = { Text(text = folder.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = if (folder.unreadCount > 0) {
            {
                Text(
                    text = pluralStringResource(R.plurals.unread_count, folder.unreadCount, folder.unreadCount),
                    maxLines = 1,
                )
            }
        } else {
            null
        },
    )
}

@get:DrawableRes
private val WearFolderType.icon: Int
    get() = when (this) {
        WearFolderType.INBOX -> R.drawable.ic_inbox
        WearFolderType.SENT -> R.drawable.ic_send
        WearFolderType.ARCHIVE -> R.drawable.ic_archive
        WearFolderType.SPAM -> R.drawable.ic_report
        WearFolderType.TRASH -> R.drawable.ic_delete
        WearFolderType.REGULAR -> R.drawable.ic_folder
    }

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun FolderListScreenPreview() {
    ThunderWrenTheme {
        FolderListScreen(
            state = State(account = PreviewData.mailboxes.last(), folders = PreviewData.folders),
            onEvent = {},
        )
    }
}
