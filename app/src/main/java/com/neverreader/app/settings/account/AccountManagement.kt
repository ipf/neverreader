package com.neverreader.app.settings.account

import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.unit.dp
import com.neverreader.app.R
import com.neverreader.app.UserManager
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.SettingsAction
import com.neverreader.ui.compose.SettingsHeader
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** A thin class to allow [AccountManagementFragment] to be launched as a fullscreen activity. */
class AccountManagementActivity : AbsNeverReaderActivity() {

    override val accessType: ActivityAccessRestriction = ActivityAccessRestriction.ANY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, AccountManagementFragment())
                .commit()
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, AccountManagementActivity::class.java))
        }
    }
}

/** Account info and logout. */
@AndroidEntryPoint
class AccountManagementFragment : AbsNeverReaderFragment() {

    @Inject
    lateinit var userManager: UserManager

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AppTheme {
                AccountScreen(
                    onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    onLogout = { userManager.logout(activity as? AbsNeverReaderActivity) },
                )
            }
        }
    }
}

@Composable
private fun AccountScreen(
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
