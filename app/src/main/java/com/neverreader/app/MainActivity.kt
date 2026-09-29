package com.neverreader.app

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neverreader.app.R
import com.neverreader.app.auth.AuthenticationActivity
import com.neverreader.app.repository.ThumbnailRepository
import com.neverreader.app.settings.isDarkAppTheme
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.sdk.util.AbsNeverReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AbsNeverReaderActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var thumbnailRepository: ThumbnailRepository

    @Inject lateinit var userManager: UserManager

    @Inject lateinit var appPrefs: AppPrefs

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

            // The NavHost pops its own back stack, so there is no activity-level
            // callback to install. BackPressedUtil and the clipboard prompt keep
            // theirs in the base class.
            setAppContent {
                AppNavHost(
                    thumbnailRepository = thumbnailRepository,
                    userManager = userManager,
                    appPrefs = appPrefs,
                    darkTheme = isDarkAppTheme(this@MainActivity),
                )
            }
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                setupEventsObserver()
            }
        }
    }

    /**
     * Nothing to route: the app declares no VIEW filter, so an intent can only be
     * the launch one. Sharing a page in arrives as SEND and is handled by
     * [AddActivity] instead.
     */
    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private suspend fun setupEventsObserver() {
        viewModel.events.onSubscription {
            viewModel.onEventCollectionStarted()
        }.collect { event ->
            when (event) {
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

}
