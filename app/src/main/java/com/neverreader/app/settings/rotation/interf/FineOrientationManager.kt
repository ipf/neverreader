package com.neverreader.app.settings.rotation.interf

/**
 * A manager class which handles "fine" orientation changes when listening for the orientation via the device sensor.
 */
interface FineOrientationManager {
    fun interface OnNewRotationListener {
        fun onNewRotation()
    }

    /**
     * Marks the current orientation as the last known orientation.
     */
    fun markCurrentOrientation()

    /**
     * @param listener a listener for changes to the orientation.
     */
    fun setOnNewRotationListener(listener: OnNewRotationListener?)

    /**
     * @param enabled whether to listen for "fine" orientation changes.
     */
    fun setEnabled(enabled: Boolean)
}
