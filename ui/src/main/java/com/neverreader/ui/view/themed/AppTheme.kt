package com.neverreader.ui.view.themed

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.neverreader.ui.NeverReaderDimensions
import com.neverreader.ui.text.Graphik
import com.neverreader.ui.text.LocalNeverReaderTypography
import com.neverreader.ui.text.AppTypography

@Composable
fun AppTheme(
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val themeColors = themeColors(context)
    val appColors = when (themeColors) {
        ThemeColors.LIGHT -> LightColors
        else -> DarkColors
    }

    val appTypography = AppTypography().withDefaultFontFamily(Graphik(context.assets))

    MaterialTheme(
        colorScheme = when (themeColors) {
            ThemeColors.LIGHT -> lightColorScheme()
            else -> darkColorScheme()
        }
            .copy(
                primary = appColors.teal2,
                background = appColors.background,
                surface = appColors.background,
                surfaceContainerHigh = appColors.background,
                surfaceVariant = appColors.grey7,
                onBackground = appColors.onBackground,
                onSurface = appColors.onBackground,
                onSurfaceVariant = Color.Black,
            ),
    ) {
        CompositionLocalProvider(
            LocalNeverReaderColors provides appColors,
            LocalNeverReaderTypography provides appTypography,
            LocalContentColor provides appColors.onBackground,
            LocalTextStyle provides appTypography.p1,
            content = content
        )
    }
}

object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalNeverReaderColors.current
    val typography: AppTypography
        @Composable
        get() = LocalNeverReaderTypography.current
    val dimensions: NeverReaderDimensions
        get() = NeverReaderDimensions
}

@Composable
private fun themeColors(context: Context): ThemeColors? {
    val themed = AppThemeUtil.findThemed(context)
    val themeColors by themed?.getThemeColorsChanges(context)
        ?.subscribeAsState(initial = themed.getThemeColors(context))
        ?: (if (isSystemInDarkTheme()) ThemeColors.DARK else ThemeColors.LIGHT).let { remember { mutableStateOf(it) } }
    return themeColors
}
