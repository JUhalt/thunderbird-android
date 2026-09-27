package net.thunderbird.wear.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import net.thunderbird.wear.R
import net.thunderbird.wear.data.SwipeActions
import net.thunderbird.wear.ui.settings.SettingsContract.Event
import net.thunderbird.wear.ui.settings.SettingsContract.State
import net.thunderbird.wear.ui.theme.ThunderWrenTheme

@Composable
fun SettingsScreen(
    state: State,
    onEvent: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Start with the first item (the header) at the top instead of centered, so it isn't hidden under the clock.
    val listState = rememberScalingLazyListState(initialCenterItemIndex = 0)
    val settings = state.settings

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
                    Text(text = stringResource(R.string.settings_title))
                }
            }

            swipeActionItems(selected = settings.swipeActions, onEvent = onEvent)

            item {
                ListHeader {
                    Text(text = stringResource(R.string.settings_messages))
                }
            }
            item {
                SettingSwitch(
                    checked = settings.confirmDelete,
                    onCheckedChange = { onEvent(Event.ConfirmDeleteChanged(it)) },
                    label = R.string.settings_confirm_delete,
                )
            }
            item {
                SettingSwitch(
                    checked = settings.markAsReadWhenOpened,
                    onCheckedChange = { onEvent(Event.MarkAsReadWhenOpenedChanged(it)) },
                    label = R.string.settings_mark_as_read_when_opened,
                )
            }
            item {
                SettingSwitch(
                    checked = settings.showPreviews,
                    onCheckedChange = { onEvent(Event.ShowPreviewsChanged(it)) },
                    label = R.string.settings_show_previews,
                )
            }

            item {
                Text(
                    text = stringResource(R.string.settings_version, state.appVersion),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

private fun ScalingLazyListScope.swipeActionItems(selected: SwipeActions, onEvent: (Event) -> Unit) {
    item {
        ListHeader {
            Text(text = stringResource(R.string.settings_swipe_actions))
        }
    }

    items(SwipeActions.entries) { swipeActions ->
        RadioButton(
            selected = swipeActions == selected,
            onSelect = { onEvent(Event.SwipeActionsSelected(swipeActions)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(swipeActions.label)) },
            secondaryLabel = swipeActions.description?.let { description ->
                { Text(text = stringResource(description)) }
            },
        )
    }
}

@Composable
private fun SettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    @StringRes label: Int,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(label)) },
    )
}

@get:StringRes
private val SwipeActions.label: Int
    get() = when (this) {
        SwipeActions.LEFT_ARCHIVE -> R.string.settings_swipe_left_archive
        SwipeActions.LEFT_DELETE -> R.string.settings_swipe_left_delete
        SwipeActions.LEFT_ARCHIVE_RIGHT_DELETE -> R.string.settings_swipe_left_archive_right_delete
        SwipeActions.LEFT_DELETE_RIGHT_ARCHIVE -> R.string.settings_swipe_left_delete_right_archive
    }

/** Swiping in both directions takes over the usual swipe to go back, so say how going back works then. */
@get:StringRes
private val SwipeActions.description: Int?
    get() = when (this) {
        SwipeActions.LEFT_ARCHIVE, SwipeActions.LEFT_DELETE -> null

        SwipeActions.LEFT_ARCHIVE_RIGHT_DELETE,
        SwipeActions.LEFT_DELETE_RIGHT_ARCHIVE,
        -> R.string.settings_swipe_back_from_edge
    }

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    ThunderWrenTheme {
        SettingsScreen(
            state = State(appVersion = "0.1.0-beta5"),
            onEvent = {},
        )
    }
}
