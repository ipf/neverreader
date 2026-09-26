package com.neverreader.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.neverreader.app.R
import com.neverreader.app.auth.AuthenticationActivity
import com.neverreader.app.list.MyListFragment
import com.neverreader.app.reader.Reader
import com.neverreader.app.settings.PrefsFragment
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.view.notification.AppSnackbar
import com.neverreader.util.BackPressedUtil
import com.neverreader.util.android.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AbsNeverReaderActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val navHostFragment: NavHostFragment?
        get() = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? NavHostFragment

    private val navController: NavController?
        get() = navHostFragment?.navController

    override val accessType: ActivityAccessRestriction = ActivityAccessRestriction.ANY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First start: redirect to the server setup screen
        if (App.from(this).userManager.isLoggedIn.not()) {
            startActivity(Intent(this, AuthenticationActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupEventsObserver()
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (BackPressedUtil.onBackPressed(supportFragmentManager)) return
            if (navController?.popBackStack() != true) {
                isEnabled = false
                this@MainActivity.onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    private suspend fun setupEventsObserver() {
        viewModel.events.onSubscription {
            viewModel.onEventCollectionStarted()
        }.collect { event ->
            when (event) {
                is MainViewModel.Event.GoToSaves -> {
                    val current = currentFragment
                    if (current !is MyListFragment) {
                        navController?.popBackStack(R.id.saves, false)
                    }
                }
                is MainViewModel.Event.GoToSettings -> {
                    if (currentFragment !is PrefsFragment) {
                        navController?.navigate(R.id.settings)
                    }
                }
                is MainViewModel.Event.OpenReader -> {
                    // handled by the list fragment navigating to the reader
                }
                is MainViewModel.Event.ShowBadCredentialsToast -> {
                    val snackbar = AppSnackbar.make(
                        this,
                        AppSnackbar.Type.DEFAULT_DISMISSABLE,
                        getString(R.string.dg_forced_logout_m),
                        null,
                    )
                    snackbar.show()
                }
            }
        }
    }

    private val currentFragment: Fragment?
        get() = navHostFragment?.childFragmentManager?.primaryNavigationFragment
}
