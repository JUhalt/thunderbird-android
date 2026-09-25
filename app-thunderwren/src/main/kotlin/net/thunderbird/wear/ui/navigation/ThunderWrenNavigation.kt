package net.thunderbird.wear.ui.navigation

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import net.thunderbird.core.ui.contract.mvi.observe
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.inbox.InboxContract
import net.thunderbird.wear.ui.inbox.InboxScreen
import net.thunderbird.wear.ui.inbox.InboxViewModel
import net.thunderbird.wear.ui.mailbox.MailboxPickerContract
import net.thunderbird.wear.ui.mailbox.MailboxPickerScreen
import net.thunderbird.wear.ui.mailbox.MailboxPickerViewModel
import net.thunderbird.wear.ui.reader.MessageContract
import net.thunderbird.wear.ui.reader.MessageDetailScreen
import net.thunderbird.wear.ui.reader.MessageViewModel
import net.thunderbird.wear.ui.reply.ReplyContract
import net.thunderbird.wear.ui.reply.ReplyInput
import net.thunderbird.wear.ui.reply.ReplyScreen
import net.thunderbird.wear.ui.reply.ReplyViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * @param openMessageId A message from the unified inbox's unread messages to open, for example one tapped on the
 * Tile. [onMessageOpen] is called once it has been opened.
 */
@Composable
fun ThunderWrenNavigation(
    modifier: Modifier = Modifier,
    openMessageId: String? = null,
    onMessageOpen: () -> Unit = {},
) {
    val navController = rememberSwipeDismissableNavController()
    val currentOnMessageOpen by rememberUpdatedState(onMessageOpen)

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ThunderWrenRoutes.INBOX,
        modifier = modifier,
    ) {
        inboxDestination(navController)
        mailboxesDestination(navController)
        messageDetailDestination(navController)
        replyDestination(navController)
    }

    LaunchedEffect(openMessageId) {
        if (openMessageId != null) {
            navController.navigate(ThunderWrenRoutes.messageDetail(WearCompanion.UNREAD_MAILBOX_ID, openMessageId)) {
                // Going back from the message leads to the inbox.
                popUpTo(ThunderWrenRoutes.INBOX)
            }
            currentOnMessageOpen()
        }
    }
}

private fun NavGraphBuilder.inboxDestination(navController: NavHostController) {
    composable(ThunderWrenRoutes.INBOX) {
        val viewModel: InboxViewModel = koinViewModel()
        val (state, dispatch) = viewModel.observe { effect ->
            when (effect) {
                InboxContract.Effect.OpenMailboxes -> navController.navigate(ThunderWrenRoutes.MAILBOXES)

                is InboxContract.Effect.OpenMessage -> {
                    navController.navigate(ThunderWrenRoutes.messageDetail(effect.mailboxId, effect.messageId))
                }
            }
        }

        InboxScreen(state = state.value, onEvent = dispatch)
    }
}

private fun NavGraphBuilder.mailboxesDestination(navController: NavHostController) {
    composable(ThunderWrenRoutes.MAILBOXES) {
        val viewModel: MailboxPickerViewModel = koinViewModel()
        val (state, dispatch) = viewModel.observe { effect ->
            when (effect) {
                MailboxPickerContract.Effect.Close -> navController.popBackStack()
            }
        }

        MailboxPickerScreen(state = state.value, onEvent = dispatch)
    }
}

private fun NavGraphBuilder.messageDetailDestination(navController: NavHostController) {
    composable(route = ThunderWrenRoutes.MESSAGE_DETAIL, arguments = messageArguments) { backStackEntry ->
        val (mailboxId, messageId) = backStackEntry.messageArguments()
        val viewModel: MessageViewModel = koinViewModel { parametersOf(messageId, mailboxId) }
        val (state, dispatch) = viewModel.observe { effect ->
            when (effect) {
                is MessageContract.Effect.OpenReply -> {
                    navController.navigate(ThunderWrenRoutes.reply(effect.mailboxId, effect.messageId))
                }

                MessageContract.Effect.Close -> navController.popBackStack()
            }
        }

        MessageDetailScreen(state = state.value, onEvent = dispatch)
    }
}

private fun NavGraphBuilder.replyDestination(navController: NavHostController) {
    composable(route = ThunderWrenRoutes.REPLY, arguments = messageArguments) { backStackEntry ->
        val (mailboxId, messageId) = backStackEntry.messageArguments()
        val viewModel: ReplyViewModel = koinViewModel { parametersOf(messageId, mailboxId) }
        val inputLabel = stringResource(R.string.action_reply)
        val inputLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            viewModel.event(ReplyContract.Event.ReplyInputReceived(ReplyInput.getText(it.data)))
        }
        val (state, dispatch) = viewModel.observe { effect ->
            when (effect) {
                ReplyContract.Effect.OpenReplyInput -> {
                    try {
                        inputLauncher.launch(ReplyInput.createIntent(inputLabel))
                    } catch (_: ActivityNotFoundException) {
                        viewModel.event(ReplyContract.Event.ReplyInputUnavailable)
                    }
                }

                ReplyContract.Effect.Close -> navController.popBackStack()
            }
        }

        ReplyScreen(state = state.value, onEvent = dispatch)
    }
}

private val messageArguments = listOf(
    navArgument(ThunderWrenRoutes.ARG_MAILBOX_ID) { type = NavType.StringType },
    navArgument(ThunderWrenRoutes.ARG_MESSAGE_ID) { type = NavType.StringType },
)

/** The mailbox ID and message ID of a message route. */
private fun NavBackStackEntry.messageArguments(): Pair<String, String> {
    val mailboxId = arguments?.getString(ThunderWrenRoutes.ARG_MAILBOX_ID) ?: WearCompanion.UNIFIED_MAILBOX_ID
    val messageId = arguments?.getString(ThunderWrenRoutes.ARG_MESSAGE_ID).orEmpty()
    return mailboxId to messageId
}
