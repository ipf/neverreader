package com.neverreader.app.settings.rotation

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.Surface
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.util.android.ApiLevel
import com.neverreader.util.prefs.IntPreference
import com.neverreader.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the locking and unlocking of rotation for [RotationLockComponents].
 */
@Singleton
class RotationLock @Inject constructor(prefs: Preferences) {
    private val orientation: IntPreference = prefs.forApp("orientation", ActivityInfo.SCREEN_ORIENTATION_SENSOR)


    val isLocked: Boolean
        get() {
            val requestedOrientation: Int = orientation.get()
            return requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED && requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

    fun setLocked(lock: Boolean, activity: Activity) {
        val requestedOrientation: Int

        if (lock) {
            // if the user locks then we'll need to know their current screen orientation
            requestedOrientation = getFullOrientation(
                activity.windowManager.getDefaultDisplay().rotation,
                activity.resources.configuration.orientation
            )
        } else {
            // if we've unlocked, set the requested orientation to be the device sensor

            // NOTE: It has been SCREEN_ORIENTATION_SENSOR since the dawn of time, but in order to get the Android P OS lock feature to appear on screen,
            // it needs to be SCREEN_ORIENTATION_UNSPECIFIED. It is possible that SCREEN_ORIENTATION_UNSPECIFIED works for all OSs but doesn't seem worth
            // finding out when we can just change the behaviour going forward. TODO research this and test

            requestedOrientation =
                if (ApiLevel.isNougatOrGreater()) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }

        orientation.set(requestedOrientation)

        applyCurrentOrientation(activity)
    }

    /**
     * @param activity applies the current screen orientation to the provided Activity.
     */
    fun applyCurrentOrientation(activity: Activity) {
        activity.requestedOrientation = orientation.get()
    }

    /**
     * @return the more recently locked screen orientation.
     */
    fun getOrientation(): Int {
        return orientation.get()
    }

    companion object {
        /**
         * A device can be in one of 4 orientations, depending on whether it is in landscape / portrait, and upside down / right side up.
         * This returns the full orientation based on the rotation of the device and the current display configuration.
         *
         * @param rotation    The rotation. See [android.view.Display.getRotation]
         * @param orientation Either [Configuration.ORIENTATION_LANDSCAPE], [Configuration.ORIENTATION_PORTRAIT]
         * @return [ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE],  [ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE],  [ActivityInfo.SCREEN_ORIENTATION_PORTRAIT], or  [ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT]
         */
        private fun getFullOrientation(rotation: Int, orientation: Int): Int {
            val reversed: Boolean

            when (orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> {
                    reversed = rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_270
                    return if (!reversed) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                }

                Configuration.ORIENTATION_PORTRAIT -> {
                    reversed = rotation == Surface.ROTATION_180 || rotation == Surface.ROTATION_90
                    return if (!reversed) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                }

                else -> return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT // Shouldn't happen but used as default
            }
        }
    }
}
