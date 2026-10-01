package com.neverreader.ui.compose

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * ItemRow is the one composable every list in the app goes through, and its
 * trailing edge is load-bearing: the archive button has to sit on the same
 * vertical line as the row's content edge, or the row reads as crooked.
 *
 * That alignment was wrong for a long time and nothing noticed. The cause was
 * not visible from the source alone: a weighted date and a second weighted
 * spacer split the leftover width in half, and Compose places the unused
 * remainder after the *last* child, so the icons ended up pushed left by half
 * the empty width -- about 57dp on a phone. Measured from the semantics tree
 * below rather than eyeballed from a screenshot, because a screenshot did not
 * catch it.
 */
@RunWith(RobolectricTestRunner::class)
class ItemRowLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * Drive the row's date through state so one test can compare both branches.
     * A ComposeTestRule owns a single activity and throws on a second
     * setContent, so swapping a var between renders is the way to see the row
     * change in place.
     */
    private val date = mutableStateOf<String?>("Sep 29, 2026")
    private val imageUrl = mutableStateOf<String?>(null)

    /** What ThumbnailRepository.load returns: null means the fetch failed. */
    private val imageBytes = mutableStateOf<ByteArray?>(null)
    private var favoriteTaps = 0
    private var shareTaps = 0
    private var archiveTaps = 0

    private fun setRow(title: String = "A title") {
        compose.setContent {
            AppTheme(darkTheme = false) {
                ItemRow(
                    title = title,
                    domain = "example.com",
                    meta = "3 min read",
                    excerpt = null,
                    imageUrl = imageUrl.value,
                    loadImage = { imageBytes.value },
                    favorite = false,
                    unread = true,
                    savedDate = date.value,
                    onClick = {},
                    onToggleFavorite = { favoriteTaps++ },
                    onShare = { shareTaps++ },
                    onArchive = { archiveTaps++ },
                )
            }
        }
    }

    private fun label(res: Int) = ApplicationProvider.getApplicationContext<android.content.Context>()
        .getString(res)

    private fun rightEdgeOf(res: Int): Float =
        compose.onNodeWithContentDescription(label(res)).fetchSemanticsNode().boundsInRoot.right

    private fun leftEdgeOf(res: Int): Float =
        compose.onNodeWithContentDescription(label(res)).fetchSemanticsNode().boundsInRoot.left

    /** Width of the composable under test in px; the content edge is inside it. */
    private fun widthOfRoot(): Float =
        compose.onRoot().fetchSemanticsNode().boundsInRoot.right

    // ---- the regression ---------------------------------------------------

    /**
     * The regression itself: three actions, in order, ending at the content
     * edge.
     *
     * The root is 320dp wide and the row pads by nr_space_md (20dp), so the
     * content edge is at 300. Before the fix the archive button stopped around
     * 243 -- half the leftover stranded after it.
     */
    @Test
    fun `actions are ordered and flush right with a date present`() {
        setRow()

        val favoriteLeft = leftEdgeOf(R.string.ic_favorite)
        val shareLeft = leftEdgeOf(R.string.ic_share)
        val archiveRight = rightEdgeOf(R.string.ic_archive)
        val contentEdge = widthOfRoot() - 20f

        assertTrue(favoriteLeft < shareLeft, message = "favorite should sit left of share")
        assertTrue(shareLeft < archiveRight, message = "share should sit left of archive")
        assertTrue(
            abs(archiveRight - contentEdge) <= 1f,
            message = "archive right edge $archiveRight should reach the content edge $contentEdge",
        )
    }

    /**
     * The other branch. With no date the spacer stands in for it, and both cases
     * have to land on the same edge, or the actions jump sideways as dates come
     * and go.
     */
    @Test
    fun `archive lands on the same edge when the date goes away`() {
        setRow()
        val withDate = rightEdgeOf(R.string.ic_archive)

        date.value = null
        compose.waitForIdle()
        val withoutDate = rightEdgeOf(R.string.ic_archive)

        assertTrue(
            withoutDate > 0f,
            message = "archive should still be laid out with no date",
        )
        assertEquals(
            withDate, withoutDate,
            message = "archive moved when the date went away",
        )
    }

    /**
     * A date long enough to fill the row must not push the actions off the edge.
     * This is the case that regressed before: with the date weighted fill=false,
     * a long date took all the space and the remainder landed after the icons.
     */
    @Test
    fun `actions stay flush right when the date fills the row`() {
        date.value = "a very long saved-date string that fills the whole row width"
        setRow()

        val contentEdge = widthOfRoot() - 20f
        val archiveRight = rightEdgeOf(R.string.ic_archive)

        assertTrue(
            abs(archiveRight - contentEdge) <= 1f,
            message = "archive right edge $archiveRight should reach $contentEdge with a long date",
        )
    }

    /**
     * A thumbnail sits on the same trailing edge, so it is what the archive
     * button has to line up with. Only presence is asserted: the bytes are
     * fetched by the app rather than Coil, and a loader returning null draws no
     * image, so there is no tile to measure against here.
     */
    @Test
    fun `a row can request a thumbnail without disturbing the actions`() {
        imageUrl.value = "https://example.com/thumb.jpg"
        setRow()

        val contentEdge = widthOfRoot() - 20f
        val archiveRight = rightEdgeOf(R.string.ic_archive)

        assertTrue(
            abs(archiveRight - contentEdge) <= 1f,
            message = "archive should stay on the content edge when a thumbnail is requested",
        )
        compose.onNodeWithText("A title").assertIsDisplayed()
    }

    /**
     * A photo the server cannot serve must leave no trace in the row.
     *
     * ThumbnailRepository.load returns null on any failure, so a broken image
     * used to reserve a 90x60 tile plus its 10dp gap and then draw nothing in
     * it: a hole in the row, with the title narrowed to three quarters of the
     * width for a picture that never arrives. Counting the nodes in the text
     * column is what tells the two cases apart - the tile is a real child when
     * it is laid out, and absent when the bytes never arrive.
     */
    @Test
    fun `a thumbnail that fails to load reserves no space`() {
        imageUrl.value = "https://example.com/gone.jpg"
        imageBytes.value = null
        setRow()

        val withoutTile = textColumnChildCount()

        // produceState is keyed on imageUrl, so a new URL is what makes the load
        // run again - which is exactly what happens when a recycled row gets a
        // different article.
        imageBytes.value = PNG_BYTES
        imageUrl.value = "https://example.com/ok.jpg"
        compose.waitForIdle()
        val withTile = textColumnChildCount()

        assertTrue(
            withoutTile < withTile,
            message = "a failed load should lay out one node fewer than a successful one " +
                "(failed=$withoutTile, loaded=$withTile)",
        )
    }

    @Test
    fun `a row with no thumbnail and a row with a broken one look the same`() {
        imageUrl.value = null
        setRow()
        val noUrl = textColumnChildCount()

        imageUrl.value = "https://example.com/gone.jpg"
        compose.waitForIdle()
        val broken = textColumnChildCount()

        assertEquals(
            noUrl, broken,
            message = "a broken thumbnail should be indistinguishable from having none",
        )
    }

    @Test
    fun `a thumbnail that loads does take up its tile`() {
        imageUrl.value = "https://example.com/ok.jpg"
        imageBytes.value = PNG_BYTES
        setRow()

        val withTile = textColumnChildCount()

        imageBytes.value = null
        imageUrl.value = "https://example.com/gone.jpg"
        compose.waitForIdle()
        val withoutTile = textColumnChildCount()

        assertTrue(
            withTile > withoutTile,
            message = "a loaded thumbnail should add nodes (loaded=$withTile, null=$withoutTile)",
        )
    }

    /** Child count of the row's weighted text column, from the unmerged tree. */
    private fun textColumnChildCount(): Int {
        val row = compose.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val column = row.children.first { it.children.size >= 3 }
        return column.children.size
    }

    // ---- behaviour --------------------------------------------------------

    @Test
    fun `each action reports its own click`() {
        setRow()

        compose.onNodeWithContentDescription(label(R.string.ic_favorite)).performClick()
        compose.onNodeWithContentDescription(label(R.string.ic_share)).performClick()
        compose.onNodeWithContentDescription(label(R.string.ic_archive)).performClick()

        assertEquals(1, favoriteTaps)
        assertEquals(1, shareTaps)
        assertEquals(1, archiveTaps)
    }

    /**
     * The icons carry their own contentDescription, which is all TalkBack has to
     * go on. A dropped label is a silent accessibility regression that nothing
     * else in the build would report.
     */
    @Test
    fun `every action is labelled for accessibility`() {
        setRow()

        for (res in listOf(R.string.ic_favorite, R.string.ic_share, R.string.ic_archive)) {
            val text = label(res)
            assertTrue(text.isNotBlank(), message = "string resource $res is blank")
            compose.onNodeWithContentDescription(text).assertIsDisplayed()
        }
    }

    /**
     * The meta-line joins domain and reading time with a middot, so asserting on
     * the pieces separately would be wrong; the joined string is the contract.
     */
    @Test
    fun `row renders title and the joined meta line`() {
        setRow(title = "Hello")

        compose.onNodeWithText("Hello").assertIsDisplayed()
        compose.onNodeWithText("example.com · 3 min read").assertIsDisplayed()
        compose.onNodeWithText("Sep 29, 2026").assertIsDisplayed()
    }

    @Test
    fun `row renders without a date`() {
        date.value = null
        setRow()

        compose.onNodeWithText("A title").assertIsDisplayed()
        compose.onNodeWithText("example.com · 3 min read").assertIsDisplayed()
        assertTrue(
            rightEdgeOf(R.string.ic_archive) > 0f,
            message = "archive should still be laid out with no date",
        )
    }

    private companion object {
        /**
         * A real 1x1 PNG. The tile is never decoded in these tests - Coil does not
         * fetch under Robolectric - but passing bytes rather than null is what
         * distinguishes "the fetch worked" from "it did not".
         */
        val PNG_BYTES: ByteArray = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
        )
    }
}
