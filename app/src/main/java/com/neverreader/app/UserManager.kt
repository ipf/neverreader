package com.neverreader.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
    @ApplicationContext context: Context?,
    private val accountManager: AccountManager
) : AppLifecycle {
    val isLoggedIn: Boolean
        /**
         * Whether the user has an active account.
         */
        get() = accountManager.activeCached != null

    /**
     * Call after an auth flow has saved the active account; kicks off a sync.
     */
    fun onAuthSuccess(context: Context?) {
        SyncWorker.enqueueNow(context!!)
    }

    /**
     * Logs the user out: clears the account, its data, and finishes login-gated activities.
     */
    fun logout(activity: AbsNeverReaderActivity?) {
        threads_logout(activity)
    }

    private fun threads_logout(activity: AbsNeverReaderActivity?) {
        val context = activity?.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            accountManager.logout()
            SyncWorker.enqueueNow(context!!)
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
        val activityClass = this.defaultActivity
        activity.startActivity(Intent(activity, activityClass))
    }

    val defaultActivity: Class<out Activity>
        /**
         * The activity to launch by default.
         */
        get() = MainActivity::class.java


    fun hasDeletedAccount(): Boolean {
        return false
    }

    fun onShowedDeletedAccountToast() {
    }

    fun hadBadCredentials(): Boolean {
        return false
    }

    fun onShowedBadCredentialsMessage() {
    }

    fun enableSignedOutExperience() {
    }
}
