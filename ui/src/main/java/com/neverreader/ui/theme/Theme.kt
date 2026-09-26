package com.neverreader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.neverreader.ui.NeverReaderDimensions

/**
 * The app theme.
 *
 * `darkTheme` is a parameter rather than being read from a singleton so the
 * in-app light/dark preference wins over the system setting, and so previews and
 * tests can pin a mode. The legacy `Theme` pref only had LIGHT and DARK; the
 * system default is now a real option.
 */
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val assets = currentAssetManager
    val typography = remember(assets) {
        AppTypography().withFontFamily(AppFontFamily.of(assets))
    }

    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()
            .copy(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.background,
                onSurface = colors.onBackground,
                surfaceVariant = colors.grey7,
                // Was hardcoded to Color.Black, which made every secondary label
                // invisible in dark mode.
                onSurfaceVariant = colors.textSecondary,
                outline = colors.divider,
                outlineVariant = colors.grey6,
                error = colors.coral2,
                secondary = colors.teal3,
                tertiary = colors.amber3,
                surfaceTint = colors.primary,
            ),
        typography = typography.toMaterialTheme(),
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(
            LocalAppColors provides colors,
            LocalAppTypography provides typography,
            LocalContentColor provides colors.onBackground,
            LocalTextStyle provides typography.p3,
            content = content,
        )
    }
}

/**
 * Shorthand for `AppTheme.colors`, so call sites read `AppTheme.colors.grey1`
 * rather than reaching for a `CompositionLocal`.
 */
object AppTheme {
    val colors: AppColors
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    val dimensions: NeverReaderDimensions
        get() = NeverReaderDimensions
}

internal val currentAssetManager
    @Composable @ReadOnlyComposable
    get() = LocalContext.current.assets
