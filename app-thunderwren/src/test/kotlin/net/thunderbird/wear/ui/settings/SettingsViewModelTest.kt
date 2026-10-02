package net.thunderbird.wear.ui.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.wear.data.DemoMode
import net.thunderbird.wear.data.MessageSwipeAction
import net.thunderbird.wear.testing.FakeDemoModeStore
import net.thunderbird.wear.testing.FakeWatchSettingsStore
import net.thunderbird.wear.ui.settings.SettingsContract.Event

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val settings = FakeWatchSettingsStore()
    private val demoMode = FakeDemoModeStore()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `shows the stored settings and the app version`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        assertThat(testSubject.state.value.settings.swipeLeft).isEqualTo(MessageSwipeAction.ARCHIVE)
        assertThat(testSubject.state.value.settings.swipeRight).isNull()
        assertThat(testSubject.state.value.isDemoOn).isFalse()
        assertThat(testSubject.state.value.appVersion).isEqualTo("1.2.3")
    }

    @Test
    fun `swipe left switches between archive and delete`() = runTest {
        val testSubject = createTestSubject()

        testSubject.event(Event.SwipeLeftClicked)
        assertThat(settings.settings.value.swipeLeft).isEqualTo(MessageSwipeAction.DELETE)

        testSubject.event(Event.SwipeLeftClicked)
        assertThat(settings.settings.value.swipeLeft).isEqualTo(MessageSwipeAction.ARCHIVE)
    }

    @Test
    fun `swipe right goes from going back to archive, delete, and back again`() = runTest {
        val testSubject = createTestSubject()

        testSubject.event(Event.SwipeRightClicked)
        assertThat(settings.settings.value.swipeRight).isEqualTo(MessageSwipeAction.ARCHIVE)

        testSubject.event(Event.SwipeRightClicked)
        assertThat(settings.settings.value.swipeRight).isEqualTo(MessageSwipeAction.DELETE)

        testSubject.event(Event.SwipeRightClicked)
        assertThat(settings.settings.value.swipeRight).isNull()
    }

    @Test
    fun `switches are stored and shown`() = runTest {
        val testSubject = createTestSubject()

        testSubject.event(Event.ConfirmDeleteChanged(true))
        testSubject.event(Event.MarkAsReadWhenOpenedChanged(false))
        testSubject.event(Event.ShowPreviewsChanged(false))
        advanceUntilIdle()

        val stored = settings.settings.value
        assertThat(stored.confirmDelete).isTrue()
        assertThat(stored.markAsReadWhenOpened).isFalse()
        assertThat(stored.showPreviews).isFalse()
        assertThat(testSubject.state.value.settings).isEqualTo(stored)
    }

    @Test
    fun `demo mailbox can be turned on and off`() = runTest {
        val testSubject = createTestSubject()

        testSubject.event(Event.DemoChanged(true))
        advanceUntilIdle()

        assertThat(demoMode.mode.value).isEqualTo(DemoMode.ON)
        assertThat(testSubject.state.value.isDemoOn).isTrue()

        testSubject.event(Event.DemoChanged(false))
        advanceUntilIdle()

        assertThat(demoMode.mode.value).isEqualTo(DemoMode.OFF)
        assertThat(testSubject.state.value.isDemoOn).isFalse()
    }

    private fun createTestSubject() = SettingsViewModel(
        settingsStore = settings,
        demoModeStore = demoMode,
        appVersion = "1.2.3",
    )
}
