package com.neverreader.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.sdk.util.AbsNeverReaderActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
/**
 * App-level auth orchestration over the [AccountManager].
 *
 * For the app, a "login" is setting the active account and kicking off a sync; a "logout"
 * clears the account and its data.
 */
@Singleton
class UserManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val accountManager: AccountManager
) {
    /**
     * Logout work outlives the Activity that started it, so it runs on a
     * singleton scope rather than one constructed per call and never cancelled.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Whether the user has an active account.
     */
    val isLoggedIn: Boolean
        get() = accountManager.activeCached != null

    /**
     * Logs the user out: clears the account, its data, and finishes login-gated activities.
     */
    fun logout(activity: AbsNeverReaderActivity?) {
        // The application context, not the Activity's: this Activity may already be
        //  finishing or be null when logout is triggered from a screen whose host
        //  is not an AbsNeverReaderActivity.
        scope.launch {
            accountManager.logout()
            SyncWorker.enqueueNow(appContext)
        }
        if (activity != null) {
            startDefaultActivity(activity)
            activity.finishAllActivities(true)
        }
    }

    /**
     * Launches the default, starting Activity.
     */
    fun startDefaultActivity(activity: Activity) {
        activity.startActivity(Intent(activity, defaultActivity))
    }

    val defaultActivity: Class<out Activity> = MainActivity::class.java
}

