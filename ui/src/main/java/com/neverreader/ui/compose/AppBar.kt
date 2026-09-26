package com.neverreader.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.ThinDivider
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon

/**
 * The top app bar. Height is pinned to `nr_app_bar_height` (56dp) so it matches
 * the settings screen, which still uses the XML bar.
 */
@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
) {
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen.nr_app_bar_height)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(AppTheme.dimensions.sideGrid))
            navigationIcon()
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CompositionLocalProvider(LocalTextStyle provides AppTheme.typography.h7) {
                    title()
                }
            }
            actions()
            Spacer(Modifier.width(AppTheme.dimensions.sideGrid))
        }
        ThinDivider()
    }
}

@Preview
@Composable
private fun AppBarPreview() {
    AppTheme {
        AppBar(
            Modifier.width(480.dp),
            navigationIcon = { AppIconButton(onClick = {}) { UpIcon() } },
            title = { Text("Saves") },
        )
    }
}
