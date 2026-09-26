package com.neverreader.app.settings.rotation

import android.app.Activity
import android.database.ContentObserver
import android.os.Handler
import android.provider.Settings
import com.neverreader.app.settings.rotation.interf.OSRotationLock

class AndroidOSRotationLock(
    private val activity: Activity,
    private val rotationLock: RotationLock
) : OSRotationLock {
    private var oSRotationLockObserver: ContentObserver? = null
    private var isOsRotationLocked = false

    init {
        checkForOSRotationLock()
    }

    /**
     * Sets whether the OS level orientation lock is enabled. This is checked on initialization and whenever a content change is detected via mOSRotationLockObserver.
     */
    private fun checkForOSRotationLock() {
        isOsRotationLocked = Settings.System.getInt(
            activity.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0
        ) == 0
    }

    override fun startObserving() {
        oSRotationLockObserver = object : ContentObserver(Handler()) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                checkForOSRotationLock()
                if (isOsRotationLocked && rotationLock.isLocked) {
                    // Unlock our lock so the OS level lock takes over
                    rotationLock.setLocked(false, activity)
                }
            }
        }
        activity.getContentResolver().registerContentObserver(
            Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
            false,
            oSRotationLockObserver!!
        )
    }

    override fun stopObserving() {
        activity.getContentResolver().unregisterContentObserver(oSRotationLockObserver!!)
    }

    override val isLocked: Boolean
        get() {
        return isOsRotationLocked
    }
}
