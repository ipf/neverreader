package com.neverreader.app.settings.view.preferences

import android.util.SparseArray
import android.view.View
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.PreferenceViews.EnabledCondition
import com.neverreader.ui.view.settings.SettingsSwitchView

class ToggleSwitchPreference(
    private val mSettings: AbsPrefsFragment,
    private val pref: PrefHandler,
    private val label: String,
    private val summary: SparseArray<CharSequence?>?,
    private val listener: OnChangeListener?,
    private val enabledCondition: EnabledCondition?,
    private val uiEntityIdentifier: String?
) : Preference(mSettings) {
    interface OnChangeListener {
        /**
         * Called when the preference is requested to change.
         * @param view The view that triggered the change
         * @param nowEnabled
         * @return true if allowed to change, false if not
         */
        fun onChange(view: View?, nowEnabled: Boolean): Boolean

        /**
         * After the value has changed.
         *
         * @param nowEnabled
         */
        fun afterChange(nowEnabled: Boolean)
    }

    interface PrefHandler {
        fun get(): Boolean
        fun set(value: Boolean)
    }

    private var isChecked: Boolean

    init {
        this.isChecked = pref.get()
    }

    private fun getSummary(): CharSequence? {
        if (summary == null || summary.size() == 0) {
            return null
        } else {
            var sum: CharSequence? = null
            if (isEnabled) {
                if (isChecked) {
                    sum = summary.get(ActionPreference.Companion.SUMMARY_CHECKED)
                }
            } else {
                sum = summary.get(ActionPreference.Companion.SUMMARY_UNAVAILABLE)
            }

            if (sum == null) {
                sum = summary.get(ActionPreference.Companion.SUMMARY_DEFAULT_OR_UNCHECKED)
            }
            return sum
        }
    }

    override val type: PrefViewType
        get() {
        return PrefViewType.TOGGLE
    }

    override fun applyToView(layout: View?) {
        val view = layout as SettingsSwitchView
        view.bind().isToggle(true).title(label).subtitle(getSummary())
            .checked(isEnabled && isChecked)
        if (uiEntityIdentifier != null) {
            view.uiEntityIdentifier = uiEntityIdentifier
        }
    }

    override val isEnabled: Boolean
        get() {
        if (enabledCondition != null) {
            return enabledCondition.isTrue
        } else {
            return true
        }
    }

    override val isClickable: Boolean
        get() {
        return true
    }

    override fun update(): Boolean {
        val newVal = pref.get()
        if (newVal != isChecked) {
            isChecked = newVal
            return true
        } else {
            return false
        }
    }

    public override fun onClick(v: View?) {
        val newValue = !isChecked
        val allowed = listener == null || listener.onChange(v, newValue)

        if (allowed) {
            isChecked = newValue
            pref.set(newValue)

            if (listener != null) {
                listener.afterChange(newValue)
            }

            mSettings.onPreferenceChange(true)
        }
    }

    public override fun onLongClick(v: View?): Boolean {
        return false
    }
}
