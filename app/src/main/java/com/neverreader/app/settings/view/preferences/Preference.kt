package com.neverreader.app.settings.view.preferences

import android.view.View
import android.view.View.OnLongClickListener
import com.neverreader.app.settings.AbsPrefsFragment

/**
 * An item and View in [PrefAdapter]. Typically the view is a representation or
 * control of a [AppPrefs].
 */
abstract class Preference(settings: AbsPrefsFragment) : View.OnClickListener, OnLongClickListener {
    /** Used as a dummy preference for the banner view  */
    open class SimplePreference(settings: AbsPrefsFragment) : Preference(settings) {
        override val type: PrefViewType?
        get() {
            return null
        }

        override fun applyToView(view: View?) {
        }

        override val isEnabled: Boolean
        get() {
            return false
        }

        override val isClickable: Boolean
        get() {
            return false
        }

        override fun update(): Boolean {
            return false
        }

        public override fun onClick(v: View?) {
        }

        public override fun onLongClick(v: View?): Boolean {
            return false
        }
    }

    enum class PrefViewType {
        BANNER, HEADER, ACTION, TOGGLE, CACHE_LIMIT, IMPORTANT
    }

    protected val fragment: AbsPrefsFragment

    init {
        if (settings == null) {
            throw NullPointerException("settings cannot be null")
        }
        this.fragment = settings
    }

    /**
     * @return The view type indentifier within [PrefAdapter]. Used for recycling views via [BaseAdapter.getItemViewType].
     */
    abstract val type: PrefViewType?

    /**
     * Setup the view for controlling or showing this [Preference].
     * View will be of type that [PrefAdapter.onCreateViewHolder] creates based on [.getType]
     */
    abstract fun applyToView(view: View?)

    /**
     * @return Whether or not the [Preference] and view is enabled.
     */
    abstract val isEnabled: Boolean

    /**
     * @return Whether or not a pressed state or [OnClickListener] will be allowed on the view that represents this.
     */
    abstract val isClickable: Boolean

    /**
     * Something may have changed related to the status of the [Preference]. If needed,
     * recheck its status.
     *
     * @return true if the setting did change, false if remains the same as before.
     */
    abstract fun update(): Boolean

    /**
     * The view you set up via [.applyToView] was clicked on.
     */
    abstract override fun onClick(v: View?)

    abstract override fun onLongClick(v: View?): Boolean
}
