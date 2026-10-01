package com.neverreader.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import com.neverreader.util.prefs.AndroidPrefStore
import com.neverreader.util.prefs.BooleanPref
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The settings screen's rows.
 *
 * SettingsToggle is the interesting one. It takes the preference rather than a
 * value, so the row stays in step with anything else that writes to it - which is
 * the whole point, and the sort of thing that quietly stops being true if someone
 * changes it to take a Boolean and hoist the state into the screen.
 */
@RunWith(RobolectricTestRunner::class)
class SettingsListTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var store: AndroidPrefStore
    private lateinit var pref: BooleanPref

    private fun label(res: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    @Before
    fun setUp() {
        // AndroidPrefStore wraps a SharedPreferences, not a Context.
        val prefs = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getSharedPreferences("settings-list-test", android.content.Context.MODE_PRIVATE)
        store = AndroidPrefStore(prefs)
        store.clear()
        pref = BooleanPref("settings_test_toggle", defaultValue = false, store = store)
    }

    // ---- SettingsAction ----------------------------------------------------

    @Test
    fun `an action row renders its title`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsAction(title = "Account", onClick = {})
            }
        }

        compose.onNodeWithText("Account").assertIsDisplayed()
    }

    @Test
    fun `an action row renders its summary when given one`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsAction(title = "Server", summary = "https://readeck.example", onClick = {})
            }
        }

        compose.onNodeWithText("Server").assertIsDisplayed()
        compose.onNodeWithText("https://readeck.example").assertIsDisplayed()
    }

    @Test
    fun `an action row without a summary shows only its title`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsAction(title = "Log out", onClick = {})
            }
        }

        compose.onNodeWithText("Log out").assertIsDisplayed()
    }

    @Test
    fun `tapping an action row reports the click`() {
        var taps = 0
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsAction(title = "Log out", onClick = { taps++ })
            }
        }

        compose.onNodeWithText("Log out").performClick()

        assertEquals(1, taps)
    }

    // ---- SettingsToggle ----------------------------------------------------

    @Test
    fun `a toggle row reflects the stored value`() {
        pref.set(true)
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsToggle(titleRes = R.string.search_hint, pref = pref)
            }
        }

        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun `a toggle row starts off when the preference has never been set`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsToggle(titleRes = R.string.search_hint, pref = pref)
            }
        }

        compose.onNode(isToggleable()).assertIsOff()
    }

    @Test
    fun `tapping the row flips the preference`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsToggle(titleRes = R.string.search_hint, pref = pref)
            }
        }

        compose.onNode(isToggleable()).performClick()

        assertTrue(pref.get(), "tapping the row should have written true to the preference")
    }

    /**
     * The point of taking the preference instead of a value: a write from
     * anywhere - a sync, another screen - has to move this switch. A row that
     * held its own state would still show the stale value here.
     */
    @Test
    fun `a write from outside the row moves the switch`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsToggle(titleRes = R.string.search_hint, pref = pref)
            }
        }
        compose.onNode(isToggleable()).assertIsOff()

        pref.set(true)
        compose.waitForIdle()

        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun `toggling twice returns to the original value`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsToggle(titleRes = R.string.search_hint, pref = pref)
            }
        }

        compose.onNode(isToggleable()).performClick()
        compose.onNode(isToggleable()).performClick()

        assertEquals(false, pref.get(), "two taps should cancel out")
    }

    // ---- SettingsHeader ----------------------------------------------------

    @Test
    fun `a section header renders its title`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                SettingsHeader(title = "Account")
            }
        }

        compose.onNodeWithText("Account").assertIsDisplayed()
    }
}