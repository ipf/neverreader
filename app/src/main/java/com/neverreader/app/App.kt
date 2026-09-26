package com.neverreader.app

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jakewharton.threetenabp.AndroidThreeTen
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.repository.BookmarkRepository
import com.neverreader.sdk.http.HttpClientDelegate
import com.neverreader.app.settings.Theme
import com.neverreader.sdk.util.wakelock.WakeLockManager
import com.neverreader.util.android.Clipboard
import com.neverreader.app.settings.rotation.RotationLock
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.ui.view.notification.AppSnackbar
import com.neverreader.util.android.IntentUtils
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.CopyOnWriteArraySet
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    init {
        sContext = this
    }

    @Inject lateinit var appThreads: AppThreads
    @Inject lateinit var prefs: AppPrefs
    @Inject lateinit var http: HttpClientDelegate
    @Inject lateinit var bookmarkRepository: BookmarkRepository
    @Inject lateinit var accountManager: AccountManager
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var theme: Theme
    @Inject lateinit var rotationLock: RotationLock
    @Inject lateinit var activities: ActivityMonitor
    @Inject lateinit var clipboard: Clipboard
    @Inject lateinit var wakelocks: WakeLockManager

    private val sOnUserPresenceChangedListeners =
        CopyOnWriteArraySet<OnUserPresenceChangedListener>()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        AppSnackbar.init { _, _, _ -> }
        SyncWorker.schedule(this)
    }

    fun mode(): AppMode = if (BuildConfig.DEBUG) AppMode.DEV else AppMode.PRODUCTION
    fun threads(): AppThreads = appThreads
    fun theme(): Theme = theme
    fun prefs(): AppPrefs = prefs
    fun rotationLock(): RotationLock = rotationLock
    fun accountManager(): AccountManager = accountManager
    fun userManager(): UserManager = userManager
    fun bookmarks(): BookmarkRepository = bookmarkRepository
    fun activities(): ActivityMonitor = activities
    fun clipboard(): Clipboard = clipboard
    fun wakelocks(): WakeLockManager = wakelocks


    interface OnUserPresenceChangedListener {
        fun onUserPresenceChanged(isInApp: Boolean)
    }

    companion object {
        private lateinit var sContext: App
        private val sOnUserPresenceChangedListeners =
            CopyOnWriteArraySet<OnUserPresenceChangedListener>()

        fun from(context: Context): App = context.applicationContext as App

        fun getContext(): App = sContext

        fun addOnUserPresenceChangedListener(listener: OnUserPresenceChangedListener) {
            sOnUserPresenceChangedListeners.add(listener)
        }

        fun removeOnUserPresenceChangedListener(listener: OnUserPresenceChangedListener) {
            sOnUserPresenceChangedListeners.remove(listener)
        }

        var activityContext: AbsNeverReaderActivity? = null

        fun onActivityChange(activity: AbsNeverReaderActivity?) {
            activityContext = activity
        }

        fun setUserPresent(present: Boolean, activity: AbsNeverReaderActivity? = null) {
        }

        fun notifyUserPresenceChanged(present: Boolean) {
            for (listener in sOnUserPresenceChangedListeners) {
                listener.onUserPresenceChanged(present)
            }
        }

        @JvmStatic
        fun getStringResource(id: Int): String? {
            if (id == 0) return null
            return sContext.getString(id)
        }

        fun viewUrl(context: Context, url: String, showDialogOnFail: Boolean = true): Boolean {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return if (IntentUtils.isActivityIntentAvailable(context, intent)) {
                context.startActivity(intent)
                true
            } else if (showDialogOnFail) {
                AlertDialog.Builder(context)
                    .setTitle(R.string.dg_browser_not_found_t)
                    .setMessage(R.string.dg_browser_not_found_m)
                    .setNeutralButton(R.string.ac_ok, null)
                    .show()
                false
            } else {
                false
            }
        }
    }
}
