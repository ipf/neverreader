package com.neverreader.ui.view.badge

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPx
import org.apache.commons.lang3.ArrayUtils

/**
 * The background bounding box for use in various Badge views.
 * Maybe could be replaced with just an xml shape drawable.
 */
internal class BadgeDrawable(context: Context, color: ColorStateList) : Drawable() {
    private val mRectFill = RectF()
    private val mPaintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mDisabledColorFill: ColorStateList?
    private val mColorFill: ColorStateList
    private val mCornerRadius: Float

    init {
        mPaintFill.isAntiAlias = true
        mPaintFill.isDither = true
        mPaintFill.style = Paint.Style.FILL
        mCornerRadius = dpToPx(context, 4f)
        mColorFill = color
        mDisabledColorFill = ContextCompat.getColorStateList(context, R.color.nr_badge_disabled)
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        updateDrawComponents()
        return true
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        updateDrawComponents()
    }

    private fun updateDrawComponents() {
        val currentFill = mPaintFill.getColor()

        val state = getState()
        var fill = mColorFill.getColorForState(state, Color.TRANSPARENT)
        if (!ArrayUtils.contains(state, android.R.attr.state_enabled)) {
            fill = mDisabledColorFill!!.getColorForState(state, fill)
        }

        mPaintFill.setColor(fill)

        if (currentFill != fill) {
            /*
			 * Workaround for a quirk in the TextView. If the text color selector doesn't change
			 * color, then it doesn't invalidate on state change. So if we detect our custom paints
			 * have changed color, we force the invalidate to ensure it redraws.
			 */
            invalidateSelf()
        }
        mRectFill.set(getBounds())
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(mRectFill, mCornerRadius, mCornerRadius, mPaintFill)
    }

    override fun setAlpha(alpha: Int) {
        mPaintFill.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaintFill.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
