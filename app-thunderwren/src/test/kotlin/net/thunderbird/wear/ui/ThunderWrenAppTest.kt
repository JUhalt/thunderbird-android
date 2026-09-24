package net.thunderbird.wear.ui

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import net.thunderbird.feature.wear.companion.WearMessageAction
import net.thunderbird.wear.ThunderWrenApplication
import net.thunderbird.wear.data.DemoAwarePhoneConnection
import net.thunderbird.wear.data.DemoModeStore
import net.thunderbird.wear.data.DemoPhoneConnection
import net.thunderbird.wear.data.PhoneConnection
import net.thunderbird.wear.data.SelectedMailboxStore
import net.thunderbird.wear.testing.FakeDemoModeStore
import net.thunderbird.wear.testing.FakePhoneConnection
import net.thunderbird.wear.testing.FakeSelectedMailboxStore
import net.thunderbird.wear.testing.STARRED
import net.thunderbird.wear.testing.UNIFIED
import net.thunderbird.wear.testing.UNREAD
import net.thunderbird.wear.testing.mailbox
import net.thunderbird.wear.testing.message
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

/** Drives the real watch UI with a fake phone. The tall screen keeps every list item composed. */
@RunWith(AndroidJUnit4::class)
@Config(application = ThunderWrenApplication::class, sdk = [33], qualifiers = "w320dp-h900dp-round-watch")
class ThunderWrenAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val phone = FakePhoneConnection()

    @Before
    fun setUp() {
        loadKoinModules(
            module {
                single<PhoneConnection> { phone }
                single<SelectedMailboxStore> { FakeSelectedMailboxStore() }
                single<DemoModeStore> { FakeDemoModeStore() }
            },
        )
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `explains how to connect before the phone has published`() {
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Connect your phone").assertIsDisplayed()
    }

    @Test
    fun `demo mailbox can be tried without a phone and is replaced by real data`() {
        val demoModeStore = FakeDemoModeStore()
        loadKoinModules(
            module {
                single<PhoneConnection> {
                    DemoAwarePhoneConnection(
                        phone = phone,
                        demo = DemoPhoneConnection(
                            getString = ApplicationProvider.getApplicationContext<Context>()::getString,
                        ),
                        demoModeStore = demoModeStore,
                    )
                }
                single<DemoModeStore> { demoModeStore }
            },
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Try the demo").performClick()

        composeRule.onNodeWithText("Welcome to the demo mailbox").assertIsDisplayed()

        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(UNIFIED to listOf(message("real", senderName = "Real Sender"))),
        )

        composeRule.onNodeWithText("Real Sender").assertIsDisplayed()
        composeRule.onNodeWithText("Welcome to the demo mailbox").assertDoesNotExist()
    }

    @Test
    fun `opens a message and archives it`() {
        // Real message IDs are MessageReference identity strings with characters that need encoding in routes.
        val messageId = "#:YWNj+b3VudA==:MQ==:dW/lk"
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(
                UNIFIED to listOf(message(messageId, senderName = "Ada", subject = "Engine notes", isRead = true)),
            ),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Ada").performClick()
        composeRule.onNodeWithText("Engine notes").assertIsDisplayed()
        composeRule.onNodeWithText("Archive").performClick()

        composeRule.waitUntil { phone.performedActions.isNotEmpty() }
        assertThat(phone.performedActions).containsExactly(messageId to WearMessageAction.ARCHIVE)
        composeRule.onNodeWithText("All inboxes").assertIsDisplayed()
    }

    @Test
    fun `switches between the unified inbox and a single account`() {
        phone.publish(
            mailboxes = listOf(
                mailbox(UNIFIED, unreadCount = 2),
                mailbox("work", name = "Work"),
                mailbox("home", name = "Home"),
            ),
            inboxes = mapOf(
                UNIFIED to listOf(message("w1", senderName = "Ada"), message("h1", senderName = "Charles")),
                "work" to listOf(message("w1", senderName = "Ada")),
                "home" to listOf(message("h1", senderName = "Charles")),
            ),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("All inboxes").assertIsDisplayed()
        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Charles").assertIsDisplayed()

        composeRule.onNodeWithText("All inboxes").performClick()
        composeRule.onNodeWithText("Home").performClick()

        composeRule.onNodeWithText("Home").assertIsDisplayed()
        composeRule.onNodeWithText("Charles").assertIsDisplayed()
        composeRule.onNodeWithText("Ada").assertDoesNotExist()

        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("All inboxes").performClick()

        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Charles").assertIsDisplayed()
    }

    @Test
    fun `shows only unread messages in the unread view`() {
        phone.publish(
            mailboxes = listOf(
                mailbox(UNIFIED, unreadCount = 1),
                mailbox(UNREAD, unreadCount = 1),
                mailbox(STARRED),
                mailbox("work", name = "Work"),
                mailbox("home", name = "Home"),
            ),
            inboxes = mapOf(
                UNIFIED to
                    listOf(message("m1", senderName = "Ada"), message("m2", senderName = "Charles", isRead = true)),
                UNREAD to listOf(message("m1", senderName = "Ada")),
            ),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("All inboxes").performClick()
        composeRule.onNodeWithText("Accounts").assertIsDisplayed()
        composeRule.onNodeWithText("Unread").performClick()

        composeRule.onNodeWithText("Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Charles").assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `archives a message from the inbox without opening it`() {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", senderName = "Ada", isRead = true))),
        )
        composeRule.setContent { ThunderWrenApp() }

        // What swiping the card to the left offers, as screen readers get it.
        composeRule.onNodeWithText("Ada").performCustomAccessibilityActionWithLabel("Archive")

        composeRule.waitUntil { phone.performedActions.isNotEmpty() }
        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.ARCHIVE)
        composeRule.onNodeWithText("Ada").assertDoesNotExist()
    }

    @Test
    fun `swiping a message all the way to the left archives it`() {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", senderName = "Ada", isRead = true))),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Ada").performTouchInput {
            swipeLeft(startX = right - 1f, endX = left, durationMillis = 300)
        }

        composeRule.waitUntil { phone.performedActions.isNotEmpty() }
        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.ARCHIVE)
    }

    @Test
    fun `marks everything as read after confirming`() {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED, unreadCount = 1)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", senderName = "Ada"))),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Mark all as read").performClick()
        composeRule.onNodeWithText("Mark everything in All inboxes as read?").assertIsDisplayed()
        assertThat(phone.markedAllRead).isEmpty()
        composeRule.onNodeWithContentDescription("Confirm").performClick()

        composeRule.waitUntil { phone.markedAllRead.isNotEmpty() }
        assertThat(phone.markedAllRead).containsExactly(UNIFIED)
    }

    @Test
    fun `replies with a ready-made reply after reviewing it`() {
        val messageId = "#:YWNj+b3VudA==:MQ==:dW/lk"
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(
                UNIFIED to listOf(message(messageId, senderName = "Ada", subject = "Lunch?", isRead = true)),
            ),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Ada").performClick()
        composeRule.onNodeWithText("Reply").performClick()
        composeRule.onNodeWithText("Reply to Ada").assertIsDisplayed()
        composeRule.onNodeWithText("Thanks!").performClick()
        composeRule.onNodeWithText("Your reply").assertIsDisplayed()
        assertThat(phone.replies).isEmpty()
        composeRule.onNodeWithText("Send").performClick()

        composeRule.waitUntil { phone.replies.isNotEmpty() }
        assertThat(phone.replies).containsExactly(messageId to "Thanks!")
        // The "sent" confirmation closes the reply screen, back to the message.
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText("Your reply").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Open on phone").assertIsDisplayed()
    }

    @Test
    fun `encrypted messages offer only open on phone`() {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED)),
            inboxes = mapOf(UNIFIED to listOf(message("m1", senderName = "Ada", isRead = true, isEncrypted = true))),
        )
        composeRule.setContent { ThunderWrenApp() }

        composeRule.onNodeWithText("Ada").performClick()

        composeRule.onNodeWithText("Open on phone").assertIsDisplayed()
        composeRule.onNodeWithText("Reply").assertDoesNotExist()
    }

    @Test
    fun `opens the message tapped on the Tile`() {
        phone.publish(
            mailboxes = listOf(mailbox(UNIFIED, unreadCount = 1), mailbox(UNREAD, unreadCount = 1)),
            inboxes = mapOf(
                UNIFIED to listOf(message("m1", senderName = "Ada")),
                UNREAD to listOf(message("m1", senderName = "Ada")),
            ),
        )
        composeRule.setContent { ThunderWrenApp(openMessageId = "m1") }

        composeRule.onNodeWithText("Reply").assertIsDisplayed()
        composeRule.waitUntil { phone.performedActions.isNotEmpty() }
        assertThat(phone.performedActions).containsExactly("m1" to WearMessageAction.MARK_READ)
    }
}
