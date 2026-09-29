package com.neverreader.sdk.util.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.view.View

import com.neverreader.app.R
import com.neverreader.sdk.util.AbsNeverReaderActivity

/**
 * Helper for showing alert dialogs, error dialogs, and progress dialogs.
 *
 * Simplified for NeverReader: no in-app support / error reporting flows.
 */
object AlertMessaging {

    fun isContextUnavailable(context: Context?): Boolean {
        return context == null || (context is AbsNeverReaderActivity && context.isFinishing)
    }

    private fun opt(context: Context?, resId: Int): CharSequence? {
        return if (resId != 0) context!!.getText(resId) else null
    }

    fun show(context: Context?, title: Int, message: Int): AlertDialog? {
        return show(context, title, message, null)
    }

    fun show(context: Context?, title: Int, message: Int, onDismiss: DialogInterface.OnDismissListener?): AlertDialog? {
        return show(context, opt(context, title), opt(context, message), null, null, null, null, onDismiss)
    }

    fun show(
        context: Context?,
        title: CharSequence?,
        message: CharSequence?,
        neutralTitle: CharSequence?,
        onNeutralClick: DialogInterface.OnClickListener?,
        onDismiss: DialogInterface.OnDismissListener?
    ): AlertDialog? {
        return show(context, title, message, null, null, neutralTitle, onNeutralClick, onDismiss)
    }

    fun show(
        context: Context?,
        title: CharSequence?,
        message: CharSequence?,
        positiveTitle: CharSequence?,
        onPositiveClick: DialogInterface.OnClickListener?,
        neutralTitle: CharSequence?,
        onNeutralClick: DialogInterface.OnClickListener?,
        onDismiss: DialogInterface.OnDismissListener? = null,
    ): AlertDialog? {
        if (isContextUnavailable(context)) return null

        val alert = AlertDialog.Builder(context)
        if (title != null) alert.setTitle(title)
        if (message != null) alert.setMessage(message)
        if (positiveTitle != null) alert.setPositiveButton(positiveTitle, onPositiveClick)
        if (neutralTitle != null) alert.setNeutralButton(neutralTitle, onNeutralClick)
        if (onDismiss != null) alert.setOnDismissListener(onDismiss)
        return alert.show()
    }

    fun showConnectionDependantError(
        activity: AbsNeverReaderActivity?,
        title: Int,
        message: Int
    ): AlertDialog? {
        if (isContextUnavailable(activity)) return null
        return show(activity, title, message)
    }

    fun showError(
        activity: AbsNeverReaderActivity?,
        title: Int,
        message: Int
    ): AlertDialog? {
        if (isContextUnavailable(activity)) return null
        return show(activity, title, message)
    }
}
