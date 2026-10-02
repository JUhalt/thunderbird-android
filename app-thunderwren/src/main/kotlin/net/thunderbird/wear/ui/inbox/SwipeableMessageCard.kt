package net.thunderbird.wear.ui.inbox

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.RevealDirection
import androidx.wear.compose.material3.RevealState
import androidx.wear.compose.material3.RevealValue
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwipeToRevealDefaults
import androidx.wear.compose.material3.SwipeToRevealScope
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.rememberRevealState
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import net.thunderbird.wear.R
import net.thunderbird.wear.data.MessageSwipeAction

/** What a swipe does to a message. */
private class SwipeAction(
    val label: String,
    @DrawableRes val icon: Int,
    val isDestructive: Boolean,
    val onAction: () -> Unit,
)

/**
 * A message that can be swiped to archive or delete it, as chosen in the settings. A full swipe to the left does
 * [swipeLeft]. If [swipeRight] is `null`, swiping right goes back as usual and the other action is offered next to
 * [swipeLeft]; otherwise swiping right does [swipeRight].
 *
 * [content] gets a modifier that offers both actions to screen readers, which can't swipe.
 */
@Composable
fun SwipeableMessageCard(
    swipeLeft: MessageSwipeAction,
    swipeRight: MessageSwipeAction?,
    confirmDelete: Boolean,
    listState: ScalingLazyListState,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable (accessibilityActions: Modifier) -> Unit,
) {
    val revealState = rememberRevealState()
    val coroutineScope = rememberCoroutineScope()

    // The revealed buttons are centered on the visible part of the card, so cover them again when the list scrolls.
    // Reading the scroll state in a snapshotFlow, not in composition, keeps the cards from recomposing on every scroll.
    LaunchedEffect(revealState, listState) {
        snapshotFlow { listState.isScrollInProgress }
            .filter { it }
            .collect { if (revealState.currentValue != RevealValue.Covered) revealState.animateTo(RevealValue.Covered) }
    }

    val archive = SwipeAction(stringResource(R.string.action_archive), R.drawable.ic_archive, false, onArchive)
    val delete = SwipeAction(stringResource(R.string.action_delete), R.drawable.ic_delete, true) {
        onDelete()
        // The message stays until the deletion is confirmed, so show it again instead of the swiped-away card.
        if (confirmDelete) coroutineScope.launch { revealState.animateTo(RevealValue.Covered) }
    }

    fun action(swipeAction: MessageSwipeAction) = when (swipeAction) {
        MessageSwipeAction.ARCHIVE -> archive
        MessageSwipeAction.DELETE -> delete
    }

    val leftSwipe = action(swipeLeft)
    // Either the swipe to the right, or the second button next to the left swipe's.
    val otherAction = swipeRight?.let(::action) ?: if (leftSwipe === archive) delete else archive
    val accessibilityActions = Modifier.semantics {
        customActions = listOf(archive, delete).map { action ->
            CustomAccessibilityAction(action.label) {
                action.onAction()
                true
            }
        }
    }

    val isBidirectional = swipeRight != null
    fun currentAction() = if (isBidirectional && revealState.isSwipingRight) otherAction else leftSwipe

    SwipeToReveal(
        primaryAction = { PrimaryButton(currentAction()) },
        onSwipePrimaryAction = { currentAction().onAction() },
        // Swiping in both directions has one action per side, so there's no second button.
        secondaryAction = if (isBidirectional) null else ({ SecondaryButton(otherAction) }),
        revealState = revealState,
        revealDirection = if (isBidirectional) RevealDirection.Bidirectional else RevealDirection.RightToLeft,
        // In both directions, the screen's left edge is left to the system, so swiping from there still goes back.
        gestureInclusion = if (isBidirectional) {
            SwipeToRevealDefaults.bidirectionalGestureInclusion
        } else {
            SwipeToRevealDefaults.gestureInclusion(revealState)
        },
        content = { content(accessibilityActions) },
    )
}

/** Swiping to the right reveals the actions on the left side of the card. */
private val RevealState.isSwipingRight: Boolean
    get() = offset > 0f || currentValue == RevealValue.LeftRevealed || targetValue == RevealValue.LeftRevealed

@Composable
private fun SwipeToRevealScope.PrimaryButton(action: SwipeAction) {
    PrimaryActionButton(
        onClick = action.onAction,
        icon = { Icon(painterResource(action.icon), contentDescription = action.label) },
        text = { Text(text = action.label) },
        modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
        containerColor = action.containerColor(),
        contentColor = action.contentColor(),
    )
}

@Composable
private fun SwipeToRevealScope.SecondaryButton(action: SwipeAction) {
    SecondaryActionButton(
        onClick = action.onAction,
        icon = { Icon(painterResource(action.icon), contentDescription = action.label) },
        modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
        containerColor = action.containerColor(),
        contentColor = action.contentColor(),
    )
}

// Archiving can be undone on the phone, so only deleting uses the colors that mark an action as destructive.
@Composable
private fun SwipeAction.containerColor(): Color = with(MaterialTheme.colorScheme) {
    if (isDestructive) errorContainer else primaryContainer
}

@Composable
private fun SwipeAction.contentColor(): Color = with(MaterialTheme.colorScheme) {
    if (isDestructive) onErrorContainer else onPrimaryContainer
}
