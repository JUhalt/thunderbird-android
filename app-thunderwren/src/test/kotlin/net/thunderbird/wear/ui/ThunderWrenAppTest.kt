package net.thunderbird.wear.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.containsExactly
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
import net.thunderbird.wear.testing.UNIFIED
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
                    DemoAwarePhoneConnection(phone = phone, demo = DemoPhoneConnection(), demoModeStore = demoModeStore)
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
}
