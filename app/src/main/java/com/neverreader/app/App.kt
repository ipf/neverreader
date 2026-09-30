package com.neverreader.app

import android.app.Application
import android.content.Context
import com.jakewharton.threetenabp.AndroidThreeTen
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.repository.BookmarkRepository
import com.neverreader.app.settings.Theme
import com.neverreader.util.android.Clipboard
import com.neverreader.util.android.FormFactor
import com.neverreader.util.prefs.Preferences
import com.neverreader.sdk.util.AbsNeverReaderActivity
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application(), Configuration.Provider {

    @Inject lateinit var appThreads: AppThreads
    @Inject lateinit var theme: Theme
    @Inject lateinit var legacyPrefs: Preferences
    @Inject lateinit var bookmarkRepository: BookmarkRepository
    @Inject lateinit var accountManager: AccountManager
    @Inject lateinit var userManager: UserManager
    @Inject lateinit var clipboard: Clipboard

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        FormFactor.init(this)
        clearLegacyRotationLock()
        SyncWorker.schedule(this)
    }

    /**
     * The rotation lock is gone. It only ever appeared as an overlay that
     * popped up for a few seconds when you turned the phone, with no settings
     * row anywhere, so there was nothing to miss - but a stored orientation
     * lock would be left behind with nothing reading it, and if it were ever
     *  reintroduced, the user would come back to a lock they never set.
     */
    private fun clearLegacyRotationLock() {
        legacyPrefs.remove(LEGACY_ROTATION_ORIENTATION_KEY)
    }

    fun threads(): AppThreads = appThreads
    fun theme(): Theme = theme
    fun accountManager(): AccountManager = accountManager
    fun userManager(): UserManager = userManager
    fun bookmarks(): BookmarkRepository = bookmarkRepository
    fun clipboard(): Clipboard = clipboard

    companion object {
        const val LEGACY_ROTATION_ORIENTATION_KEY = "orientation"

        fun from(context: Context): App = context.applicationContext as App
    }
}
