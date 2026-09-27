package net.thunderbird.wear.ui.settings

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import net.thunderbird.wear.data.SwipeActions
import net.thunderbird.wear.testing.FakeWatchSettingsStore
import net.thunderbird.wear.ui.settings.SettingsContract.Event

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val mainDispatcher = MainDispatcherHelper()
    private val settings = FakeWatchSettingsStore()

    @BeforeTest
    fun setUp() = mainDispatcher.setUp()

    @AfterTest
    fun tearDown() = mainDispatcher.tearDown()

    @Test
    fun `shows the stored settings and the app version`() = runTest {
        val testSubject = createTestSubject()
        advanceUntilIdle()

        assertThat(testSubject.state.value.settings.swipeActions).isEqualTo(SwipeActions.LEFT_ARCHIVE)
        assertThat(testSubject.state.value.appVersion).isEqualTo("1.2.3")
    }

    @Test
    fun `changes are stored and shown`() = runTest {
        val testSubject = createTestSubject()

        testSubject.event(Event.SwipeActionsSelected(SwipeActions.LEFT_ARCHIVE_RIGHT_DELETE))
        testSubject.event(Event.ConfirmDeleteChanged(true))
        testSubject.event(Event.MarkAsReadWhenOpenedChanged(false))
        testSubject.event(Event.ShowPreviewsChanged(false))
        advanceUntilIdle()

        val stored = settings.settings.value
        assertThat(stored.swipeActions).isEqualTo(SwipeActions.LEFT_ARCHIVE_RIGHT_DELETE)
        assertThat(stored.confirmDelete).isTrue()
        assertThat(stored.markAsReadWhenOpened).isFalse()
        assertThat(stored.showPreviews).isFalse()
        assertThat(testSubject.state.value.settings).isEqualTo(stored)
    }

    private fun createTestSubject() = SettingsViewModel(settingsStore = settings, appVersion = "1.2.3")
}
