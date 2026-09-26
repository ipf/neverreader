package com.neverreader.ui.view.empty

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedLottieAnimationView
import com.neverreader.ui.view.visualmargin.VisualMarginConstraintLayout
import com.neverreader.util.android.setTextOrHide

/**
 * A view for displaying empty or error messages where content loaded empty or had an error loading.
 */
class EmptyView : VisualMarginConstraintLayout {
    private val binder = Binder(this)
    private var title: TextView? = null
    private var message: TextView? = null
    private var button: TextView? = null
    private var errorButton: TextView? = null
    private var details: TextView? = null
    private var detailsDivider: View? = null
    private var animationContainer: ViewGroup? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_empty, this, true)
        title = findViewById<TextView>(R.id.title)
        message = findViewById<TextView>(R.id.message)
        button = findViewById<TextView>(R.id.button)
        errorButton = findViewById<TextView>(R.id.error_button)
        detailsDivider = findViewById<View>(R.id.detail_divider)
        details = findViewById<TextView>(R.id.details)
        animationContainer = findViewById<ViewGroup>(R.id.animation_container)
    }

    fun bind(): Binder {
        return binder
    }

    class Binder constructor(private val view: EmptyView) {
        fun clear(): Binder {
            title(null)
            message(null)
            button(null)
            details(null)
            buttonOnClick(null)
            buttonOnLongClick(null)
            animationView(null)
            return this
        }

        fun title(value: CharSequence?): Binder {
            view.title!!.setTextOrHide(value)
            return this
        }

        fun message(value: CharSequence?): Binder {
            view.message!!.setTextOrHide(value)
            return this
        }

        fun button(value: CharSequence?): Binder {
            view.button!!.setTextOrHide(value)
            view.errorButton!!.setTextOrHide(null)
            return this
        }

        fun errorButton(value: CharSequence?): Binder {
            view.errorButton!!.setTextOrHide(value)
            view.button!!.setTextOrHide(null)
            return this
        }

        fun buttonOnClick(listener: OnClickListener?): Binder {
            view.button!!.setOnClickListener(listener)
            view.errorButton!!.setOnClickListener(listener)
            return this
        }

        fun buttonOnLongClick(listener: OnLongClickListener?): Binder {
            view.button!!.setOnLongClickListener(listener)
            view.errorButton!!.setOnLongClickListener(listener)
            view.button!!.setLongClickable(listener != null)
            view.errorButton!!.setLongClickable(listener != null)
            return this
        }

        fun details(value: CharSequence?): Binder {
            view.details!!.setTextOrHide(value)
            view.detailsDivider!!.setVisibility(view.details!!.getVisibility())
            return this
        }

        fun animationView(animationView: ThemedLottieAnimationView?): Binder {
            view.animationContainer!!.removeAllViews()
            if (animationView != null) {
                view.animationContainer!!.addView(animationView)
                animationView.playAnimation() // plays the animation as soon as the View is shown
            }
            return this
        }
    }
}
