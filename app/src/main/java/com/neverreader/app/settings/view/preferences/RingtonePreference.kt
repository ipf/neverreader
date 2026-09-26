package com.neverreader.app.settings.view.preferences

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Parcelable
import android.provider.Settings
import android.view.View
import com.neverreader.app.R
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.PreferenceViews.EnabledCondition
import com.neverreader.util.prefs.StringPreference
import org.apache.commons.lang3.StringUtils

/**
 * A special case [ActionPreference] for managing a ringtone as a [StringPreference].
 *
 *
 * Lets the user choose between available ringtones on the device and stores the ringtone's uri as
 * the preference value and the ringtone's title as the selected option.
 */
class RingtonePreference(
    private val mSettings: AbsPrefsFragment,
    pref: StringPreference,
    label: String,
    condition: EnabledCondition?,
    identifier: String
) : ActionPreference(mSettings, label, null, null, null, condition, identifier) {
    protected val mPref: StringPreference
    private var mSelected: String? = null
    private var mSelectedUri: Uri? = null

    init {
        if (pref == null) {
            throw NullPointerException("mPref may not be null")
        }

        mPref = pref

        setSelected(pref.get())
    }

    private fun setSelected(uriString: String?) {
        mSelectedUri = if (uriString != null) Uri.parse(uriString) else null
        mSelected = uriString
    }

    private fun setSelected(uri: Uri?) {
        mSelectedUri = uri
        mSelected = if (uri != null) uri.toString() else null
    }

    public override fun onClick(v: View?) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
        intent.putExtra(
            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            Settings.System.DEFAULT_NOTIFICATION_URI
        )
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, mSelectedUri)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        mSettings.startActivityForResult(intent, REQUEST_CODE)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        setSelected(data.getParcelableExtra<Parcelable?>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as Uri?)

        mPref.set(mSelected)
        mSettings.onPreferenceChange(true)
    }

    public override fun update(): Boolean {
        val newVal = mPref.get()
        if (!StringUtils.equals(newVal, mSelected)) {
            setSelected(newVal)
            return true
        } else {
            return false
        }
    }

    override fun getSummary(): String? {
        val context: Context? = mSettings.activity
        if (mSelectedUri == null) {
            return context!!.getString(R.string.setting_notify_sound_silent_sum)
        }
        val ringtone = RingtoneManager.getRingtone(context, mSelectedUri)
        return ringtone.getTitle(context)
    }

    public override val isClickable: Boolean
        get() {
        return true
    }

    companion object {
        private const val REQUEST_CODE = 55
    }
}
