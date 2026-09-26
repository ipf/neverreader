package com.neverreader.sdk.util.dialog

import android.app.Dialog
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.TextView
import com.neverreader.app.R
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderActivity.OnLifeCycleChangedListener
import com.neverreader.sdk.util.AbsNeverReaderActivity.SimpleOnLifeCycleChangedListener
import com.neverreader.ui.view.progress.RainbowProgressCircleView

/**
 * Helper for blocking access to an Activity while a sync is still loading.
 */
class FetchingDialog private constructor(
    private val activity: AbsNeverReaderActivity,
    private val dismissListener: OnDismissedListener?
) {
    private val dialog = Dialog(activity, R.style.FetchingDialog)
    private val progressView: RainbowProgressCircleView
    private val activityListener: OnLifeCycleChangedListener =
        object : SimpleOnLifeCycleChangedListener() {
            override fun onActivityStop(activity: AbsNeverReaderActivity?) {
                finish()
            }
        }
    private var isDismissed = false

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.view_loading_dialog, null, false)
        val textView = view.findViewById<TextView>(R.id.message_loading)
        this.progressView = view.findViewById(R.id.progress_loading)
        textView.setText(R.string.dg_fetching)
        progressView.setProgressIndeterminate(true)
        dialog.setContentView(view)
        dialog.setCancelable(false)
        dialog.window!!.setDimAmount(0.66f)
        activity.addOnLifeCycleChangedListener(activityListener)

        // Block touches, otherwise there is a tiny gap between the display of the dialog where the activity is touchable.
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )

        dialog.show()
    }

    private fun finish() {
        if (isDismissed) return
        isDismissed = true
        if (dialog.isShowing) {
            dialog.dismiss()
        }
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        activity.removeOnLifeCycleChangeListener(activityListener)
        dismissListener?.onDismissed()
    }

    interface OnDismissedListener {
        fun onDismissed()
    }

    companion object {
        /**
         * Invoke during Activity.onStart to setup. It will automatically dismiss itself onStop.
         */
        fun blockInteractionUntilFetched(
            activity: AbsNeverReaderActivity?,
            listener: OnDismissedListener?
        ): Boolean {
            if (activity == null || activity.isFinishing) {
                return false
            }
            FetchingDialog(activity, listener)
            return true
        }
    }
}
