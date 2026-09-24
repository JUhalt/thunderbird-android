package net.thunderbird.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import net.thunderbird.wear.ui.inbox.InboxScreen
import net.thunderbird.wear.ui.inbox.InboxViewModel
import net.thunderbird.wear.ui.mailbox.MailboxPickerScreen
import net.thunderbird.wear.ui.mailbox.MailboxPickerViewModel
import net.thunderbird.wear.ui.reader.MessageActions
import net.thunderbird.wear.ui.reader.MessageDetailScreen
import net.thunderbird.wear.ui.reader.MessageViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

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
        mailboxesDestination(navController)
        messageDetailDestination(navController)
    }
}

private fun NavGraphBuilder.inboxDestination(navController: NavHostController) {
    composable(ThunderWrenRoutes.INBOX) {
        val viewModel: InboxViewModel = koinViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        InboxScreen(
            state = state,
            onMailboxClick = { navController.navigate(ThunderWrenRoutes.MAILBOXES) },
            onMessageClick = { messageId -> navController.navigate(ThunderWrenRoutes.messageDetail(messageId)) },
            onRefreshClick = viewModel::refresh,
            onStartDemoClick = viewModel::startDemo,
            onExitDemoClick = viewModel::exitDemo,
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
    composable(
        route = ThunderWrenRoutes.MESSAGE_DETAIL,
        arguments = listOf(navArgument(ThunderWrenRoutes.ARG_MESSAGE_ID) { type = NavType.StringType }),
    ) { backStackEntry ->
        val messageId = backStackEntry.arguments?.getString(ThunderWrenRoutes.ARG_MESSAGE_ID).orEmpty()
        val viewModel: MessageViewModel = koinViewModel { parametersOf(messageId) }
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.isClosed) {
            if (state.isClosed) navController.popBackStack()
        }

        MessageDetailScreen(
            state = state,
            actions = object : MessageActions {
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
