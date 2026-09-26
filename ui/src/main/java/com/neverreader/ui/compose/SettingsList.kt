package com.neverreader.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.ThinDivider

/**
 * A settings row that navigates or performs an action.
 */
@Composable
fun SettingsAction(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = AppTheme.dimensions.sideGrid,
                vertical = AppTheme.dimensions.spaceSmall,
            ),
    ) {
        Text(text = title, style = AppTheme.typography.p3, color = AppTheme.colors.grey1)
        if (summary != null) {
            Text(
                text = summary,
                style = AppTheme.typography.p4,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}

/** Convenience wrapper for rows whose title is a string resource. */
@Composable
fun SettingsAction(
    titleRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    SettingsAction(stringResource(titleRes), onClick, modifier, summary)
}

/** A section title, with a rule above it. */
@Composable
fun SettingsHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        ThinDivider()
        Text(
            text = title,
            style = AppTheme.typography.p4,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier.padding(
                horizontal = AppTheme.dimensions.sideGrid,
                vertical = AppTheme.dimensions.spaceSmall,
            ),
        )
    }
}

@Preview
@Composable
private fun SettingsPreview() {
    AppTheme {
        Column {
            SettingsHeader("Account")
            SettingsAction(title = "Account", onClick = {})
            SettingsAction(title = "Log out", onClick = {})
        }
    }
}
