package net.thunderbird.wear.ui.folder

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text

data class MailFolder(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val unreadCount: Int = 0,
)

object DefaultFolders {
    val folders = listOf(
        MailFolder("unified", "Unified Inbox", "📥", unreadCount = 3),
        MailFolder("inbox", "Inbox", "📬", unreadCount = 2),
        MailFolder("starred", "Starred", "⭐", unreadCount = 1),
        MailFolder("sent", "Sent", "📤"),
        MailFolder("drafts", "Drafts", "📝"),
        MailFolder("archive", "Archive", "📦"),
        MailFolder("trash", "Trash", "🗑️"),
    )
}

@Composable
fun FolderDrawerSheet(
    onSelectFolder: (MailFolder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
    ) {
        item {
            ListHeader {
                Text(
                    text = "Folders & Accounts",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        items(DefaultFolders.folders) { folder ->
            Button(
                onClick = { onSelectFolder(folder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    text = "${folder.iconEmoji} ${folder.name}" + if (folder.unreadCount > 0) " (${folder.unreadCount})" else "",
                )
            }
        }
    }
}
