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
    private val pref: IntPreference

    init {
        pref = prefs.forUser("appTheme", SYSTEM)
    }

    /**
     * The int key of the current app theme, resolving [SYSTEM] against the
     * configuration of the last context to ask.
     */
    @JvmOverloads
    fun get(context: Context? = null as Context?): Int {
        cachedContext = context ?: cachedContext
        return applyFlagsToTheme(pref.get())
    }

    /**
     * The effective theme, emitting the current value and then on every change.
     *
     * Backed by the preference's own change stream rather than a separate
     * subject, so a write from anywhere in the app is picked up.
     */
    fun observeFor(context: Context?): Flow<Int> =
        pref.withChanges
            .map { get(context) }
            .distinctUntilChanged()

    /**
     * Is the current theme set to a dark variant?
     *
     * @param context
     * @return
     */
    fun isDark(context: Context?): Boolean {
        return isDark(get(context))
    }

    /**
     * Set the current theme setting.
     */
    fun set(theme: Int) {
        pref.set(theme)
    }



    /**
     * Get the bg color for the current theme.
     *
     * @param context
     * @return
     */
    fun getThemeBGColor(context: Context): Int {
        val theme = get(context)
        val res: Int
        when (theme) {
            DARK -> res = R.color.nr_dm_base_bg
            LIGHT -> res = R.color.nr_base_bg
            else -> res = R.color.nr_base_bg
        }

        return ContextCompat.getColor(context, res)
    }

    /** Exposed only for use in a preference screen. Use APIs on this class to modify and query.  */
    fun pref(): IntPreference {
        return pref
    }

    companion object {
        /** Last context seen by [get], used to resolve [SYSTEM]. */
        private var cachedContext: Context? = null

        const val LIGHT: Int = 0
        const val DARK: Int = 1

        /**
         * Follow the system setting. This is the default: the preference had no
         * UI behind it, so it was stuck on [LIGHT] and the app simply had no
         * dark mode even with the system in dark.
         */
        const val SYSTEM: Int = 3

        /** Whether [context] is currently in dark mode, per the system setting. */
        fun isSystemDark(context: Context?): Boolean {
            val mode = (context?.resources?.configuration?.uiMode ?: 0) and Configuration.UI_MODE_NIGHT_MASK
            return mode == Configuration.UI_MODE_NIGHT_YES
        }

        /**
         * Resolves [SYSTEM] here rather than at every use, so the activity theme
         * and Compose agree on one value.
         */
        private fun applyFlagsToTheme(theme: Int): Int =
            if (theme == SYSTEM) {
                if (isSystemDark(cachedContext)) DARK else LIGHT
            } else {
                theme
            }

        /**
         * Is the supplied int value a dark theme variant?
         *
         * @param theme
         * @return
         */
        fun isDark(theme: Int): Boolean {
            return theme == DARK
        }

    }
}

/**
 * The in-app light/dark preference for [context], or false when there is no
 * activity to ask.
 *
 * Compose has to be told this rather than left to default to
 * isSystemInDarkTheme(): the window background comes from the XML theme that the
 * activity picked from this same preference, so when the two disagree the app
 * paints dark surfaces onto a light window.
 */
fun isDarkAppTheme(context: Context?): Boolean {
    val activity = context as? com.neverreader.sdk.util.AbsNeverReaderActivity ?: return false
    return Theme.isDark(activity.currentTheme())
}
