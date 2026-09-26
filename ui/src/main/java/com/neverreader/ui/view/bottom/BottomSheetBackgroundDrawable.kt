package com.neverreader.ui.view.bottom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.shapes.RoundRectShape
import android.graphics.drawable.shapes.Shape
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPx
import com.neverreader.ui.util.NestedColorStateList.get

/**
 * The background of a bottom sheet. Required to be a Drawable since xml shapes don't support themed colors.
 * No intrinsic bounds, fills the set bounds.
 */
class BottomSheetBackgroundDrawable(context: Context) : Drawable() {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillColors: ColorStateList?
    private val fill: Shape
    private val shadowSize: Int

    init {
        val radius = dpToPx(context, 16f)
        fill = RoundRectShape(
            floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f),
            null,
            null
        ) // Rounded tops, square bottoms
        fillColors = get(context, R.color.nr_bg)

        shadowSize = context.getResources().getDimension(R.dimen.nr_drawer_shadow_radius).toInt()
        // simulating box-shadow: 0px -3px 6px rgba(0, 0, 0, 0.06);
        fillPaint.setShadowLayer(shadowSize.toFloat(), 0.0f, 0.0f, 0x10000000)
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        val r = super.onStateChange(state)
        val newFillColor = fillColors!!.getColorForState(state, Color.TRANSPARENT)
        if (newFillColor != fillPaint.getColor()) {
            invalidateSelf()
            fillPaint.setColor(newFillColor)
            return true
        } else {
            return r
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        fill.resize(bounds.width().toFloat(), bounds.height().toFloat())
    }

    override fun draw(canvas: Canvas) {
        canvas.translate(0f, shadowSize.toFloat())
        fill.draw(canvas, fillPaint)
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.setAlpha(alpha)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.setColorFilter(colorFilter)
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
