package com.neverreader.util.android

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

object IntentUtils {
    /**
     * Convenience method for checking if an Intent that starts an Activity will actually
     * find an Activity to open or not.
     *
     * @param context
     * @param intent
     * @return
     */
    fun isActivityIntentAvailable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.isNotEmpty()
    }
}
