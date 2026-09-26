package com.neverreader.ui.theme

import android.content.res.AssetManager
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * The typefaces the app ships.
 *
 * The original brand faces (Graphik LCG for UI, Doyle for display) were
 * commercially licensed and are not redistributable, so they were replaced
 * with the closest open-licensed equivalents:
 *
 *  - [Sans] — Inter for Graphik LCG. A neo-grotesque with the same upright,
 *    neutral-but-warm character, and the best small-size hinting of any
 *    open grotesque, which matters at the 14sp the UI leans on.
 *  - [Serif] — Source Serif 4 for Doyle. A transitional serif with optical
 *    sizes; [Sans] stays the default and the serif is for display text and
 *    long-form reading.
 *
 * Both are OFL-1.1 and ship in `assets/fonts`. The reader loads them by name
 * through `@font-face` rules in `assets/html/c/text.css`, so the two must stay
 * in sync; see [com.neverreader.app.reader.ReaderFragment].
 */
object AppFontFamily {
    private const val DIR = "fonts/"

    /** The UI face. Inter, standing in for Graphik LCG. */
    fun Sans(assets: AssetManager) = FontFamily(
        Font(DIR + "Inter-Regular.ttf", assets, FontWeight.Normal),
        Font(DIR + "Inter-Medium.ttf", assets, FontWeight.Medium),
        Font(DIR + "Inter-Bold.ttf", assets, FontWeight.Bold),
        Font(DIR + "Inter-Italic.ttf", assets, FontWeight.Normal, FontStyle.Italic),
        Font(DIR + "Inter-MediumItalic.ttf", assets, FontWeight.Medium, FontStyle.Italic),
    )

    /**
     * Source Serif 4, standing in for Doyle.
     *
     * Split by optical size the way the family is designed: [Serif] is the
     * reading cut for body text, [Display] is the tighter, higher-contrast cut
     * for headings.
     */
    fun Serif(assets: AssetManager) = FontFamily(
        Font(DIR + "SourceSerif4-Regular.otf", assets, FontWeight.Normal),
        Font(DIR + "SourceSerif4-Semibold.otf", assets, FontWeight.SemiBold),
        Font(DIR + "SourceSerif4-It.otf", assets, FontWeight.Normal, FontStyle.Italic),
    )

    /** The display cut of Source Serif 4, for titles. */
    fun Display(assets: AssetManager) = FontFamily(
        Font(DIR + "SourceSerif4Display-Semibold.otf", assets, FontWeight.SemiBold),
    )
}

/**
 * The type scale. Sizes are transcribed from the legacy `App_Text_*` styles in
 * `res/values/styles.xml`; the Compose names are kept because they are what the
 * existing Compose call sites already use.
 */
@Immutable
data class AppTypography(
    val h1: TextStyle = TextStyle(fontSize = 48.sp, lineHeight = 60.sp, fontWeight = FontWeight.Medium),
    val h2: TextStyle = TextStyle(fontSize = 40.sp, lineHeight = 48.sp, fontWeight = FontWeight.Medium),
    val h3: TextStyle = TextStyle(fontSize = 33.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium),
    val h4: TextStyle = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Medium),
    val h5: TextStyle = TextStyle(fontSize = 23.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    val h6: TextStyle = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    val h7: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    val p1: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
    val p2: TextStyle = TextStyle(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal),
    val p3: TextStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    val p4: TextStyle = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),
) {
    fun withFontFamily(fontFamily: FontFamily) = copy(
        h1 = h1.copy(fontFamily = fontFamily),
        h2 = h2.copy(fontFamily = fontFamily),
        h3 = h3.copy(fontFamily = fontFamily),
        h4 = h4.copy(fontFamily = fontFamily),
        h5 = h5.copy(fontFamily = fontFamily),
        h6 = h6.copy(fontFamily = fontFamily),
        h7 = h7.copy(fontFamily = fontFamily),
        p1 = p1.copy(fontFamily = fontFamily),
        p2 = p2.copy(fontFamily = fontFamily),
        p3 = p3.copy(fontFamily = fontFamily),
        p4 = p4.copy(fontFamily = fontFamily),
    )

    /**
     * Gives the display sizes the serif cut, replacing the Doyle-based
     * `App_Text_Extra_Large_Title` the XML styles used to reach for.
     */
    fun withDisplayFamily(fontFamily: FontFamily) = copy(
        h1 = h1.copy(fontFamily = fontFamily),
        h2 = h2.copy(fontFamily = fontFamily),
        h3 = h3.copy(fontFamily = fontFamily),
    )

    /**
     * Gives the body sizes the reading serif, for long-form article text.
     */
    fun withReadingFamily(fontFamily: FontFamily) = copy(
        p1 = p1.copy(fontFamily = fontFamily),
        p2 = p2.copy(fontFamily = fontFamily),
        p3 = p3.copy(fontFamily = fontFamily),
        p4 = p4.copy(fontFamily = fontFamily),
    )

    /**
     * Maps onto Material 3's slots so that stock components (Button, TextField,
     * Snackbar) render in the brand font and the app's sizes instead of the
     * Roboto/Material defaults.
     */
    fun toMaterialTheme() = Typography(
        displayLarge = h1,
        displayMedium = h2,
        displaySmall = h3,
        headlineLarge = h4,
        headlineMedium = h5,
        headlineSmall = h6,
        titleLarge = h7,
        titleMedium = h7,
        titleSmall = p4,
        bodyLarge = p3,
        bodyMedium = p3,
        bodySmall = p4,
        labelLarge = h7,
        labelMedium = p4,
        labelSmall = p4,
    )
}

val LocalAppTypography = staticCompositionLocalOf { AppTypography() }
