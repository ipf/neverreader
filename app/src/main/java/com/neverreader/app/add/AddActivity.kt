package com.neverreader.app.add

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.neverreader.app.App
import com.neverreader.app.R
import com.neverreader.backend.repo.AccountManager
import com.neverreader.backend.model.Bookmark
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.ui.view.notification.AppSnackbar
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * This Activity receives the [Intent.ACTION_SEND] intent when another app selects
 * "Add to NeverReader" in a share menu. It is a transparent Activity that searches
 * the incoming intent for a url and saves it to the active backend, showing a toast.
 */
@AndroidEntryPoint
class AddActivity : AbsNeverReaderActivity() {

    private val timeoutRunnable = Runnable { finish() }

    @Inject
    lateinit var saver: AddUrlSaver

    @Inject
    lateinit var accountManager: AccountManager

    override val isUserPresent: Boolean
        get() = false

    override fun checkClipboardForUrl() {
        // Do not check in this Activity
    }

    override fun supportsRotationLock(): Boolean {
        return false
    }

    override val isListenUiEnabled: Boolean
        get() = false

    override val accessType: ActivityAccessRestriction
        get() = ActivityAccessRestriction.ANY // toasts an error and finishes if the user is not logged in

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WARNING: This is an exported activity. Extras could come from outside apps and may not be trustworthy.
        val lp = WindowManager.LayoutParams()
        lp.copyFrom(window.attributes)
        lp.width = WindowManager.LayoutParams.MATCH_PARENT
        lp.height = WindowManager.LayoutParams.MATCH_PARENT
        lp.windowAnimations = android.R.style.Animation_Dialog
        window.attributes = lp

        // cancel on outside touches
        findViewById<View>(android.R.id.content).setOnTouchListener { _, _ ->
            finish()
            true
        }

        if (accountManager.activeCached == null) {
            showToast(AppSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_logged_out)
        } else {
            commitSave(IntentItemUtil.from(intent))
        }
    }

    override fun onStop() {
        super.onStop()
        finish() // if the user navigates away, just finish
    }

    override fun finish() {
        super.finish()
        cancelTimeout()
    }

    private fun showToast(
        type: AppSnackbar.Type,
        notificationText: Int,
    ) {
        AppSnackbar.make(
            this,
            type,
            resources.getText(notificationText),
            null,
        ).show()
        startTimeout()
    }

    private fun commitSave(intentItem: IntentItem) {
        if (intentItem.url != null) {
            // lifecycleScope, not a scope constructed per call: the save outlives this
            // transparent Activity, which finishes as soon as the toast is queued.
            lifecycleScope.launch {
                saver.add(intentItem, this, AddItemFromIntentUtil.Callback(this@AddActivity::onSaved))
            }
        } else {
            showToast(AppSnackbar.Type.ERROR_EXCLAIM_OUTSIDE, R.string.ts_add_invalid_url)
        }
    }

    private fun onSaved(item: Bookmark?, status: AddItemFromIntentUtil.ErrorStatus?) {
        // If there's an error we show a message and don't show any actions.
        if (status == AddItemFromIntentUtil.ErrorStatus.ADD_INVALID_URL) {
            showToast(AppSnackbar.Type.ERROR_EXCLAIM_OUTSIDE, R.string.ts_add_invalid_url)
            return
        }

        if (status == AddItemFromIntentUtil.ErrorStatus.ADD_ALREADY_IN) {
            showToast(AppSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_already_overlay)
        } else {
            showToast(AppSnackbar.Type.DEFAULT_OUTSIDE, R.string.ts_add_saved_to_ril)
        }
    }

    private fun startTimeout() {
        app()!!.threads().handler.postDelayed(timeoutRunnable, TIMEOUT_MS)
    }

    private fun cancelTimeout() {
        app()!!.threads().handler.removeCallbacks(timeoutRunnable)
    }

    companion object {
        private const val TIMEOUT_MS = 6500L
    }
}
