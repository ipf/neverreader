package com.neverreader.ui.view.menu

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPx
import com.neverreader.ui.util.NestedColorStateList
import kotlin.math.ceil

/**
 * Drawable for the selection indicator for [ThemeToggle].
 */
class ThemeToggleSelectionDrawable(context: Context) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val color: ColorStateList?
    private val radius: Float

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dpToPx(context, 2f)
        radius = dpToPx(context, 23.5f)
        color = NestedColorStateList.get(context, R.color.nr_themed_teal_2)
    }

    override fun getIntrinsicWidth(): Int {
        return ceil((radius * 2).toDouble()).toInt()
    }

    override fun getIntrinsicHeight(): Int {
        return intrinsicWidth
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        super.onStateChange(state)
        return true
    }

    override fun draw(canvas: Canvas) {
        paint.color = color!!.getColorForState(state, Color.TRANSPARENT)
        val bounds = getBounds()
        canvas.drawCircle(bounds.centerX().toFloat(), bounds.centerY().toFloat(), radius, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.setAlpha(alpha)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
