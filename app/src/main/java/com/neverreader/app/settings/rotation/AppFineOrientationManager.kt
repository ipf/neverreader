package com.neverreader.app.settings.rotation

import android.app.Activity
import android.view.OrientationEventListener
import android.view.Surface
import com.neverreader.app.settings.rotation.interf.FineOrientationManager
import com.neverreader.app.settings.rotation.interf.FineOrientationManager.OnNewRotationListener
import kotlin.math.roundToInt

class AppFineOrientationManager(private val activity: Activity) : OrientationEventListener(
    activity
), FineOrientationManager {
    private var onNewRotationListener: OnNewRotationListener? = null

    override fun onOrientationChanged(orientation: Int) {
        onFineOrientationChange(orientation)
    }

    /**
     * This method is triggered only while LOCKED because we no longer receive onConfigurationChange events for device orientation changes.
     * Instead, we activate an OrientationEventListener to listen for ALL changes in device orientation.
     *
     *
     * By comparing the current fine orientation with the last orientation the lock was shown in (within FINE_ORIENTATION_THRESHOLD), we can
     * determine if an orientation change has occurred.
     *
     *
     * If an orientation change is detected we show the lock button again.
     */
    private fun onFineOrientationChange(rotation: Int) {
        if (rotation == ORIENTATION_UNKNOWN) { // returned if the device is flat and orientation cannot be determined
            return
        }

        // Determine if the lock has already been shown for this orientation, within the FINE_ORIENTATION_THRESHOLD.
        var stillInLastShown = true
        when (lastShownForOrientation) {
            Surface.ROTATION_0 -> stillInLastShown =
                rotation < 90 - FINE_ORIENTATION_THRESHOLD || rotation > 270 + FINE_ORIENTATION_THRESHOLD

            Surface.ROTATION_90 -> stillInLastShown =
                rotation < 180 - FINE_ORIENTATION_THRESHOLD && rotation > 0 + FINE_ORIENTATION_THRESHOLD

            Surface.ROTATION_180 -> stillInLastShown =
                rotation < 270 - FINE_ORIENTATION_THRESHOLD && rotation > 90 + FINE_ORIENTATION_THRESHOLD

            Surface.ROTATION_270 -> stillInLastShown =
                rotation < 360 - FINE_ORIENTATION_THRESHOLD && rotation > 180 + FINE_ORIENTATION_THRESHOLD
        }

        if (!stillInLastShown) {
            lastShownForOrientation = getSurfaceRegion(rotation)

            onNewRotationListener!!.onNewRotation()
        }
    }

    /**
     * Sets the last Display rotation the lock widget was shown for (ROTATION_0, ROTATION_90, ROTATION_180, or ROTATION_270).
     */
    override fun markCurrentOrientation() {
        lastShownForOrientation = activity.getWindowManager().getDefaultDisplay().getRotation()
    }

    override fun setOnNewRotationListener(listener: OnNewRotationListener?) {
        onNewRotationListener = listener
    }

    /**
     * Toggle whether the OrientationEventListener is active. This is only active while locked as we need to manually decide when to show the unlock button
     * rather than relying on configuration changes to tell us we've definitely rotated.
     */
    override fun setEnabled(enabled: Boolean) {
        if (enabled) {
            enable()
        } else {
            disable()
        }
    }

    companion object {
        private const val FINE_ORIENTATION_THRESHOLD = 10

        private var lastShownForOrientation = 0

        /**
         * Given a "fine" orientation value from the sensor, determine which Surface rotation value it falls within.
         */
        private fun getSurfaceRegion(rotation: Int): Int {
            var region = ((rotation / 90f).roundToInt()) * 90
            if (region >= 360) {
                region -= 360
            }
            when (region) {
                0 -> return Surface.ROTATION_0
                90 -> return Surface.ROTATION_90
                180 -> return Surface.ROTATION_180
                270 -> return Surface.ROTATION_270
            }
            return Surface.ROTATION_0
        }
    }
}
