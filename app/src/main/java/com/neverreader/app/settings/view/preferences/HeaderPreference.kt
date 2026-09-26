package com.neverreader.app.settings.view.preferences

import android.view.View
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.ui.view.menu.SectionHeaderView

/**
 * A non-[AppPrefs] Preference that shows a [HeaderPreferenceView]. Used to display
 * dividing headers within Settings.
 */
class HeaderPreference(
    settings: AbsPrefsFragment,
    private val mLabel: String?,
    private val topDivider: Boolean
) : Preference(settings) {
    override val type: PrefViewType
        get() {
        return PrefViewType.HEADER
    }

    override fun applyToView(layout: View?) {
        val view = layout as SectionHeaderView
        view.bind().label(mLabel).showTopDivider(topDivider).showBottomDivider(false)
            .textAllCaps(true)
    }

    override val isEnabled: Boolean
        get() {
        return false
    }

    /**
     * Recheck the setting
     * @return true if the setting changed, false if remains the same as before.
     */
    override fun update(): Boolean {
        return false
    }

    override fun onClick(v: View?) {}

    override fun onLongClick(v: View?): Boolean {
        return false
    }

    override val isClickable: Boolean
        get() {
        return false
    }
}
