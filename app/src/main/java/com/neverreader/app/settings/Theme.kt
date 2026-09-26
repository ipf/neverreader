package com.neverreader.app.settings

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderActivity.Companion.from
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.R
import com.neverreader.util.prefs.IntPreference
import com.neverreader.util.prefs.Preferences
import io.reactivex.Observable
import io.reactivex.functions.Function
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Theme @Inject constructor(prefs: Preferences) {
    private val changes: Subject<Any?> = PublishSubject.create<Any?>()
    private val change = Any()
    private val pref: IntPreference

    init {
        pref = prefs.forUser("appTheme", LIGHT)
    }

    /**
     * Get the int key of the current app theme.
     *
     * @param context The context the themed view will display in. This allows for checks to see if the view allows certain Themes.
     * @return
     */
    // OPT there are some drawing and layout operations that hit this, is that ok?
    /**
     * Get the int key of the current app theme. This will not check if a theme is allowed in this context. When asking
     * on behalf of a View or Activity, use get(Context) instead.
     *
     * @return
     */
    @JvmOverloads
    fun get(context: Context? = null as Context?): Int {
        // Get the current setting
        val theme = pref.get()


        // If a context is available, use it to determine if any themes are not allowed in this context.
        var allowedFlag: Int = FLAG_ALLOW_ALL
        val activity = from(context)
        if (activity != null) {
            allowedFlag = activity.themeFlag
        }

        return applyFlagsToTheme(theme, allowedFlag)
    }

    /**
     * Get the int key of the current app theme.
     *
     * @param view This will search for the PageFragment that this view belongs to and check what themes are allowed for it.
     * @param frag If the PageFragment is already known pass it here.
     * @return int key of the current app theme
     */
    @JvmOverloads
    fun get(view: View, frag: Fragment? = null): Int {
        var frag = frag
        if (view.isInEditMode()) return LIGHT

        // Get the current setting
        val theme = pref.get()


        // If a context is available, use it to determine if any themes are not allowed in this context.
        var allowedFlag: Int = FLAG_ALLOW_ALL
        val activity = AbsNeverReaderActivity.from(view.getContext())
        if (activity != null) {
            if (frag == null) {
                frag = activity.getFragmentParentOfView(view)
            }
            if (frag is AbsNeverReaderFragment) {
                allowedFlag = frag.themeFlag
            } else {
                // This could happen because the view has not been added yet, if the view is created and then added, but then doesn't have its drawable state refreshed, or if the fragment isn't a AbsNeverReaderFragment.
                // Just use the default for the context
                return get(view.getContext())
            }
        }

        return applyFlagsToTheme(theme, allowedFlag)
    }

    fun observeFor(context: Context?): Observable<Int?>? {
        return changes.map<Int?>(Function { `__`: Any? -> get(context) }).distinctUntilChanged()
    }

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
        changes.onNext(change)
    }

    /**
     * Get the current theme represented by a view state int[]
     *
     * @param forView
     * @return
     */
    fun getState(forView: View): IntArray {
        return getState(get(forView, null))
    }

    /**
     * Get the current theme represented by a view state int[]
     *
     * @param forView
     * @params frag If the parent PageFragment is known, pass it here. TODO this whole thing is a bloody hack, need to find a easier way to handle this.
     * @return
     */
    fun getState(forView: View, frag: AbsNeverReaderFragment?): IntArray {
        return getState(get(forView, frag))
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

        return context.getResources().getColor(res)
    }

    /** Exposed only for use in a preference screen. Use APIs on this class to modify and query.  */
    fun pref(): IntPreference {
        return pref
    }

    companion object {
        const val LIGHT: Int = 0
        const val DARK: Int = 1

        // OPT this would be a good place to use bitwise ops instead
        const val FLAG_ALLOW_ALL: Int = 0
        const val FLAG_ONLY_DARK: Int = 1
        const val FLAG_ONLY_LIGHT: Int = 2

        val STATE_LIGHT: IntArray = intArrayOf(R.attr.state_light)
        val STATE_DARK: IntArray = intArrayOf(R.attr.state_dark)

        private fun applyFlagsToTheme(theme: Int, flag: Int): Int {
            when (flag) {
                FLAG_ONLY_DARK -> return DARK
                FLAG_ONLY_LIGHT -> return LIGHT

                else -> return theme
            }
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

        /**
         * Get the theme for the supplied int key, represented by a view state int[]
         *
         * @param theme
         * @return
         */
        fun getState(theme: Int): IntArray {
            when (theme) {
                DARK -> return STATE_DARK
                LIGHT -> return STATE_LIGHT
                else -> return STATE_LIGHT
            }
        }

        fun isDark(drawableState: IntArray): Boolean {
            val len = drawableState.size
            var state: Int
            for (i in 0..<len) {
                state = drawableState[i]
                if (state == STATE_DARK[0]) {
                    return true
                }
            }
            return false
        }

        /**
         * Returns the status bar color to use for the provided theme.
         */
        fun getStatusBarColor(theme: Int, context: Context): Int {
            when (theme) {
                DARK -> return ContextCompat.getColor(context, R.color.nr_dm_base_bg)
                LIGHT -> return ContextCompat.getColor(context, R.color.nr_base_bg)
                else -> return ContextCompat.getColor(context, R.color.nr_base_bg)
            }
        }

        fun getNavigationBarDividerColor(theme: Int, context: Context): Int {
            when (theme) {
                DARK -> return ContextCompat.getColor(context, R.color.nr_dm_grey_6)
                LIGHT -> return ContextCompat.getColor(context, R.color.nr_grey_6)
                else -> return ContextCompat.getColor(context, R.color.nr_grey_6)
            }
        }
    }
}
