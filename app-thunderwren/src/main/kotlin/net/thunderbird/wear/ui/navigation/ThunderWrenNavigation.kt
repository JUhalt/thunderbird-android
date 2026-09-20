package net.thunderbird.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import org.koin.compose.viewmodel.koinViewModel

object ThunderWrenRoutes {
    const val INBOX = "inbox"
    const val FOLDERS = "folders"
    const val MESSAGE_DETAIL = "message/{messageId}"

    fun messageDetail(messageId: String) = "message/$messageId"
}

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

        composable(ThunderWrenRoutes.FOLDERS) {
            FolderDrawerSheet(
                onSelectFolder = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = ThunderWrenRoutes.MESSAGE_DETAIL,
            arguments = listOf(
                navArgument("messageId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val messageId = backStackEntry.arguments?.getString("messageId") ?: "1"
            val message = SampleEmailData.getSampleMessage(messageId)

            MessageDetailScreen(
                message = message,
                onArchiveClick = { navController.popBackStack() },
                onDeleteClick = { navController.popBackStack() },
            )
        }
    }
}
