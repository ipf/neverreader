package com.neverreader.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

/**
 * A settings row that flips a boolean preference.
 *
 * Takes the preference rather than a value so the row stays in sync with whatever
 * else writes to it, without the screen having to hold its own state.
 */
@Composable
fun SettingsToggle(
    titleRes: Int,
    pref: com.neverreader.util.prefs.BooleanPreference,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val value = pref.withChanges.collectAsState(initial = pref.get()).value == true
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { pref.set(!value) }
            .padding(
                horizontal = AppTheme.dimensions.sideGrid,
                vertical = AppTheme.dimensions.spaceSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(titleRes),
                style = AppTheme.typography.p3,
                color = AppTheme.colors.grey1,
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = AppTheme.typography.p4,
                    color = AppTheme.colors.textSecondary,
                )
            }
        }
        Spacer(Modifier.width(AppTheme.dimensions.spaceSmall))
        Switch(checked = value, onCheckedChange = { pref.set(it) })
    }
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
