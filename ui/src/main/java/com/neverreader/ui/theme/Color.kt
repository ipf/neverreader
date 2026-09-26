package com.neverreader.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Single source of truth for the NeverReader palette.
 *
 * These values are a direct transcription of the design team's spec
 * (https://www.figma.com/file/ZJfEMS2fVY0TdB2Ao3uWv8/FINAL-Design-System).
 * The legacy `res/values/colors.xml` copy is deleted with the rest of the
 * view system; keep new colours here.
 *
 * The ramps are numbered light-to-dark, which is why the semantic role
 * [AppColors.grey1] resolves to `Grey1` in light mode and `DmGrey1` in dark.
 */
@Suppress("MagicNumber")
object Palette {
    // Functional
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)
    val Transparent = Color(0x00000000)

    // Light mode ramps
    val Grey1 = Color(0xFF1A1A1A)
    val Grey2 = Color(0xFF333333)
    val Grey3 = Color(0xFF404040)
    val Grey4 = Color(0xFF737373)
    val Grey5 = Color(0xFF8C8C8C)
    val Grey6 = Color(0xFFD9D9D9)
    val Grey7 = Color(0xFFECECEC)

    val Teal1 = Color(0xFF004D48)
    val Teal2 = Color(0xFF008078)
    val Teal3 = Color(0xFF009990)
    val Teal4 = Color(0xFF80BFBB)
    val Teal5 = Color(0xFF95D5D2)
    val Teal6 = Color(0xFFE8F7F6)

    val Coral1 = Color(0xFF901424)
    val Coral2 = Color(0xFFEF4056)
    val Coral3 = Color(0xFFF79FAA)
    val Coral4 = Color(0xFFFBCFD4)
    val Coral5 = Color(0xFFFDECEE)
    val Coral6 = Color(0xFFFDF2F5)

    val Amber3 = Color(0xFFFF9F00)
    val Amber4 = Color(0xFFFEE8C3)
    val Apricot1 = Color(0xFFB24000)

    val Lapis1 = Color(0xFF1649AC)
    val Lapis3 = Color(0xFF3668FF)
    val Lapis5 = Color(0xFFDCEAFF)

    // Dark mode ramps
    val DmGrey1 = Color(0xFFF2F2F2)
    val DmGrey2 = Color(0xFFCCCCCC)
    val DmGrey3 = Color(0xFFCCCCCC)
    val DmGrey4 = Color(0xFF999999)
    val DmGrey5 = Color(0xFF737373)
    val DmGrey6 = Color(0xFF404040)
    val DmGrey7 = Color(0xFF333333)

    val DmTeal1 = Color(0xFF004D48)
    val DmTeal2 = Color(0xFF008078)
    val DmTeal3 = Color(0xFF00CCC0)
    val DmTeal4 = Color(0xFF0D4643)
    val DmTeal5 = Color(0xFF13302E)
    val DmTeal6 = Color(0xFF162827)

    val DmCoral1 = Color(0xFF901424)
    val DmCoral2 = Color(0xFFEF4056)
    val DmCoral3 = Color(0xFF842D38)
    val DmCoral4 = Color(0xFF642028)
    val DmCoral5 = Color(0xFF421218)
    val DmCoral6 = Color(0xFF421218)

    val DmAmber3 = Color(0xFFFF9F00)
    val DmApricot1 = Color(0xFFE55300)

    val DmLapis1 = Color(0xFF1649AC)
    val DmLapis3 = Color(0xFF95D2FF)
    val DmLapis5 = Color(0xFF15253D)
}

/**
 * Semantic colours. Each role names what it is *for*, so a component never
 * reaches for a raw ramp value and never has to branch on light vs dark.
 *
 * This is what replaces the 68 `nr_themed_*` state-list selectors: they were
 * doing this same job through a custom `state_dark` drawable-state.
 */
@Immutable
data class AppColors(
    val background: Color,
    val onBackground: Color,
    val cardBackground: Color,
    val divider: Color,
    val iconButtonBackground: Color,
    val chipBackground: Color,
    val chipSelectedBackground: Color,
    val primary: Color,
    val onPrimary: Color,
    val teal1: Color,
    val teal2: Color,
    val teal3: Color,
    val teal6: Color,
    val onTeal: Color,
    val coral2: Color,
    val coral5: Color,
    val onCoral: Color,
    val amber3: Color,
    val amber4: Color,
    val apricot1: Color,
    val lapis1: Color,
    val lapis3: Color,
    val lapis5: Color,
    val grey1: Color,
    val grey2: Color,
    val grey3: Color,
    val grey4: Color,
    val grey5: Color,
    val grey6: Color,
    val grey7: Color,
) {
    /** Secondary text, the "domain · 5 min" line under an article title. */
    val textSecondary: Color get() = grey4

    /** Tertiary text and disabled icon tints. */
    val textTertiary: Color get() = grey5
}

@Suppress("MagicNumber")
val LightColors = AppColors(
    background = Palette.White,
    onBackground = Palette.Grey1,
    cardBackground = Palette.White,
    divider = Palette.Grey6,
    iconButtonBackground = Palette.Transparent,
    chipBackground = Palette.Grey7,
    chipSelectedBackground = Palette.Teal6,
    primary = Palette.Teal2,
    onPrimary = Palette.White,
    teal1 = Palette.Teal1,
    teal2 = Palette.Teal2,
    teal3 = Palette.Teal3,
    teal6 = Palette.Teal6,
    onTeal = Palette.White,
    coral2 = Palette.Coral2,
    coral5 = Palette.Coral5,
    onCoral = Palette.White,
    amber3 = Palette.Amber3,
    amber4 = Palette.Amber4,
    apricot1 = Palette.Apricot1,
    lapis1 = Palette.Lapis1,
    lapis3 = Palette.Lapis3,
    lapis5 = Palette.Lapis5,
    grey1 = Palette.Grey1,
    grey2 = Palette.Grey2,
    grey3 = Palette.Grey3,
    grey4 = Palette.Grey4,
    grey5 = Palette.Grey5,
    grey6 = Palette.Grey6,
    grey7 = Palette.Grey7,
)

@Suppress("MagicNumber")
val DarkColors = AppColors(
    background = Palette.DmGrey1,
    onBackground = Palette.DmGrey1,
    cardBackground = Palette.Black,
    divider = Palette.DmGrey7,
    iconButtonBackground = Palette.Transparent,
    chipBackground = Palette.DmGrey7,
    chipSelectedBackground = Palette.DmTeal5,
    primary = Palette.DmTeal3,
    onPrimary = Palette.Black,
    teal1 = Palette.DmTeal1,
    teal2 = Palette.DmTeal2,
    teal3 = Palette.DmTeal3,
    teal6 = Palette.DmTeal6,
    onTeal = Palette.White,
    coral2 = Palette.DmCoral2,
    coral5 = Palette.DmCoral5,
    onCoral = Palette.White,
    amber3 = Palette.DmAmber3,
    amber4 = Palette.Amber3,
    apricot1 = Palette.DmApricot1,
    lapis1 = Palette.DmLapis1,
    lapis3 = Palette.DmLapis3,
    lapis5 = Palette.DmLapis5,
    grey1 = Palette.DmGrey1,
    grey2 = Palette.DmGrey2,
    grey3 = Palette.DmGrey3,
    grey4 = Palette.DmGrey4,
    grey5 = Palette.DmGrey5,
    grey6 = Palette.DmGrey6,
    grey7 = Palette.DmGrey7,
)

val LocalAppColors = staticCompositionLocalOf { LightColors }
