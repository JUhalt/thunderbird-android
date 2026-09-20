package net.thunderbird.wear.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import net.thunderbird.wear.ui.inbox.InboxScreen
import net.thunderbird.wear.ui.model.SampleEmailData
import net.thunderbird.wear.ui.reader.MessageDetailScreen

object ThunderWrenRoutes {
    const val INBOX = "inbox"
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
            InboxScreen(
                headers = SampleEmailData.sampleHeaders,
                onEmailClick = { messageId ->
                    navController.navigate(ThunderWrenRoutes.messageDetail(messageId))
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
