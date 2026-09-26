package com.neverreader.sdk.util.dialog

import android.app.Dialog
import android.app.ProgressDialog
import android.os.Bundle

open class ProgressDialogFragment(showingInstance: Boolean = false) : ExtendedDialogFragment() {
    override var isShowingInstance: Boolean = showingInstance
    private var mCancellable = false

    override fun onCreateArgs(args: Bundle?): Bundle? {
        args?.putBoolean(ARG_CANCELABLE, mCancellable)
        return super.onCreateArgs(args)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Basic setup
        val message = arguments!!.getString(KEY_MESSAGE)
        val cancellable = arguments!!.getBoolean(ARG_CANCELABLE)

        val dialog = ProgressDialog(activity)
        dialog.setMessage(message)
        dialog.setIndeterminate(true)


        // Some final setup
        dialogCancelableSetup(this, dialog, cancellable)
        return dialog
    }


    companion object {

        const val ARG_CANCELABLE: String = "cancelable"

        private var mIsShowingProgressDialogFragment = false

        fun getNew(message: Int, cancelable: Boolean): ProgressDialogFragment {
            return getNew(message, null, cancelable)
        }

        fun getNew(message: Int, tag: String?, cancelable: Boolean): ProgressDialogFragment {
            val frag = ProgressDialogFragment()
            frag.createArgs(null, message)
            return frag
        }

        fun getNew(message: String?, tag: String?, cancelable: Boolean): ProgressDialogFragment {
            val frag = ProgressDialogFragment()
            frag.createArgs(null, message)
            return frag
        }
    }
}
