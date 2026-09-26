package com.neverreader.sdk.util

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewStub
import com.neverreader.app.R
import com.neverreader.app.settings.rotation.AndroidOSRotationLock
import com.neverreader.app.settings.rotation.AppFineOrientationManager
import com.neverreader.app.settings.rotation.RotationLockComponents
import com.neverreader.app.settings.rotation.interf.RotationLockView
import com.neverreader.util.android.view.ResizeDetectRelativeLayout

/**
 * The root view of [AbsNeverReaderActivity] that holds the content view, plus all of the
 * standard accessory views used on every screen of the app.
 *
 *
 * After inflating, call [.attach] to connect the activity to this view.
 *
 * TODO move the other stubs like rotate lock etc into this place for management.
 */
class NeverReaderActivityRootView : ResizeDetectRelativeLayout {
    private var content: AppActivityContentView? = null
    private var rotationLockComponents: RotationLockComponents? = null
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.ril_root, this, true)
    }

    fun attach(activity: AbsNeverReaderActivity) {
        content = findViewById<AppActivityContentView>(R.id.content)
        if (activity.isListenUiEnabled) {
        }

        if (activity.supportsRotationLock()) {
            val view =
                (this.findViewById<View?>(R.id.stub_lock) as ViewStub).inflate() as RotationLockView
            rotationLockComponents = RotationLockComponents(
                activity,
                activity.app()!!.prefs().ROTATION_LOCK,
                AndroidOSRotationLock(activity, activity.app()!!.rotationLock()),
                view,
                AppFineOrientationManager(activity),
                activity.app()!!.rotationLock()
            )

            activity.addOnLifeCycleChangedListener(rotationLockComponents)
            activity.addOnConfigurationChangedListener(rotationLockComponents)
        }
    }

    fun setListenInsets(insets: Rect?) {
    }

    /** Set the bottom space needed to show the listen component.  */
    fun setListenSpacing(height: Int) {
        val lp = content!!.layoutParams as LayoutParams
        lp.bottomMargin = height
        content!!.layoutParams = lp
    }

    val contentView: AppActivityContentView
        get() = content!!

    /**
     * The Compose surface every activity's snackbars render into. Owned here so
     * that one host serves the whole app, rather than each screen setting up its
     * own.
     */
    val snackbarHost: androidx.compose.ui.platform.ComposeView
        get() = findViewById(R.id.snackbarHost)

    /**
     * Called when the activity has detected the user's press of the back key.
     * @return true if handled the back press, false to let something else handle
     */
    fun onBackPressed(): Boolean {
        return false
    }

    fun expandListen() {
    }

}
