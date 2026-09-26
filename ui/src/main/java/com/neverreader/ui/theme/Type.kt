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
 * Typeface used for all UI text.
 *
 * The original brand faces (Graphik LCG, Doyle) are commercially licensed and
 * are stored GPG-encrypted under `secrets/fonts/`. Run `secrets/decrypt.sh` to
 * drop them into the assets and they will be picked up automatically. Until
 * then we fall back to Inter, which is the closest open-licensed geometric
 * grotesque and is what gives the app its Pocket-era look.
 */
object AppFontFamily {
    private const val ASSET_DIR = "fonts"

    private val GRAPHIK = mapOf(
        FontWeight.Normal to "graphik_lcg_regular_no_leading.otf",
        FontWeight.Medium to "graphik_lcg_medium_no_leading.otf",
        FontWeight.Bold to "graphik_lcg_bold_no_leading.otf",
    )

    private val INTER = mapOf(
        FontWeight.Normal to "Inter-Regular.ttf",
        FontWeight.Medium to "Inter-Medium.ttf",
        FontWeight.Bold to "Inter-Bold.ttf",
    )

    private fun exists(assets: AssetManager, name: String): Boolean = try {
        assets.open("$ASSET_DIR/$name").close()
        true
    } catch (_: java.io.IOException) {
        false
    }

    fun of(assets: AssetManager): FontFamily {
        val family = if (GRAPHIK.values.all { exists(assets, it) }) GRAPHIK else INTER
        val regular = family.getValue(FontWeight.Normal)
        val medium = family.getValue(FontWeight.Medium)
        val bold = family.getValue(FontWeight.Bold)

        // Inter and Graphik name their italics differently, so resolve each by
        // what is actually on disk rather than deriving the name.
        fun italic(candidates: List<String>): String =
            candidates.firstOrNull { exists(assets, it) } ?: regular

        val regularItalic = italic(
            listOf("Inter-Italic.ttf", "graphik_lcg_regular_italic_no_leading.otf")
        )
        val mediumItalic = italic(
            listOf("Inter-MediumItalic.ttf", "graphik_lcg_medium_italic_no_leading.otf")
        )

        return FontFamily(
            Font("$ASSET_DIR/$regular", assets, FontWeight.Normal),
            Font("$ASSET_DIR/$medium", assets, FontWeight.Medium),
            Font("$ASSET_DIR/$bold", assets, FontWeight.Bold),
            Font("$ASSET_DIR/$regularItalic", assets, FontWeight.Normal, FontStyle.Italic),
            Font("$ASSET_DIR/$mediumItalic", assets, FontWeight.Medium, FontStyle.Italic),
        )
    }
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
