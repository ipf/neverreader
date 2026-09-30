package com.neverreader.app.settings

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.ui.R
import com.neverreader.util.prefs.IntPreference
import com.neverreader.util.prefs.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Theme @Inject constructor(prefs: Preferences) {
    private val pref: IntPreference = prefs.forUser("appTheme", SYSTEM)

    /**
     * The int key of the current app theme, resolving [SYSTEM] against [context].
     *
     * The context used to be optional and stashed in a `companion object` field
     * to resolve [SYSTEM] later. That was a static holding on to the last
     * Activity that asked - and every activity did, in `onCreate` - so it pinned
     * a destroyed Activity for the process's lifetime, and could read back a
     * dead one for the answer. The resolution is now a pure function of the
     * context handed in.
     */
    fun get(context: Context): Int = resolve(pref.get(), context)

    /**
     * The effective theme, emitting the current value and then on every change.
     *
     * Backed by the preference's own change stream rather than a separate
     * subject, so a write from anywhere in the app is picked up.
     *
     * This covers preference changes only. A system dark-mode switch does not
     * move the preference, so it does not emit: callers that need to follow the
     * system also have to listen for configuration changes.
     */
    fun observeFor(context: Context): Flow<Int> =
        pref.withChanges
            // null is the preference being cleared, which is the default.
            .map { resolve(it ?: SYSTEM, context) }
            .distinctUntilChanged()

    /**
     * Set the current theme setting.
     */
    fun set(theme: Int) {
        pref.set(theme)
    }

    /**
     * Get the bg color for the current theme.
     */
    fun getThemeBGColor(context: Context): Int {
        val res = when (resolve(pref.get(), context)) {
            DARK -> R.color.nr_dm_base_bg
            else -> R.color.nr_base_bg
        }
        return ContextCompat.getColor(context, res)
    }

    companion object {
        const val LIGHT: Int = 0
        const val DARK: Int = 1

        /**
         * Follow the system setting. This is the default: the preference had no
         * UI behind it, so it was stuck on [LIGHT] and the app simply had no
         * dark mode even with the system in dark.
         */
        const val SYSTEM: Int = 3

        /** Whether [context] is currently in dark mode, per the system setting. */
        fun isSystemDark(context: Context): Boolean =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        /**
         * Resolves [SYSTEM] here rather than at every use, so the activity theme
         * and Compose agree on one value.
         */
        private fun resolve(theme: Int, context: Context): Int =
            if (theme == SYSTEM) {
                if (isSystemDark(context)) DARK else LIGHT
            } else {
                theme
            }

        /** Is the supplied int value a dark theme variant? */
        fun isDark(theme: Int): Boolean = theme == DARK
    }
}

/**
 * The in-app light/dark preference for [context], or false when there is no
 * activity to ask.
 *
 * Compose has to be told this rather than left to default to
 * isSystemInDarkTheme(): the window background comes from the XML theme that the
 * activity picked from this same preference, so when the two disagree, the app
 * paints dark surfaces onto a light window.
 */
fun isDarkAppTheme(context: Context?): Boolean {
    val activity = context as? AbsNeverReaderActivity ?: return false
    return Theme.isDark(activity.currentTheme())
}
