package com.neverreader.ui.view.progress

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedConstraintLayout
import com.neverreader.util.android.setTextOrHide

/**
 * A fullscreen tinted view which blocks user interactions with an optional [RainbowProgressCircleView] and/or message.
 */
class FullscreenProgressView : ThemedConstraintLayout {
    private val binder: Binder = Binder()

    private var progressCircle: View? = null
    private var messageView: TextView? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(context: Context) : super(context!!) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_fullscreen_progress, this, true)
        setClickable(true)
        setFocusable(true)
        setBackgroundColor(Color.parseColor("#88000000"))
        progressCircle = findViewById<View>(R.id.progress_circle)
        messageView = findViewById<TextView>(R.id.message)
        bind().clear()
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            progressCircle(true)
            message(null)
            visible(false)
            return this
        }

        fun progressCircle(show: Boolean): Binder {
            progressCircle!!.setVisibility(if (show) VISIBLE else GONE)
            return this
        }

        fun message(message: CharSequence?): Binder {
            messageView!!.setTextOrHide(message)
            return this
        }

        fun visible(show: Boolean): Binder {
            setVisibility(if (show) VISIBLE else GONE)
            return this
        }
    }
}
