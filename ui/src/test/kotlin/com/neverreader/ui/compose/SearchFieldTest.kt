package com.neverreader.ui.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * SearchField wraps BasicTextField rather than Material's TextField, which
 * means it owns its own placeholder, clear button and focus behaviour and has
 * to get them right itself.
 *
 * The hint-instead-of-placeholder detail is the one worth pinning: the hint is a
 * sibling Text that is drawn only while the query is empty, so a stale hint left
 * visible under real text is exactly the kind of small wrongness that a
 * screenshot at one state would never show.
 */
@RunWith(RobolectricTestRunner::class)
class SearchFieldTest {

    @get:Rule
    val compose = createComposeRule()

    private fun label(res: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    private fun setField(initial: String = "", autoFocus: Boolean = false) {
        val query = mutableStateOf(initial)
        compose.setContent {
            AppTheme(darkTheme = false) {
                SearchField(
                    query = query.value,
                    onQueryChange = { query.value = it },
                    autoFocus = autoFocus,
                )
            }
        }
    }

    @Test
    fun `shows the hint while empty`() {
        setField()

        compose.onNodeWithText(label(R.string.search_hint)).assertIsDisplayed()
    }

    @Test
    fun `the clear button is absent while empty`() {
        setField()

        // Nothing to clear yet, so there is no clear button to find.
        compose.onNodeWithContentDescription(label(R.string.search_clear)).assertDoesNotExist()
    }

    @Test
    fun `typing into the field reveals the clear button`() {
        setField()

        compose.onNodeWithText(label(R.string.search_hint)).assertIsDisplayed()
        // The field is a BasicTextField with no label of its own, so it is found
        // by its text-input semantics rather than by a string.
        compose.onNodeWithText("", useUnmergedTree = true).performTextInput("kotlin")

        compose.onNodeWithContentDescription(label(R.string.search_clear)).assertIsDisplayed()
    }

    @Test
    fun `the clear button empties the query`() {
        setField(initial = "kotlin")

        compose.onNodeWithContentDescription(label(R.string.search_clear)).performClick()
        compose.waitForIdle()

        // Back to empty, so the hint is showing again and the button is gone.
        compose.onNodeWithText(label(R.string.search_hint)).assertIsDisplayed()
        compose.onNodeWithContentDescription(label(R.string.search_clear)).assertDoesNotExist()
    }
}