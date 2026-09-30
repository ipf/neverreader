package com.neverreader.app.auth

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neverreader.app.App
import com.neverreader.app.MainActivity
import com.neverreader.sdk.util.AbsNeverReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * The initial screen for first run of the app. Connects to a Readeck or Wallabag
 * server.
 *
 * Also the destination of Readeck's OAuth redirect, so it has to cope with
 * arriving that way twice over: cold-started from the browser while the app was
 * not running, and brought forward via `onNewIntent` when it already was.
 */
@AndroidEntryPoint
class AuthenticationActivity : AbsNeverReaderActivity() {

    private val viewModel: AuthenticationViewModel by viewModels()

    override fun checkClipboardForUrl() {
        // Do not check in this Activity
    }

    override val accessType: ActivityAccessRestriction
        get() = ActivityAccessRestriction.LOGIN_ACTIVITY

    override fun onRestart() {
        super.onRestart()
        if (App.from(this).userManager.isLoggedIn) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setAppContent {
            AuthenticationScreen(onOpenUrl = ::openInBrowser)
        }

        // Deliver the redirect before the screen starts observing, so a cold
        // start from the browser does not sit on the setup screen with a
        // sign-in already completed behind it.
        handleRedirect(intent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is AuthenticationViewModel.Event.Success -> goToMainScreen()
                        is AuthenticationViewModel.Event.OpenBrowser -> openInBrowser(event.url)
                    }
                }
            }
        }
    }

    /**
     * `singleTask` means the running instance is reused rather than a new one
     * being created, so a second redirect arrives here.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleRedirect(intent)
    }

    private fun handleRedirect(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != CALLBACK_SCHEME) return
        viewModel.onAuthorizationRedirect(data)
    }

    private fun openInBrowser(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure {
                // No browser, or none that will take it. The device-code
                // fallback is on the setup screen for exactly this.
                viewModel.onBrowserUnavailable()
            }
    }

    private fun goToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        /** Must match [com.neverreader.backend.readeck.ReadeckAuth.REDIRECT_URI]. */
        private const val CALLBACK_SCHEME = "com.neverreader.app"
    }
}
