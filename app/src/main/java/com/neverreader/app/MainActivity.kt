package com.neverreader.app

import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.neverreader.app.auth.AuthenticationActivity
import com.neverreader.app.repository.ThumbnailRepository
import com.neverreader.app.settings.isDarkAppTheme
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.sdk.util.AbsNeverReaderActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AbsNeverReaderActivity() {

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
            // callback to install. The clipboard prompt keeps its hook in the
            // base class.
            setAppContent {
                AppNavHost(
                    thumbnailRepository = thumbnailRepository,
                    userManager = userManager,
                    appPrefs = appPrefs,
                    darkTheme = isDarkAppTheme(this@MainActivity),
                )
            }
        }
    }

    /**
     * Nothing to route: the app declares no VIEW filter, so intent can only be
     * the launch one. Sharing a page in arrives as SEND and is handled by
     * [com.neverreader.app.add.AddActivity] instead.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}
