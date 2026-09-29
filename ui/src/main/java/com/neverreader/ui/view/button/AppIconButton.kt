package com.neverreader.ui.view.button

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neverreader.ui.theme.AppTheme

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides AppTheme.colors.grey3,
    ) {
        // No long-press tooltip: the label reaches TalkBack through the icon's
        // own contentDescription, which every AppIcons entry supplies, and a
        // long press is a touch gesture rather than an accessibility one.
        IconButton(
            onClick,
            modifier.size(50.dp),
            content = content,
        )
    }
}

@Preview
@Composable
fun UpIconButtonPreview() {
    AppIconButton(onClick = { }) {
        UpIcon()
    }
}
