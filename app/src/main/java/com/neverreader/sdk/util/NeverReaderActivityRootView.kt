package com.neverreader.sdk.util

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.neverreader.app.R
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
        content = findViewById(R.id.content)
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
