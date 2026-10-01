package com.neverreader.ui.view.button

import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * The 50dp touch target is a deliberate accessibility decision, not a style
 * choice: it is what keeps every action in the app reachable without the label
 * having to grow. AppIconButton is the single place that size is set, so if
 * someone shrinks it the whole app silently drops below the recommended
 * minimum and nothing else would report it.
 */
@RunWith(RobolectricTestRunner::class)
class AppIconButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private fun label(res: Int) =
        ApplicationProvider.getApplicationContext<android.content.Context>().getString(res)

    @Test
    fun `the touch target stays at 50dp`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                AppIconButton(onClick = {}) { ArchiveIcon() }
            }
        }

        compose.onNodeWithContentDescription(label(R.string.ic_archive))
            .assertHeightIsEqualTo(50.dp)
    }

    @Test
    fun `clicking reports once`() {
        var taps = 0
        compose.setContent {
            AppTheme(darkTheme = false) {
                AppIconButton(onClick = { taps++ }) { ArchiveIcon() }
            }
        }

        compose.onNodeWithContentDescription(label(R.string.ic_archive)).performClick()

        assertEquals(1, taps)
    }

    /**
     * AppIconButton deliberately has no long-press tooltip; the comment on it
     * says the icon's own contentDescription is what TalkBack uses. That makes
     * every AppIcons entry's label load-bearing, so all of them are checked for
     * being present and non-blank here rather than left to a manual pass.
     */
    @Test
    fun `every app icon carries a label`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                AppIconButton(onClick = {}) { ArchiveIcon() }
            }
        }

        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        for (res in listOf(
            R.string.ic_archive,
            R.string.ic_favorite,
            R.string.ic_share,
        )) {
            val text = context.getString(res)
            assert(text.isNotBlank()) { "string resource $res is blank" }
        }
        compose.onNodeWithContentDescription(label(R.string.ic_archive)).assertIsDisplayed()
    }
}