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
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import net.thunderbird.wear.R
import net.thunderbird.wear.data.MessageSwipeAction
import net.thunderbird.wear.data.WatchSettings
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

            swipeItems(swipeLeft = settings.swipeLeft, swipeRight = settings.swipeRight, onEvent = onEvent)

            messageItems(settings, onEvent)

            item {
                SettingSwitch(
                    checked = state.isDemoOn,
                    onCheckedChange = { onEvent(Event.DemoChanged(it)) },
                    label = R.string.settings_demo,
                    description = R.string.settings_demo_description,
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

private fun ScalingLazyListScope.messageItems(settings: WatchSettings, onEvent: (Event) -> Unit) {
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
}

private fun ScalingLazyListScope.swipeItems(
    swipeLeft: MessageSwipeAction,
    swipeRight: MessageSwipeAction?,
    onEvent: (Event) -> Unit,
) {
    item {
        ListHeader {
            Text(text = stringResource(R.string.settings_swipe_actions))
        }
    }
    item {
        SettingChoice(
            label = R.string.settings_swipe_left,
            value = swipeLeft.label,
            onClick = { onEvent(Event.SwipeLeftClicked) },
        )
    }
    item {
        SettingChoice(
            label = R.string.settings_swipe_right,
            value = swipeRight?.label ?: R.string.settings_swipe_go_back,
            onClick = { onEvent(Event.SwipeRightClicked) },
        )
    }
    // Swiping right on a message takes over the usual swipe to go back, so say how going back works then.
    if (swipeRight != null) {
        item {
            Text(
                text = stringResource(R.string.settings_swipe_back_from_edge),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A setting with a few values. Tapping it switches to the next one. */
@Composable
private fun SettingChoice(
    @StringRes label: Int,
    @StringRes value: Int,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(label)) },
        secondaryLabel = { Text(text = stringResource(value)) },
    )
}

@Composable
private fun SettingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    @StringRes label: Int,
    @StringRes description: Int? = null,
) {
    SwitchButton(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(text = stringResource(label)) },
        secondaryLabel = description?.let { { Text(text = stringResource(it)) } },
    )
}

@get:StringRes
private val MessageSwipeAction.label: Int
    get() = when (this) {
        MessageSwipeAction.ARCHIVE -> R.string.action_archive
        MessageSwipeAction.DELETE -> R.string.action_delete
    }

@Preview(device = "id:wearos_small_round", showSystemUi = true)
@Composable
private fun SettingsScreenPreview() {
    ThunderWrenTheme {
        SettingsScreen(
            state = State(appVersion = "0.1.0-beta8"),
            onEvent = {},
        )
    }
}
