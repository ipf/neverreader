package com.neverreader.util.android

import android.content.Context
import android.os.Build
import android.view.WindowManager

/**
 * Bounds used to cap a dialog to something that fits on screen.
 *
 * `WindowManager.defaultDisplay` is deprecated, so the pre-API-30 branch reads
 * the metrics off the [Context] instead. That does mean the two branches
 * measure different things - the display below API 30, the window from 30 on -
 * which is also what the deprecated call did, so nothing gets worse.
 */
object WindowManagerUtil {
    fun getScreenWidth(context: Context): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds.width()
        } else {
            context.resources.displayMetrics.widthPixels
        }

    fun getScreenHeight(context: Context): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.getSystemService(WindowManager::class.java)
                .currentWindowMetrics.bounds.height()
        } else {
            context.resources.displayMetrics.heightPixels
        }
}
