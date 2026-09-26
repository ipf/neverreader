package com.neverreader.app.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neverreader.app.App
import com.neverreader.app.R
import com.neverreader.app.UserManager
import com.neverreader.app.settings.account.AccountManagementActivity
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.SettingsAction
import com.neverreader.ui.compose.SettingsHeader
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The settings screen: account and open-source licenses.
 */
@AndroidEntryPoint
class PrefsFragment : AbsNeverReaderFragment() {

    @Inject
    lateinit var userManager: UserManager

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            com.neverreader.ui.theme.AppTheme {
                SettingsScreen(
                    onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onAccount = {
                        AccountManagementActivity.startActivity(requireContext())
                    },
                    onLogout = { userManager.logout(activity as AbsNeverReaderActivity) },
                    onOpenSourceLicenses = {
                        OpenSourceLicensesActivity.startActivity(requireContext())
                    },
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
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

            SettingsHeader(stringResource(R.string.settings_section_about))
            SettingsAction(R.string.settings_open_source_licenses, onClick = onOpenSourceLicenses)
        }
    }
}
