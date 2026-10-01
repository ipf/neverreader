package com.neverreader.ui.view.button

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.neverreader.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * The filled button, used once in the app: "Save" in the add-URL sheet.
 *
 * It is the only control that is a plain Material Button rather than something
 * hand-built from AppTheme tokens, because it sits in a bottom sheet and wanted
 * Material's own container colour. The height therefore comes from the caller,
 * not from here, which is worth pinning: AddUrlSheet passes 48dp, and a default
 * creeping in here would silently change every button's size.
 */
@RunWith(RobolectricTestRunner::class)
class BoxButtonTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `renders its text`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                BoxButton(text = "Save", onClick = {})
            }
        }

        compose.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun `clicking reports the click`() {
        var taps = 0
        compose.setContent {
            AppTheme(darkTheme = false) {
                BoxButton(text = "Save", onClick = { taps++ })
            }
        }

        compose.onNodeWithText("Save").performClick()

        assertEquals(1, taps)
    }

    @Test
    fun `the caller decides the height`() {
        compose.setContent {
            AppTheme(darkTheme = false) {
                BoxButton(
                    text = "Save",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
            }
        }

        compose.onNodeWithText("Save").assertHeightIsEqualTo(48.dp)
    }
}