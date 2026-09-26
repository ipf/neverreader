package com.neverreader.util.android

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Allows for easy and readable Android OS Api Level checks. Also, since the Build.VERSION_CODES aren't available
 * on all Api Levels, this knows what int values to check against.
 *
 * More: http://developer.android.com/guide/appendix/api-levels.html
 */
object ApiLevel {
    private const val Q = 29
    private const val NOUGAT = 24
    private const val P = 28

    @ChecksSdkIntAtLeast(api = NOUGAT)
    fun isNougatOrGreater(): Boolean {
        return Build.VERSION.SDK_INT >= NOUGAT
    }

    @ChecksSdkIntAtLeast(api = P)
    fun isPreP(): Boolean {
        return Build.VERSION.SDK_INT < P
    }

    @ChecksSdkIntAtLeast(api = P)
    fun isLightNavigationBarAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= P
    }


    @ChecksSdkIntAtLeast(api = Q)
    fun hasSystemDarkTheme(): Boolean {
        return Build.VERSION.SDK_INT >= Q
    }

}
