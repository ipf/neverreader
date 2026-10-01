package com.neverreader.ui.compose

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.neverreader.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The snackbar host, which AbsNeverReaderActivity mounts once for the whole app
 * rather than per screen.
 *
 * That arrangement means it holds a SnackbarHostState it does not own, so what
 * matters is that the host surfaces what is sent to it, including the action
 * label. A host that quietly dropped messages would break every screen's error
 * reporting with nothing left to show for it.
 *
 * Deliberately not using runTest: a snackbar's dismissal is a timed suspend on
 * the Compose rule's own clock, which a TestScope would wait on forever. Messages
 * are shown as Indefinite so they stay up for the assertion.
 */
@RunWith(RobolectricTestRunner::class)
class AppSnackbarHostTest {

    @get:Rule
    val compose = createComposeRule()

    private val scope = CoroutineScope(SupervisorJob())

    @After
    fun tearDown() {
        scope.cancel()
    }

    private fun setHost(state: SnackbarHostState) {
        compose.setContent {
            AppTheme(darkTheme = false) {
                AppSnackbarHost(hostState = state)
            }
        }
    }

    private fun show(state: SnackbarHostState, message: String, action: String? = null) {
        scope.launch {
            state.showSnackbar(message, actionLabel = action, duration = SnackbarDuration.Indefinite)
        }
        compose.waitForIdle()
    }

    @Test
    fun `an empty host shows nothing`() {
        val state = SnackbarHostState()
        setHost(state)

        compose.waitForIdle()

        assertNull(state.currentSnackbarData, "a fresh host should have nothing queued")
    }

    @Test
    fun `shows the message sent to it`() {
        val state = SnackbarHostState()
        setHost(state)

        show(state, "Sync failed")

        compose.onNodeWithText("Sync failed").assertIsDisplayed()
    }

    /**
     * The action is the whole reason a failure message is worth showing: "Retry"
     * has to reach the screen, and tapping it has to take.
     */
    @Test
    fun `shows and acts on the retry label`() {
        val state = SnackbarHostState()
        setHost(state)

        show(state, "Sync failed", action = "Retry")

        compose.onNodeWithText("Retry").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.waitForIdle()

        // Clicking the action dismisses the host's message rather than leaving it
        // stuck on screen over the list.
        compose.onNodeWithText("Sync failed").assertDoesNotExist()
    }

    /**
     * Mounted once per activity, so a second message arriving while the first is
     * up has to queue rather than replace it or drop it.
     */
    @Test
    fun `a second message queues behind the first`() {
        val state = SnackbarHostState()
        setHost(state)

        show(state, "First")
        compose.onNodeWithText("First").assertIsDisplayed()

        show(state, "Second")

        // showSnackbar suspends until the message it queued is dismissed, so the
        // second one is still waiting and the first must stay on screen rather
        // than being dropped or overwritten.
        compose.onNodeWithText("First").assertIsDisplayed()
        assertEquals("First", state.currentSnackbarData?.visuals?.message)
    }
}