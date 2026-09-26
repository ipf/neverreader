package com.neverreader.ui.compose

import androidx.compose.foundation.layout.*
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
import com.neverreader.ui.view.ThinDivider
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import com.neverreader.ui.view.themed.AppTheme

@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    title: @Composable () -> Unit = {},
    actions: @Composable() (RowScope.() -> Unit) = {},
) {
    val navIconBuiltInSpace = 13.dp

    Column(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(AppTheme.dimensions.sideGrid - navIconBuiltInSpace))
            navigationIcon()
            Spacer(Modifier.width(dimensionResource(R.dimen.nr_space_md) - navIconBuiltInSpace))
            TitleContainer(content = title)
            Spacer(Modifier.width(dimensionResource(R.dimen.nr_space_md)))
            Row(content = actions)
        }
        ThinDivider()
    }
}

@Composable
private fun RowScope.TitleContainer(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTextStyle provides AppTheme.typography.h7,
    ) {
        Box(Modifier.weight(1f)) {
            content()
        }
    }
}

@Preview
@Composable
fun AppBarPreview() {
    AppBar(
        Modifier.width(480.dp),
        {
            AppIconButton(onClick = {}) {
                UpIcon()
            }
        },
        { Text("Title") },
    )
}
