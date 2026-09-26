package com.neverreader.app.settings.view.preferences

import android.util.SparseArray
import android.view.View
import android.view.View.OnLongClickListener
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.PreferenceViews.EnabledCondition
import com.neverreader.ui.view.settings.SettingsSwitchView

/**
 * This is a basic [Preference] that can register a simple click listener.
 */
open class ActionPreference constructor(
    settings: AbsPrefsFragment,
    label: String?,
    summary: SparseArray<CharSequence?>?,
    action: OnClickAction?,
    longPressAction: OnClickAction?,
    condition: EnabledCondition?,
    identifier: String?
) : Preference(settings), OnLongClickListener {
    protected val label: String
    protected val summary: SparseArray<CharSequence?>?

    private val enabledCondition: EnabledCondition?
    private val action: OnClickAction?
    private val longPressAction: OnClickAction?

    protected val uiEntityIdentifier: String?

    /** Use [PreferenceViews] instead.  */
    init {
        if (label == null) {
            throw NullPointerException("label cannot be null")
        }
        this.label = label
        this.summary = summary
        enabledCondition = condition
        this.action = action
        this.longPressAction = longPressAction
        uiEntityIdentifier = identifier
    }

    open fun getSummary(): CharSequence? {
        return if (summary == null || summary.size() == 0) {
            null
        } else {
            if (isEnabled) {
                summary.get(SUMMARY_DEFAULT_OR_UNCHECKED)
            } else {
                val sum: CharSequence? = summary.get(SUMMARY_UNAVAILABLE)
                sum ?: summary.get(SUMMARY_DEFAULT_OR_UNCHECKED)
            }
        }
    }

    fun updateSummary(key: Int, summary: CharSequence?): ActionPreference {
        this.summary!!.put(key, summary)
        return this
    }

    override fun onClick(v: View?) {
        action?.onClick()
    }

    override fun onLongClick(v: View?): Boolean {
        if (longPressAction != null) {
            longPressAction.onClick()
            return true
        } else {
            return false
        }
    }

    override val type: PrefViewType?
        get() = PrefViewType.ACTION

    fun interface OnClickAction {
        fun onClick()
    }

    override fun applyToView(layout: View?) {
        val view = layout as SettingsSwitchView
        view.bind().isToggle(false).title(label).subtitle(getSummary())
        if (uiEntityIdentifier != null) {
            view.uiEntityIdentifier = uiEntityIdentifier
        }
    }

    override val isEnabled: Boolean
        get() {
            return if (!isClickable) {
                false
            } else enabledCondition?.isTrue ?: true
    }

    override val isClickable: Boolean
        get() {
        return action != null
    }

    override fun update(): Boolean {
        return false
    }

    companion object {
        var SUMMARY_UNAVAILABLE: Int = -1
        var SUMMARY_DEFAULT_OR_UNCHECKED: Int = 0
        var SUMMARY_CHECKED: Int = 1
    }
}
