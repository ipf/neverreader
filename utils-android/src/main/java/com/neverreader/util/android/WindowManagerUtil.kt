package com.neverreader.util.android

import android.os.Build
import android.view.WindowManager

/**
 * Methods for getting screen width and height depending on the API level
 */
object WindowManagerUtil {
    fun getScreenWidth(windowManager: WindowManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.width()
        } else {
            windowManager.defaultDisplay.width
        }
    }

    fun getScreenHeight(windowManager: WindowManager): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            windowManager.defaultDisplay.height
        }
    }
}
