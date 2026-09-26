package com.neverreader.app.auth

import android.content.Intent
import android.os.Bundle
import com.neverreader.app.App
import com.neverreader.app.MainActivity
import com.neverreader.app.R
import com.neverreader.backend.model.BackendType
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.view.notification.AppSnackbar
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

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, AuthenticationFragment())
                .commit()
        }
    }
}
