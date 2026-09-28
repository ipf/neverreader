package com.neverreader.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import com.neverreader.app.R
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.theme.AppTheme

/**
 * The bundled-licences list.
 *
 * This was a whole activity plus a @AndroidEntryPoint fragment to show one
 * composable, which meant declaring the activity in the manifest and hosting a
 * fragment container for it. The settings screen now renders it directly.
 */
@Composable
fun OpenSourceLicensesScreen(onBack: () -> Unit) {
    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar and the list drew on top of each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            navigationIcon = { AppIconButton(onClick = onBack) { UpIcon() } },
            title = { Text(stringResource(R.string.setting_oss)) },
        )
        LibrariesContainer(
            Modifier.fillMaxSize(),
            showVersion = false,
        )
    }
}

@Preview
@Composable
private fun OpenSourceLicensesScreenPreview() {
    AppTheme { OpenSourceLicensesScreen(onBack = {}) }
}
