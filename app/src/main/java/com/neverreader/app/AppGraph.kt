package com.neverreader.app

import com.neverreader.app.reader.internal.article.DisplaySettingsManager
import com.neverreader.app.settings.SystemDarkTheme
import com.neverreader.app.settings.Theme
import com.neverreader.app.settings.UserAgent
import com.neverreader.app.settings.rotation.RotationLock
import com.neverreader.repository.BookmarkRepository
import com.neverreader.sdk.http.HttpClientDelegate
import com.neverreader.sdk.image.ImageCache
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.sdk.util.wakelock.WakeLockManager
import com.neverreader.util.android.Clipboard

/**
 * The app's main component accessors.
 *
 * @deprecated use dagger/hilt dependency injection instead
 */
@Deprecated
interface AppGraph {

    fun mode(): AppMode
    fun theme(): Theme
    fun systemDarkTheme(): SystemDarkTheme
    fun activities(): ActivityMonitor
    fun dispatcher(): AppLifecycleEventDispatcher
    fun threads(): AppThreads
    fun clipboard(): Clipboard
    fun imageCache(): ImageCache
    fun http(): HttpClientDelegate
    fun prefs(): AppPrefs
    fun displaySettings(): DisplaySettingsManager
    fun rotationLock(): RotationLock
    fun userAgent(): UserAgent
    fun device(): Device
    fun appOpen(): AppOpen
    fun wakelocks(): WakeLockManager
    fun bookmarks(): BookmarkRepository
}
