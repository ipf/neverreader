package com.neverreader.app.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.mikepenz.aboutlibraries.Libs
import kotlinx.serialization.json.Json
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
    // The overload that loaded the asset itself is deprecated: the list has to be
    // decoded and handed in. It comes from the raw resource the
    // aboutlibraries plugin generates.
    val context = LocalContext.current
    val libraries = remember(context) { loadLibraries(context) }

    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar and the list drew on top of each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            navigationIcon = { AppIconButton(onClick = onBack) { UpIcon() } },
            title = { Text(stringResource(R.string.setting_oss)) },
        )
        if (libraries == null) {
            // A malformed or missing asset should not take the screen down.
            Text(stringResource(R.string.setting_oss))
        } else {
            LibrariesContainer(
                libraries = libraries,
                modifier = Modifier.fillMaxSize(),
                showVersion = false,
            )
        }
    }
}

@Preview
@Composable
private fun OpenSourceLicensesScreenPreview() {
    AppTheme { OpenSourceLicensesScreen(onBack = {}) }
}

/**
 * The plugin's generated asset, decoded into the list the container renders.
 * Null rather than a throw: a missing or malformed asset should leave the
 * screen empty, not crash it.
 */
private fun loadLibraries(context: Context): Libs? = runCatching {
    val json = context.resources.openRawResource(R.raw.aboutlibraries)
        .bufferedReader()
        .use { it.readText() }
    Json.decodeFromString(Libs.serializer(), json)
}.getOrNull()
