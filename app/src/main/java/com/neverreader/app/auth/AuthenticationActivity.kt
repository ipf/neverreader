package com.neverreader.app.auth

import android.content.Intent
import android.os.Bundle
import com.neverreader.app.App
import com.neverreader.app.MainActivity
import com.neverreader.app.R
import com.neverreader.backend.model.BackendType
import com.neverreader.sdk.util.AbsNeverReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/** The initial screen for first run of the app. Connects to a Readeck or Wallabag server.  */
@AndroidEntryPoint
class AuthenticationActivity : AbsNeverReaderActivity() {

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
            AuthenticationScreen(onAuthenticated = ::goToMainScreen)
        }
    }

    private fun goToMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
