package com.neverreader.app.settings.view.preferences

import android.app.AlertDialog
import android.content.DialogInterface
import android.util.SparseArray
import android.view.View
import com.neverreader.app.R
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.PreferenceViews.EnabledCondition
import com.neverreader.util.prefs.IntPreference

/**
 * Manages a multiple choice [IntPreference]. Displays an [ActionPreference]
 * with the currently selected option as the summary/description.
 *
 *
 * When tapped, it opens a dialog picker.
 */
open class MultipleChoicePreference constructor(
    private val mSettings: AbsPrefsFragment,
    pref: PrefHandler?,
    label: String,
    summary: SparseArray<CharSequence?>,
    listener: OnSelectedItemChangedListener?,
    condition: EnabledCondition?,
    identifier: String?
) : ActionPreference(mSettings, label, summary, null, null, condition, identifier) {
    protected val mPref: PrefHandler
    private val mListener: OnSelectedItemChangedListener?
    private var mSelected: Int
    private val mChoices: Array<CharSequence?>

    interface PrefHandler {
        fun getSelected(): Int
        fun setSelected(index: Int)
    }

    /** Use [PreferenceViews] instead.  */
    init {
        if (summary == null || summary.size() == 0) {
            throw NullPointerException("summary may not be empty")
        }

        if (pref == null) {
            throw NullPointerException("pref may not be null")
        }

        val size = summary.size()
        mChoices = arrayOfNulls<CharSequence>(size)
        for (i in 0..<size) {
            mChoices[i] = summary.valueAt(i)
        }

        mPref = pref

        mListener = listener

        mSelected = pref.getSelected()
    }

    override val type: PrefViewType
        get() {
        return PrefViewType.ACTION
    }

    override fun onClick(v: View?) {
        AlertDialog.Builder(mSettings.requireActivity())
            .setTitle(label)
            .setSingleChoiceItems(mChoices, mSelected
            ) { dialog, which ->
                if (onItemSelected(v, which, dialog)) {
                    dialog.dismiss()
                } else {
                    // TODO need a way to reset the selection
                }
            }
            .setNegativeButton(R.string.ac_cancel
            ) { dialog, which -> dialog.dismiss() }
            .show()
    }

    fun onItemSelected(view: View?, newValue: Int, dialog: DialogInterface?): Boolean {
        val allowed = mListener == null || mListener.onItemSelected(view, newValue, dialog)

        if (allowed && newValue != mSelected) {
            mSelected = newValue
            mPref.setSelected(newValue)
            mSettings.onPreferenceChange(true)
            mListener?.onItemSelectionChanged(newValue)
        }

        if (uiEntityIdentifier != null) {
        }

        return allowed
    }

    interface OnSelectedItemChangedListener {
        /**
         * Called when the preference is requested to change. **It has not changed yet. If you query the preference it will have the old value.** You must return true to allow it to change.
         *
         *
         * If you want to know when the value has actually changed, see [.onItemSelectionChanged].
         *
         * @param view the view that triggered the selection
         * @param newValue
         * @return true if allowed to change, false if not
         */
        fun onItemSelected(view: View?, newValue: Int, dialog: DialogInterface?): Boolean

        /**
         * The preference's value has changed.
         * @param newValue
         * @see .onItemSelected
         */
        fun onItemSelectionChanged(newValue: Int)
    }

    override fun update(): Boolean {
        val newVal = mPref.getSelected()
        if (newVal != mSelected) {
            mSelected = newVal
            return true
        } else {
            return false
        }
    }

    override fun getSummary(): CharSequence? {
        return summary?.get(mSelected)
    }

    override val isClickable: Boolean
        get() {
        return true
    }
}
