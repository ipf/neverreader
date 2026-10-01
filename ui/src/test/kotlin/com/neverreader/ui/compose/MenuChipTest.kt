package com.neverreader.ui.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The sort chip on the list screen: it shows the current choice and opens a menu
 * of the alternatives.
 *
 * The behaviour worth pinning is the part that is easy to get subtly wrong and
 * invisible until used: the chip has to open the menu, picking an option has to
 * both report it *and* close the menu, and tapping away has to close the menu
 * without reporting anything. The last one matters most - a menu that stays open
 * after a dismissed tap sits over the list with no way out.
 */
@RunWith(RobolectricTestRunner::class)
class MenuChipTest {

    @get:Rule
    val compose = createComposeRule()

    private enum class Sort { NEWEST, OLDEST, TITLE }

    private var selected: Sort? = null

    private fun setChip(current: Sort = Sort.NEWEST) {
        selected = null
        compose.setContent {
            AppTheme(darkTheme = false) {
                MenuChip(
                    options = Sort.entries,
                    selected = current,
                    optionLabel = {
                        when (it) {
                            Sort.NEWEST -> "Newest"
                            Sort.OLDEST -> "Oldest"
                            Sort.TITLE -> "Title"
                        }
                    },
                    onSelect = { selected = it },
                    icon = R.drawable.ic_nr_sort_line,
                    iconContentDescription = R.string.ic_sort,
                )
            }
        }
    }

    /**
     * A menu entry matching [label].
     *
     * Once the menu is open, the current choice appears twice: on the chip and as
     * its own entry. Selecting by text alone is ambiguous, and the chip merges the
     * sort icon's contentDescription into its row while a menu entry carries none
     * - which is the difference used here to tell them apart.
     */
    private fun menuEntry(label: String) = compose.onNode(
        hasText(label) and SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription),
    )

    /** The chip itself, which is the one node carrying the icon's label. */
    private fun chip() = compose.onNode(hasContentDescription(sortLabel))

    private val sortLabel: String
        get() = ApplicationProvider.getApplicationContext<android.content.Context>()
            .getString(R.string.ic_sort)

    @Test
    fun `shows the current choice`() {
        setChip(current = Sort.OLDEST)

        compose.onNodeWithText("Oldest").assertIsDisplayed()
    }

    @Test
    fun `the menu is closed until the chip is tapped`() {
        setChip()

        compose.onNodeWithText("Oldest").assertDoesNotExist()
    }

    @Test
    fun `tapping the chip opens every option`() {
        setChip()

        chip().performClick()

        compose.onNodeWithText("Oldest").assertIsDisplayed()
        compose.onNodeWithText("Title").assertIsDisplayed()
    }

    @Test
    fun `picking an option reports it and closes the menu`() {
        setChip()

        chip().performClick()
        menuEntry("Title").performClick()

        assertEquals(Sort.TITLE, selected)
        // Closed: the choice is now on the chip, and nothing is left floating
        // over the list.
        compose.onNodeWithText("Oldest").assertDoesNotExist()
    }

    /**
     * Picking the option already shown still has to close the menu. Skipping the
     * close would leave it hanging open for a no-op.
     */
    @Test
    fun `picking the current option still closes the menu`() {
        setChip(current = Sort.NEWEST)

        chip().performClick()
        menuEntry("Newest").performClick()

        assertEquals(Sort.NEWEST, selected)
        compose.onNodeWithText("Oldest").assertDoesNotExist()
    }

    /** A dismissed tap must not look like a selection. */
    @Test
    fun `dismissing the menu reports nothing`() {
        setChip()

        chip().performClick()
        menuEntry("Oldest").assertIsDisplayed()
        // The menu dismisses on an outside touch, which is what onDismissRequest
        // is wired to; tapping the chip again is that outside touch.
        chip().performClick()

        assertTrue(selected == null, "a dismissed menu should not report a choice, got $selected")
    }

    /**
     * The chip's icon is the only thing telling a screen-reader user this opens a
     * menu, and it is easy to drop the description when swapping the icon.
     */
    @Test
    fun `the sort icon carries a label`() {
        setChip()

        assertTrue(sortLabel.isNotBlank(), "the sort icon's label is blank")
        chip().assertIsDisplayed()
    }
}