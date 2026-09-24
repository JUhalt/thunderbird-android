package net.thunderbird.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import net.thunderbird.wear.ui.folder.FolderDrawerSheet
import net.thunderbird.wear.ui.inbox.InboxScreen
import net.thunderbird.wear.ui.inbox.InboxUiState
import net.thunderbird.wear.ui.inbox.InboxViewModel
import net.thunderbird.wear.ui.model.SampleEmailData
import net.thunderbird.wear.ui.reader.MessageDetailScreen
import net.thunderbird.wear.ui.reply.QuickReplySheet
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ThunderWrenNavigation(
    modifier: Modifier = Modifier,
) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = ThunderWrenRoutes.INBOX,
        modifier = modifier,
    ) {
        inboxDestination(navController)
        foldersDestination(navController)
        messageDetailDestination(navController)
        quickReplyDestination(navController)
    }
}

private val messageIdArguments = listOf(
    navArgument(ThunderWrenRoutes.ARG_MESSAGE_ID) { type = NavType.StringType },
)

private fun NavBackStackEntry.messageId(): String =
    arguments?.getString(ThunderWrenRoutes.ARG_MESSAGE_ID) ?: "1"

private fun NavGraphBuilder.inboxDestination(navController: NavHostController) {
    composable(ThunderWrenRoutes.INBOX) {
        val viewModel: InboxViewModel = koinViewModel()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        val headers = when (val state = uiState) {
            is InboxUiState.Success -> state.headers
            else -> SampleEmailData.sampleHeaders
        }

        InboxScreen(
            headers = headers,
            onEmailClick = { messageId ->
                navController.navigate(ThunderWrenRoutes.messageDetail(messageId))
            },
            onOpenFoldersClick = {
                navController.navigate(ThunderWrenRoutes.FOLDERS)
            },
            onRefreshClick = {
                viewModel.loadInbox()
            },
        )
    }
}

private fun NavGraphBuilder.foldersDestination(navController: NavHostController) {
    composable(ThunderWrenRoutes.FOLDERS) {
        FolderDrawerSheet(
            onSelectFolder = {
                navController.popBackStack()
            },
        )
    }
}

private fun NavGraphBuilder.messageDetailDestination(navController: NavHostController) {
    composable(
        route = ThunderWrenRoutes.MESSAGE_DETAIL,
        arguments = messageIdArguments,
    ) { backStackEntry ->
        val messageId = backStackEntry.messageId()
        val message = SampleEmailData.getSampleMessage(messageId)

        MessageDetailScreen(
            message = message,
            onArchiveClick = { navController.popBackStack() },
            onDeleteClick = { navController.popBackStack() },
            onReplyClick = { navController.navigate(ThunderWrenRoutes.quickReply(messageId)) },
        )
    }
}

private fun NavGraphBuilder.quickReplyDestination(navController: NavHostController) {
    composable(
        route = ThunderWrenRoutes.QUICK_REPLY,
        arguments = messageIdArguments,
    ) { backStackEntry ->
        val message = SampleEmailData.getSampleMessage(backStackEntry.messageId())

        QuickReplySheet(
            recipientName = message.header.senderName,
            // Sending is not implemented yet; return to the message.
            onSendReply = { navController.popBackStack() },
        )
    }
}
