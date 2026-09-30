package com.neverreader.app.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.neverreader.app.R
import com.neverreader.app.UserManager
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.SettingsAction
import com.neverreader.ui.compose.SettingsHeader
import com.neverreader.ui.compose.SettingsToggle
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon

private enum class SettingsOverlay { Account, Licenses }

/**
 * The settings screen: account, privacy, and open-source licenses. A navigation
 * destination, with the sub-screens as overlays rather than separate activities.
 */
@Composable
fun SettingsScreen(
    appPrefs: AppPrefs,
    userManager: UserManager,
    onBack: () -> Unit,
) {
    // The two sub-screens used to be separate activities, each with a manifest
    // entry and a fragment container to host it. They are one composable each.
    // Hoisted: LocalContext is composable, and these lambdas are not.
    val activity = LocalContext.current as? AbsNeverReaderActivity
    var overlay by remember { mutableStateOf<SettingsOverlay?>(null) }
    if (overlay != null) {
        BackHandler { overlay = null }
    }
    when (overlay) {
        SettingsOverlay.Account -> AccountScreen(
            onBack = { overlay = null },
            onLogout = { userManager.logout(activity) },
        )

        SettingsOverlay.Licenses -> OpenSourceLicensesScreen(
            onBack = { overlay = null },
        )

        null -> SettingsList(
            appPrefs = appPrefs,
            onBack = onBack,
            onAccount = { overlay = SettingsOverlay.Account },
            onLogout = { userManager.logout(activity) },
            onOpenSourceLicenses = { overlay = SettingsOverlay.Licenses },
        )
    }
}

@Composable
private fun SettingsList(
    appPrefs: AppPrefs,
    onBack: () -> Unit,
    onAccount: () -> Unit,
    onLogout: () -> Unit,
    onOpenSourceLicenses: () -> Unit,
) {
    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar and the list drew over each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            navigationIcon = { AppIconButton(onClick = onBack) { UpIcon() } },
            title = {
                Text(
                    text = stringResource(R.string.settings_title),
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
            SettingsAction(R.string.settings_account, onClick = onAccount)
            SettingsAction(R.string.settings_logout, onClick = onLogout)

            SettingsHeader(stringResource(R.string.settings_section_privacy))
            SettingsToggle(
                titleRes = R.string.settings_load_third_party_images,
                pref = appPrefs.LOAD_THIRD_PARTY_IMAGES,
            )

            SettingsHeader(stringResource(R.string.settings_section_about))
            SettingsAction(R.string.settings_open_source_licenses, onClick = onOpenSourceLicenses)
        }
    }
}
