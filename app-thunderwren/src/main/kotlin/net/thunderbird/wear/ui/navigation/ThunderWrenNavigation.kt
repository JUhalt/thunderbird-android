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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.wear.R
import net.thunderbird.wear.ui.inbox.InboxActions
import net.thunderbird.wear.ui.inbox.InboxScreen
import net.thunderbird.wear.ui.inbox.InboxUiState
import net.thunderbird.wear.ui.inbox.InboxViewModel
import net.thunderbird.wear.ui.mailbox.MailboxPickerScreen
import net.thunderbird.wear.ui.mailbox.MailboxPickerViewModel
import net.thunderbird.wear.ui.reader.MessageActions
import net.thunderbird.wear.ui.reader.MessageDetailScreen
import net.thunderbird.wear.ui.reader.MessageViewModel
import net.thunderbird.wear.ui.reply.ReplyActions
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
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        InboxScreen(
            state = state,
            actions = object : InboxActions {
                override fun onMailboxClick() = navController.navigate(ThunderWrenRoutes.MAILBOXES)
                override fun onMessageClick(messageId: String) {
                    val mailboxId = (state as? InboxUiState.Content)?.mailbox?.id ?: WearCompanion.UNIFIED_MAILBOX_ID
                    navController.navigate(ThunderWrenRoutes.messageDetail(mailboxId, messageId))
                }
                override fun onArchive(messageId: String) = viewModel.archive(messageId)
                override fun onDelete(messageId: String) = viewModel.delete(messageId)
                override fun onMarkAllRead(mailboxId: String) = viewModel.markAllRead(mailboxId)
                override fun onRefresh() = viewModel.refresh()
                override fun onStartDemo() = viewModel.startDemo()
                override fun onExitDemo() = viewModel.exitDemo()
            },
        )
    }
}

private fun NavGraphBuilder.mailboxesDestination(navController: NavHostController) {
    composable(ThunderWrenRoutes.MAILBOXES) {
        val viewModel: MailboxPickerViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        MailboxPickerScreen(
            state = state,
            onMailboxClick = { mailboxId ->
                viewModel.select(mailboxId)
                navController.popBackStack()
            },
        )
    }
}

private fun NavGraphBuilder.messageDetailDestination(navController: NavHostController) {
    composable(route = ThunderWrenRoutes.MESSAGE_DETAIL, arguments = messageArguments) { backStackEntry ->
        val (mailboxId, messageId) = backStackEntry.messageArguments()
        val viewModel: MessageViewModel = koinViewModel { parametersOf(messageId, mailboxId) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.isClosed) {
            if (state.isClosed) navController.popBackStack()
        }

        MessageDetailScreen(
            state = state,
            actions = object : MessageActions {
                override fun onReply() = navController.navigate(ThunderWrenRoutes.reply(mailboxId, messageId))
                override fun onOpenOnPhone() = viewModel.openOnPhone()
                override fun onToggleRead() = viewModel.toggleRead()
                override fun onToggleStar() = viewModel.toggleStar()
                override fun onArchive() = viewModel.archive()
                override fun onDelete() = viewModel.delete()
                override fun onDismissOpenOnPhoneConfirmation() = viewModel.dismissOpenOnPhoneConfirmation()
            },
        )
    }
}

private fun NavGraphBuilder.replyDestination(navController: NavHostController) {
    composable(route = ThunderWrenRoutes.REPLY, arguments = messageArguments) { backStackEntry ->
        val (mailboxId, messageId) = backStackEntry.messageArguments()
        val viewModel: ReplyViewModel = koinViewModel { parametersOf(messageId, mailboxId) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val inputLabel = stringResource(R.string.action_reply)
        val inputLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            ReplyInput.getText(it.data)?.let(viewModel::setDraft)
        }

        ReplyScreen(
            state = state,
            actions = object : ReplyActions {
                override fun onSpeakOrType() {
                    try {
                        inputLauncher.launch(ReplyInput.createIntent(inputLabel))
                    } catch (_: ActivityNotFoundException) {
                        viewModel.showInputUnavailable()
                    }
                }
                override fun onQuickReply(text: String) = viewModel.setDraft(text)
                override fun onSend() = viewModel.send()
                override fun onChange() = viewModel.clearDraft()
                override fun onSent() {
                    navController.popBackStack()
                }
            },
        )
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
