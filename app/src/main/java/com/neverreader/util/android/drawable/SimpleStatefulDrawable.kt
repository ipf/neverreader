package com.neverreader.util.android.drawable

import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Contains some of the boilerplate for custom drawables we write over and over again.
 * Register paints with [.registerPaint].
 */
abstract class SimpleStatefulDrawable : Drawable() {
    private val paints = ArrayList<Paint>()

    protected fun registerPaint(paint: Paint) {
        paints.add(paint)
        paint.setAntiAlias(true)
    }

    override fun setAlpha(alpha: Int) {
        for (paint in paints) {
            paint.setAlpha(alpha)
        }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        for (paint in paints) {
            paint.setColorFilter(cf)
        }
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        super.onStateChange(state)
        for (paint in paints) {
            if (paint is StatefulPaint) {
                paint.setState(state)
            }
        }
        invalidateSelf()
        return true
    }
}

