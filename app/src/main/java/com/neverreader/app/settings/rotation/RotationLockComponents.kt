package com.neverreader.app.settings.rotation

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Handler
import com.neverreader.app.settings.rotation.interf.FineOrientationManager
import com.neverreader.app.settings.rotation.interf.FineOrientationManager.OnNewRotationListener
import com.neverreader.app.settings.rotation.interf.OSRotationLock
import com.neverreader.app.settings.rotation.interf.RotationLockView
import com.neverreader.app.settings.rotation.interf.RotationLockView.OnClick
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderActivity.OnConfigurationChangedListener
import com.neverreader.sdk.util.AbsNeverReaderActivity.SimpleOnLifeCycleChangedListener
import com.neverreader.util.prefs.BooleanPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Rotation Lock is a feature which lets the user easily lock or unlock NeverReader's display orientation in order to prevent accidental screen rotation while in the app.
 * The device can be in one of 4 orientations, depending on whether it is in landscape or portrait, and upside down or right side up.
 *
 *
 * While UNLOCKED, if a rotation is detected via the Activity's onConfigurationChanged callback, a rotation lock button
 * will briefly appear on the screen. Tapping this button will lock NeverReader into the current orientation.
 *
 *
 * While LOCKED, a device sensor OrientationEventListener is activated, and checks for orientation changes within the threshold of a rotation change.
 * If that is detected the lock button is again shown in case they want to unlock. Unlocking releases it to auto rotation again.
 *
 *
 * The lock status persists through app sessions via a SharedPreference.
 *
 *
 * A user may also disable this feature altogether in the settings screen. Disabling the feature in settings will release the lock if currently locked.
 *
 *
 * Modern versions of Android have rotation locks as an OS feature. While a user has an OS rotation lock active our rotation lock feature will be disabled and not show, as we cannot disable the OS level lock.
 * If they activate an OS lock while our lock is locked, we release ours and let the OS handle it.
 */
class RotationLockComponents(
    private val activity: Activity,
    userPref: BooleanPreference,
    osRotationLock: OSRotationLock,
    lockView: RotationLockView,
    fineOrientationManager: FineOrientationManager,
    private val rotationLock: RotationLock
) : SimpleOnLifeCycleChangedListener(), OnConfigurationChangedListener {
    // setup initial state
    private var handler: Handler = Handler()

    private val userPref: BooleanPreference
    private val lockView: RotationLockView
    private val osRotationLock: OSRotationLock
    private val fineOrientationManager: FineOrientationManager
    private var prefJob: Job?
    private var currentOrientation = 0

    init {
        setupCurrentOrientation()

        this.userPref = userPref
        this.osRotationLock = osRotationLock
        this.lockView = lockView
        this.fineOrientationManager = fineOrientationManager

        fineOrientationManager.setOnNewRotationListener(OnNewRotationListener {
            if (this.isLockingAllowed) {
                lockView.show(rotationLock.isLocked)
            }
        })

        // Scoped to this component and cancelled in onActivityDestroy, matching the
        // lifetime the Rx Disposable had.
        prefJob = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).launch {
            userPref.changes().collect { enabled ->
                // If it is locked and the setting has turned off, we want to unlock the rotation.
                if (enabled == false && rotationLock.isLocked) {
                    handler.post(Runnable { rotationLock.setLocked(false, activity) })
                }
            }
        }

        lockView.setOnToggleClick(OnClick { checked: Boolean ->
            // lock the current orientation
            rotationLock.setLocked(checked, this.activity)
            fineOrientationManager.setEnabled(checked)

            fineOrientationManager.markCurrentOrientation()
            lockView.show(rotationLock.isLocked)
        })

        // we don't show the lock on initialization, so mark the current orientation as already shown
        fineOrientationManager.markCurrentOrientation()
    }

    private val isLockingAllowed: Boolean
        /**
         * Whether or not the rotation lock widget will show on rotation. If it is currently locked this will always return true.
         */
        get() {
            // always show if currently locked, so we have a way to get out of a lock
            if (rotationLock.isLocked) {
                return true
            }
            // don't show the lock if user settings say no OR we're in an OS lock, since we can't get out of that anyway
            return if (!userPref.get() || osRotationLock.isLocked) {
                false
            } else {
                // otherwise, all good to show
                true
            }
        }

    private fun setupCurrentOrientation() {
        val requestedOrientation = activity.requestedOrientation

        // if our current activity orientation equals that in the RotationLock, do nothing.
        if (requestedOrientation == rotationLock.getOrientation()) {
            return
        }

        // if the user has locked OR the current Activity orientation is not using the sensor (meaning it changed through some outside config change)
        // then apply our locked orientation to the Activity.
        if (rotationLock.isLocked || requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
            rotationLock.applyCurrentOrientation(activity)
        }

        currentOrientation = activity.resources.configuration.orientation
    }

    public override fun onActivityRestart(activity: AbsNeverReaderActivity?) {
        setupCurrentOrientation()
        lockView.hide()
        fineOrientationManager.markCurrentOrientation()
    }

    public override fun onActivityResume(activity: AbsNeverReaderActivity?) {
        fineOrientationManager.setEnabled(rotationLock.isLocked)
        osRotationLock.startObserving()
    }

    override fun onActivityPause(activity: AbsNeverReaderActivity?) {
        fineOrientationManager.setEnabled(false)
        osRotationLock.stopObserving()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
    }

    override fun onActivityDestroy(activity: AbsNeverReaderActivity?) {
        handler.post(Runnable {
            activity?.removeOnConfigurationChangedListener(this)
            activity?.removeOnLifeCycleChangeListener(this)
            prefJob?.cancel()
            prefJob = null
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        val previousOrientation = currentOrientation
        currentOrientation = newConfig?.orientation ?: 0

        handler.post(Runnable {
            if (this.isLockingAllowed && previousOrientation != currentOrientation) {
                lockView.show(rotationLock.isLocked)
            }
        })
    }
}
