package com.neverreader.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.SettingsAction
import com.neverreader.ui.compose.SettingsHeader
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import com.neverreader.app.R

/**
 * Account info and logout.
 *
 * This was a whole activity plus a @AndroidEntryPoint fragment, which meant a
 * manifest entry and a fragment container to host it. The settings screen
 * renders it directly.
 */
@Composable
fun AccountScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
) {
    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar and the list drew over each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            navigationIcon = { AppIconButton(onClick = onBack) { UpIcon() } },
            title = {
                Text(
                    text = stringResource(R.string.setting_account_management),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsHeader(stringResource(R.string.settings_section_account))
            SettingsAction(R.string.settings_logout, onClick = onLogout)
        }
    }
}
