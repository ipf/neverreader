package com.neverreader.sdk.util.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import com.neverreader.app.App.Companion.getContext as appContextFn
import com.neverreader.app.R
import com.neverreader.sdk.util.drawable.RainbowDrawable
import com.neverreader.ui.view.themed.ThemedView

class RainbowBar : ThemedView {
    var rainbow: RainbowDrawable? = null
        private set

    constructor(context: Context?) : super(context) {
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

    private fun init() {
        this.rainbow = RainbowDrawable(this)
    }

    protected override fun verifyDrawable(who: Drawable): Boolean {
        if (who === this.rainbow) {
            return true
        }
        return super.verifyDrawable(who)
    }

    override fun getSuggestedMinimumHeight(): Int {
        return MIN_RAINBOW_HEIGHT
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        rainbow!!.state = drawableState
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rainbow!!.setBounds(0, 0, w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        rainbow!!.draw(canvas)
    }

    companion object {
        val MIN_RAINBOW_HEIGHT: Int =
            appContextFn()!!.getResources().getDimensionPixelSize(R.dimen.rainbow_bar_height)
    }
}
