package com.neverreader.sdk.util.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.DialogFragment
import com.neverreader.app.App.Companion.getStringResource

/**
 * An abstract base class for creating dialog fragments.
 *
 * Adds extra functionality and helper methods for the lifecycle of the dialog.
 *
 * @author max
 */
abstract class RilDialogFragment : DialogFragment() {
    private var mOnCloseListener: OnCloseListener? = null
    private var mCalledDimissListener = false
    protected var mWasRestored: Boolean = false
    protected var mShouldPersist: Boolean = true

    fun setShouldPersist(value: Boolean) {
        mShouldPersist = value
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(STATE_SHOULD_PERSIST, mShouldPersist)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClose(false)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        onClose(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        onClose(false)
    }

    protected open fun onClose(isCancel: Boolean) {
        if (mOnCloseListener != null) {
            if (isCancel) {
                mOnCloseListener!!.onCancel(this)
            } else if (!mCalledDimissListener) {
                mCalledDimissListener = true
                mOnCloseListener!!.onDismiss(this)
            }
        }
    }

    /**
     * This will not be restored
     */
    fun setOnCloseListener(listener: OnCloseListener?) {
        mOnCloseListener = listener
    }

    interface OnCloseListener {
        fun onCancel(frag: RilDialogFragment?)
        fun onDismiss(frag: RilDialogFragment?)
    }

    /**
     * API HACK
     * since the support package does not yet have the dismissAllowingStateLoss() method available,
     * this is a workaround.
     *
     * Looking at the source code, the support package's onDismiss has the following contents:
     * if (!mRemoved) {
     * // Note: we need to use allowStateLoss, because the dialog
     * // dispatches this asynchronously so we can receive the call
     * // after the activity is paused.  Worst case, when the user comes
     * // back to the activity they see the dialog again.
     * dismissInternal(true);
     * }
     * So by calling the private method dismissInternal(true), this accomplishes the same thing. The interface is ignored,
     * so it is safe to send a null value provided that overrides of this method don't need it.
     */
    override fun dismissAllowingStateLoss() {
        if (getFragmentManager() == null || isRemoving()) return  // already removed.


        dismissAllowingStateLoss()
    }

    /**  // OPT there is a copy of this method in OptionalDialogFragment.  sharing is caring.
     * When fragments detach, they do not have access to resources and this can crash.  So instead of
     * using getString(), you can use this method which will get the string from the App context instead.
     * @param res
     * @return
     */
    fun getStringSafely(res: Int): String? {
        return getStringResource(res)
    }

    companion object {
        protected const val STATE_SHOULD_PERSIST: String = "stateShouldPersist"

        @JvmStatic
        protected fun dialogCancelableSetup(
            fragment: DialogFragment,
            dialog: Dialog,
            cancelable: Boolean
        ): Dialog {
            // Ensures proper setup, including workaround fixes, for whatever cancelable mode the dialog is given

            if (!cancelable) {
                fragment.setCancelable(false)
                dialog.setOnKeyListener(object : DialogInterface.OnKeyListener {
                    override fun onKey(
                        dialog: DialogInterface?,
                        keyCode: Int,
                        event: KeyEvent?
                    ): Boolean {
                        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                            // Workaround, don't let the search button close a non-cancelable dialog
                            return true
                        }
                        return false
                    }
                })
            } else {
                dialog.setCanceledOnTouchOutside(true)
            }

            return dialog
        }
    }
}
