package net.thunderbird.wear.tile

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import kotlin.test.Test
import net.thunderbird.wear.ThunderWrenActivity
import net.thunderbird.wear.sync.Glance
import net.thunderbird.wear.sync.GlanceMessage
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
// A plain Application, so the app's Koin isn't started for a layout test.
@Config(application = Application::class, sdk = [33])
class UnreadTileLayoutTest {
    private val testSubject = UnreadTileLayout(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `shows how to connect before the phone has published`() {
        val texts = testSubject.create(glance = null, screenWidthDp = SCREEN_WIDTH).texts()

        assertThat(texts).containsExactly("ThunderWren", "Open Thunderbird on your phone")
    }

    @Test
    fun `shows only the app name when the count is hidden`() {
        val texts = testSubject.create(Glance(unreadCount = null, latestUnread = emptyList()), SCREEN_WIDTH).texts()

        assertThat(texts).containsExactly("ThunderWren", "Tap to see your mail")
    }

    @Test
    fun `shows the count without senders`() {
        val texts = testSubject.create(Glance(unreadCount = 4, latestUnread = emptyList()), SCREEN_WIDTH).texts()

        assertThat(texts).containsExactly("ThunderWren", "4 unread")
    }

    @Test
    fun `shows two messages with subjects, each opening the message`() {
        val glance = Glance(
            unreadCount = 3,
            latestUnread = listOf(
                GlanceMessage(id = "m1", sender = "Ada", subject = "Lunch?"),
                GlanceMessage(id = "m2", sender = "Charles", subject = "Engine"),
                GlanceMessage(id = "m3", sender = "Grace", subject = "Compilers"),
            ),
        )

        val root = testSubject.create(glance, SCREEN_WIDTH)

        assertThat(root.texts()).containsExactly("3 unread", "Ada", "Lunch?", "Charles", "Engine")
        assertThat(root.openedMessageIds()).containsExactly("m1", "m2")
    }

    @Test
    fun `shows three senders when subjects are hidden`() {
        val glance = Glance(
            unreadCount = 5,
            latestUnread = listOf("Ada", "Charles", "Grace").mapIndexed { index, sender ->
                GlanceMessage(id = "m$index", sender = sender, subject = null)
            },
        )

        val texts = testSubject.create(glance, SCREEN_WIDTH).texts()

        assertThat(texts).containsExactly("5 unread", "Ada", "Charles", "Grace")
        assertThat(texts).doesNotContain("Lunch?")
    }

    private fun LayoutElementBuilders.LayoutElement.texts(): List<String> = elements().mapNotNull { element ->
        (element as? LayoutElementBuilders.Text)?.text?.value
    }

    private fun LayoutElementBuilders.LayoutElement.openedMessageIds(): List<String> {
        return elements().mapNotNull { element ->
            val launchAction = element.modifiers()?.clickable?.onClick as? ActionBuilders.LaunchAction
            val extra = launchAction?.androidActivity?.keyToExtraMapping?.get(ThunderWrenActivity.EXTRA_MESSAGE_ID)
            (extra as? ActionBuilders.AndroidStringExtra)?.value
        }
    }

    private fun LayoutElementBuilders.LayoutElement.elements(): List<LayoutElementBuilders.LayoutElement> {
        val children = when (this) {
            is LayoutElementBuilders.Box -> contents
            is LayoutElementBuilders.Column -> contents
            else -> emptyList()
        }
        return listOf(this) + children.flatMap { it.elements() }
    }

    private fun LayoutElementBuilders.LayoutElement.modifiers(): ModifiersBuilders.Modifiers? = when (this) {
        is LayoutElementBuilders.Box -> modifiers
        is LayoutElementBuilders.Column -> modifiers
        is LayoutElementBuilders.Text -> modifiers
        else -> null
    }

    private companion object {
        const val SCREEN_WIDTH = 192
    }
}
