package com.neverreader.ui.view.dialog

import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.view.themed.ThemedRelativeLayout
import com.neverreader.util.android.setTextOrHide
import kotlin.math.min

/**
 * A simple prompt/messaging view. Typically used in a dialog popup.
 * Use [Binder.showAsAlertDialog] for convenience.
 */
class DialogView : ThemedRelativeLayout {
    private val binder: Binder = Binder()
    private val maxHeight = dpToPxInt(getContext(), 309f)
    private var title: TextView? = null
    private var message: TextView? = null
    private var buttonPrimary: TextView? = null
    private var buttonSecondary: TextView? = null
    private var onClickPrimary: OnClickListener? = null
    private var onClickSecondary: OnClickListener? = null
    private var dialog: AlertDialog? = null

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.view_dialog_popup, this, true)
        title = findViewById<TextView>(R.id.title)
        message = findViewById<TextView>(R.id.message)
        buttonPrimary = findViewById<TextView>(R.id.button_primary)
        buttonSecondary = findViewById<TextView>(R.id.button_secondary)
        buttonPrimary!!.setOnClickListener(OnClickListener { v: View? ->
            if (dialog != null) dialog!!.dismiss()
            if (onClickPrimary != null) onClickPrimary!!.onClick(v)
        })
        buttonSecondary!!.setOnClickListener(OnClickListener { v: View? ->
            if (dialog != null) dialog!!.dismiss()
            if (onClickSecondary != null) onClickSecondary!!.onClick(v)
        })
        bind().clear()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = View.MeasureSpec.getMode(heightMeasureSpec)
        val measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        val adjustedHeight = min(measuredHeight, maxHeight)
        val adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedHeight, mode)
        super.onMeasure(widthMeasureSpec, adjustedHeightMeasureSpec)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            title(null)
            message(null)
            buttonPrimary(null, null)
            buttonSecondary(null, null)
            return this
        }

        fun title(value: Int): Binder {
            return title(getResources().getText(value))
        }

        fun title(value: CharSequence?): Binder {
            title!!.setTextOrHide(value)
            return this
        }

        fun message(value: Int): Binder {
            return message(getResources().getText(value))
        }

        fun message(value: CharSequence?): Binder {
            message!!.setTextOrHide(value)
            return this
        }

        fun buttonPrimary(value: Int, listener: OnClickListener?): Binder {
            return buttonPrimary(getResources().getText(value), listener)
        }

        fun buttonPrimary(value: CharSequence?, listener: OnClickListener?): Binder {
            buttonPrimary!!.setTextOrHide(value)
            onClickPrimary = listener
            return this
        }

        fun buttonSecondary(value: Int, listener: OnClickListener?): Binder {
            return buttonSecondary(getResources().getText(value), listener)
        }

        fun buttonSecondary(value: CharSequence?, listener: OnClickListener?): Binder {
            buttonSecondary!!.setTextOrHide(value)
            onClickSecondary = listener
            return this
        }

        fun showAsAlertDialog(
            onDismissListener: DialogInterface.OnDismissListener?,
            cancelable: Boolean
        ): Binder {
            val self: View = this@DialogView
            if (getParent() is ViewGroup) {
                (getParent() as ViewGroup).removeView(self)
            }
            dialog =
                AlertDialog.Builder(getContext()) // TODO do we need to use an app compat theme to ensure this looks right?
                    .setView(self)
                    .setCancelable(cancelable)
                    .show()
            dialog!!.setOnDismissListener(DialogInterface.OnDismissListener { d: DialogInterface? ->
                dialog = null
                if (onDismissListener != null) {
                    onDismissListener.onDismiss(d)
                }
            })
            return this
        }
    }
}
