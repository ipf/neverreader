package com.neverreader.sdk.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.neverreader.app.App
import com.neverreader.sdk.util.AbsNeverReaderActivity.Companion.from
import com.neverreader.sdk.util.AbsNeverReaderActivity.SimpleOnLifeCycleChangedListener
import com.neverreader.util.prefs.BooleanPreference
import com.neverreader.util.prefs.Preferences
import java.util.Arrays

/**
 * Helper for checking/getting runtime permissions.
 * Create an instance during Activity.onCreate.
 * Result of this process will be returned to the [Callback.onPermissionResponse] callback.
 */
class PermissionRequester(
    context: Context?,
    requestCode: Int,
    callback: Callback,
    vararg permissions: String,
    private val prefs: Preferences
) {
    private val mActivity: AbsNeverReaderActivity? = from(context)
    private val mPermissions: Array<out String> = permissions
    private val mRequestCode: Int = requestCode
    private val mCallback: Callback = callback

    /**
     * Important: This must be created during a Activity.onCreate() to ensure it
     * receives callbacks if the activity is recreated later. TODO add a check to ensure this is only invoked during onCreate.
     */
    init {

        mActivity!!.addOnLifeCycleChangedListener(object : SimpleOnLifeCycleChangedListener() {
            public override fun onRequestPermissionsResult(
                requestCode: Int,
                permissions: Array<out String>,
                grantResults: IntArray
            ) {
                if (requestCode == mRequestCode) {
                    var allGranted = true
                    for (result in grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            allGranted = false
                            break
                        }
                    }
                    mCallback.onPermissionResponse(allGranted, permissions, grantResults)
                }
            }
        })
    }

    fun request() {
        val missing = ArrayList<String?>()
        for (permission in mPermissions) {
            if (ContextCompat.checkSelfPermission(
                    mActivity!!,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(permission)
            }
            val requestedPref = getPermissionRequestedPref(permission)
            requestedPref?.set(true)
        }
        if (missing.isEmpty()) {
            val results = IntArray(mPermissions.size)
            Arrays.fill(results, PackageManager.PERMISSION_GRANTED)
            mCallback.onPermissionResponse(true, mPermissions, results)
        } else {
            ActivityCompat.requestPermissions(mActivity!!, mPermissions, mRequestCode)
        }
    }

    interface Callback {
        fun onPermissionResponse(
            allGranted: Boolean,
            permissions: Array<out String>,
            results: IntArray?
        )
    }

    /**
     * Gets a BooleanPref that tracks whether the current installation has ever requested the given permission.  This is necessary in order to know whether we should
     * display a prompt to the user when they have selected "Don't ask again" for the permission.
     *
     * Since shouldShowRequestPermissionRationale will return false both are the user has never requested the permission and if they've selected the
     * "Don't ask again" option, it is not enough to just check the current permission state.
     *
     * @param permission the [Manifest.permission] String
     * @return the pref that pertains to the given permission.
     */
    private fun getPermissionRequestedPref(permission: String): BooleanPreference? {
        when (permission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> return prefs.forApp("readExternalStorageRequested", false)
        }
        return null
    }
}
