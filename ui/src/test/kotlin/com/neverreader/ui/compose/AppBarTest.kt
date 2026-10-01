package com.neverreader.ui.compose

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The bar at the top of every screen.
 *
 * Its height is pinned to nr_app_bar_height so it lines up with the settings
 * screen, which still uses the XML bar. Nothing else in the build can check
 * that, because a wrong height still lays out perfectly well - it just leaves a
 * seam between the two bars.
 */
@RunWith(RobolectricTestRunner::class)
class AppBarTest {

    @get:Rule
    val compose = createComposeRule()

    private var navigationTaps = 0
    private var actionTaps = 0

    private fun label(res: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    private fun setBar(
        title: String? = "Saves",
        navigation: Boolean = true,
        actionCount: Int = 0,
    ) {
        navigationTaps = 0
        actionTaps = 0
        compose.setContent {
            AppTheme(darkTheme = false) {
                AppBar(
                    navigationIcon = {
                        if (navigation) {
                            AppIconButton(onClick = { navigationTaps++ }) { UpIcon() }
                        }
                    },
                    title = { if (title != null) Text(title) },
                    actions = {
                        repeat(actionCount) {
                            AppIconButton(onClick = { actionTaps++ }) { UpIcon() }
                        }
                    },
                )
            }
        }
    }

    /** Index-th node carrying the up icon, as a matcher we can act on. */
    private fun iconAt(index: Int) =
        compose.onAllNodesWithContentDescription(label(R.string.ic_up))[index]

    private fun icons() = compose.onAllNodesWithContentDescription(label(R.string.ic_up))
        .fetchSemanticsNodes()

    /**
     * The one thing worth pinning: the bar is 56dp so it meets the settings
     * screen's XML bar without a seam. A wrong height still lays out perfectly
     * well, which is exactly why nothing else in the build would notice.
     */
    @Test
    fun `the bar is 56dp tall`() {
        setBar()

        compose.onNodeWithTag(AppBarTestTag).assertHeightIsEqualTo(56.dp)
    }

    /**
     * And the 50dp action target has to fit inside it, which is the constraint
     * that makes 56dp the number rather than a round 48.
     */
    @Test
    fun `an action fits inside the bar height`() {
        setBar(navigation = true)

        compose.onNodeWithContentDescription(label(R.string.ic_up))
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun `renders the title`() {
        setBar(title = "Saves")

        compose.onNodeWithText("Saves").assertIsDisplayed()
    }

    /**
     * The bar is used with no title on the reader, where the article heading does
     * that job, so an absent title has to be a supported state.
     */
    @Test
    fun `renders with no title`() {
        setBar(title = null)

        compose.onNodeWithContentDescription(label(R.string.ic_up)).assertIsDisplayed()
    }

    @Test
    fun `renders with no navigation icon`() {
        setBar(navigation = false)

        compose.onNodeWithText("Saves").assertIsDisplayed()
        assertTrue(icons().isEmpty(), "no navigation icon should be present")
    }

    @Test
    fun `navigation reports its click`() {
        setBar()

        compose.onNodeWithContentDescription(label(R.string.ic_up)).performClick()

        assertEquals(1, navigationTaps)
    }

    /**
     * The sort control was moved out of the bar and into the scrolling chip row,
     * because four 50dp actions squeezed the title on a narrow phone. The bar
     * still takes a variable number of actions, and dropping the title to make
     * room for them would be a regression rather than a fix.
     */
    @Test
    fun `actions do not displace the title`() {
        setBar(title = "Saves", actionCount = 3)

        compose.onNodeWithText("Saves").assertIsDisplayed()
        assertEquals(4, icons().size, "expected the navigation icon and three actions")
    }

    @Test
    fun `each action reports a click without the navigation icon`() {
        setBar(actionCount = 2)

        iconAt(1).performClick()
        iconAt(2).performClick()

        assertEquals(0, navigationTaps, "the navigation icon was tapped instead")
        assertEquals(2, actionTaps)
    }
}