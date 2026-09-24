package net.thunderbird.feature.wear.companion.internal

import app.k9mail.legacy.mailstore.MessageDetailsAccessor
import app.k9mail.legacy.mailstore.MessageListChangedListener
import app.k9mail.legacy.mailstore.MessageListRepository
import app.k9mail.legacy.mailstore.MessageMapper
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.message.extractors.PreviewResult
import com.fsck.k9.mail.Address
import net.thunderbird.feature.wear.companion.WearCompanion
import net.thunderbird.feature.wear.companion.WearInboxSnapshot
import net.thunderbird.feature.wear.companion.WearMailbox
import net.thunderbird.feature.wear.companion.WearMailboxList
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.feature.wear.companion.WearResponse

internal class FakeWearDataLayer(var isPaired: Boolean = true) : WearDataLayer {
    val published = mutableListOf<Map<String, ByteArray>>()

    override suspend fun isWatchPaired(): Boolean = isPaired

    override suspend fun publish(items: Map<String, ByteArray>) {
        published += items
    }
}

internal class FakeMessageListRepository(
    private val messages: Map<String, List<MessageDetailsAccessor>> = emptyMap(),
) : MessageListRepository {
    val listeners = mutableListOf<MessageListChangedListener>()
    val queries = mutableListOf<Pair<String, List<String>>>()

    fun notifyChanged() = listeners.forEach { it.onMessageListChanged() }

    override fun addListener(listener: MessageListChangedListener) {
        listeners += listener
    }

    override fun addListener(accountUuid: String, listener: MessageListChangedListener) {
        listeners += listener
    }

    override fun removeListener(listener: MessageListChangedListener) {
        listeners -= listener
    }

    override fun notifyMessageListChanged(accountUuid: String) = notifyChanged()

    override fun <T> getMessages(
        accountUuid: String,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> {
        queries += accountUuid to selectionArgs.toList()
        return messages[accountUuid].orEmpty().map(messageMapper::map)
    }

    override fun <T> getThreadedMessages(
        accountUuid: String,
        selection: String,
        selectionArgs: Array<String>,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> = error("not used")

    override fun <T> getThread(
        accountUuid: String,
        threadId: Long,
        sortOrder: String,
        messageMapper: MessageMapper<T>,
    ): List<T> = error("not used")
}

internal class FakeWearInboxPublisher(var publishResult: Boolean = true) : WearInboxPublisher {
    var publishNowCount = 0
    var requestPublishCount = 0

    override suspend fun publishNow(): Boolean {
        publishNowCount++
        return publishResult
    }

    override fun requestPublish() {
        requestPublishCount++
    }
}

internal class FakeWearMessageActions(var response: WearResponse = WearResponse.Ok) : WearMessageActions {
    val performed = mutableListOf<Pair<MessageReference, WearMessageAction>>()

    override fun perform(reference: MessageReference, action: WearMessageAction): WearResponse {
        performed += reference to action
        return response
    }
}

internal class FakeWearMailboxActions(var response: WearResponse = WearResponse.Ok) : WearMailboxActions {
    val markedAllRead = mutableListOf<String>()

    override fun markAllRead(mailboxId: String): WearResponse {
        markedAllRead += mailboxId
        return response
    }
}

internal class FakeWearReplySender(var response: WearResponse = WearResponse.Ok) : WearReplySender {
    val replies = mutableListOf<Pair<MessageReference, String>>()

    override fun reply(reference: MessageReference, text: String): WearResponse {
        replies += reference to text
        return response
    }
}

internal fun publication(unreadCount: Int = 0, accountUuids: List<String> = listOf("account-1")): WearPublication {
    val mailboxIds = listOf(WearCompanion.UNIFIED_MAILBOX_ID) + accountUuids
    return WearPublication(
        mailboxes = WearMailboxList(
            generatedAt = 1,
            mailboxes = mailboxIds.map { id ->
                WearMailbox(id = id, name = id, email = "", color = null, unreadCount = unreadCount)
            },
        ),
        inboxes = mailboxIds.map { id ->
            WearInboxSnapshot(mailboxId = id, generatedAt = 1, unreadCount = unreadCount, messages = emptyList())
        },
    )
}

@Suppress("LongParameterList")
internal class FakeMessageDetailsAccessor(
    override val id: Long = 1,
    override val messageServerId: String = "uid-1",
    override val folderId: Long = 7,
    override val fromAddresses: List<Address> = listOf(Address("ada@example.com", "Ada")),
    override val toAddresses: List<Address> = emptyList(),
    override val ccAddresses: List<Address> = emptyList(),
    override val messageDate: Long = 1_000,
    override val internalDate: Long = 1_000,
    override val subject: String? = "Subject",
    override val preview: PreviewResult = PreviewResult.text("Preview"),
    override val isRead: Boolean = false,
    override val isStarred: Boolean = false,
    override val isAnswered: Boolean = false,
    override val isForwarded: Boolean = false,
    override val hasAttachments: Boolean = false,
    override val threadRoot: Long = 1,
    override val threadCount: Int = 1,
) : MessageDetailsAccessor
