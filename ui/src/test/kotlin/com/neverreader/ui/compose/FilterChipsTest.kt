package com.neverreader.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.neverreader.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The filter row above the list, and the one place a selection bug is invisible:
 * tapping the wrong chip still navigates somewhere sensible, so it only shows up
 * as a filter that quietly stops changing the results.
 *
 * Also covers the `trailing` slot, because that is the component's one
 * non-obvious contract: the sort control lives in the scrolling row rather than
 * the app bar, and it has to stay clickable while the row scrolls.
 */
@RunWith(RobolectricTestRunner::class)
class FilterChipsTest {

    @get:Rule
    val compose = createComposeRule()

    private var selected: String? = null

    private fun setChips(
        tabs: List<String> = listOf("Unread", "Favorites", "Archive"),
        current: String = "Unread",
    ) {
        selected = null
        compose.setContent {
            AppTheme(darkTheme = false) {
                FilterChips(
                    tabs = tabs,
                    selected = current,
                    onSelect = { selected = it },
                    label = { it },
                    trailing = {
                        FilterChip(label = "Newest", selected = false, onClick = { selected = "Newest" })
                    },
                )
            }
        }
    }

    @Test
    fun `renders every tab plus the trailing slot`() {
        setChips()

        for (label in listOf("Unread", "Favorites", "Archive", "Newest")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun `selecting a chip reports that tab`() {
        setChips()

        compose.onNodeWithText("Archive").performClick()

        assertEquals("Archive", selected)
    }

    /**
     * The reported tab has to be the one that was tapped, not merely something
     * non-null: a lambda capturing the loop index would pass the test above and
     * still select the wrong filter.
     */
    @Test
    fun `each chip reports itself and not its neighbour`() {
        setChips()

        compose.onNodeWithText("Favorites").performClick()
        assertEquals("Favorites", selected)

        compose.onNodeWithText("Unread").performClick()
        assertEquals("Unread", selected)
    }

    @Test
    fun `the trailing slot is clickable and reports separately from the tabs`() {
        setChips()

        compose.onNodeWithText("Newest").performScrollTo().performClick()

        assertEquals("Newest", selected)
    }

    /**
     * A single tab is the degenerate case, and the one that shows up on a fresh
     * account before any filtering is possible.
     */
    @Test
    fun `handles a single tab`() {
        setChips(tabs = listOf("Unread"))

        compose.onNodeWithText("Unread").assertIsDisplayed()
        compose.onNodeWithText("Unread").performClick()
        assertEquals("Unread", selected)
    }

    @Test
    fun `renders with no tabs at all, keeping the trailing slot usable`() {
        setChips(tabs = emptyList())

        compose.onNodeWithText("Newest").assertIsDisplayed()
        compose.onNodeWithText("Newest").performClick()
        assertEquals("Newest", selected)
    }
}