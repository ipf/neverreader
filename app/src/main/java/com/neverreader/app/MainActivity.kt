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
import com.neverreader.app.settings.PrefsFragment
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.util.BackPressedUtil
import com.neverreader.util.android.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AbsNeverReaderActivity() {

    private val viewModel: MainViewModel by viewModels()

    /**
     * Held directly rather than looked up by view id: there is no
     * FragmentContainerView in a layout any more, so the container is created in
     * code and the graph attached afterwards.
     */
    private val navHost = NavHostFragment()

    private val navController: NavController?
        get() = navHost.navController

    override val accessType: ActivityAccessRestriction = ActivityAccessRestriction.ANY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The active account lives in Room and is read asynchronously, so asking
        // whether we are logged in here raced that read: on a cold start
        // activeCached was still null and a signed-in user was dumped on the setup
        // screen. Wait for the row instead. The window background covers the gap.
        lifecycleScope.launch {
            if (App.from(this@MainActivity).accountManager.active() == null) {
                startActivity(Intent(this@MainActivity, AuthenticationActivity::class.java))
                finish()
                return@launch
            }

            // The graph cannot be set before the fragment exists, and
            // NavHostFragment would normally read it from activity_main.xml.
            hostFragment(navHost) {
                supportFragmentManager.executePendingTransactions()
                navController?.setGraph(R.navigation.main_graph)
            }
            onBackPressedDispatcher.addCallback(this@MainActivity, backPressedCallback)
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
                is MainViewModel.Event.ShowBadCredentialsToast ->
                    // Long and dismissable: the user has been logged out and has to
                    // read this before signing in again.
                    snack(
                        message = getString(R.string.dg_forced_logout_m),
                        long = true,
                    )
            }
        }
    }

    private val currentFragment: Fragment?
        get() = navHost.childFragmentManager.primaryNavigationFragment
}
