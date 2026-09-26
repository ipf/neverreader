package com.neverreader.ui.util

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

/**
 * A drawable that fills its bounds with a single color, from a [ColorStateList], for the current drawable state.
 *
 *
 * Similar to [ColorDrawable] but supports [ColorStateList].
 *
 */
class ColorStateListDrawable(private val mColors: ColorStateList) : Drawable() {
    private val mPaint: Paint

    private var mAlpha = 255

    private var mRoundedCornerRect: RectF? = null
    private var mCornerRadius = 0f

    constructor(
        context: Context,
        colorStateListRes: Int,
        cornerRadius: Float
    ) : this(ContextCompat.getColorStateList(context, colorStateListRes)!!) {
        this.mCornerRadius = cornerRadius
        this.mRoundedCornerRect = if (cornerRadius > 0) RectF() else null
    }

    constructor(context: Context, colorStateListRes: Int) : this(
        ContextCompat.getColorStateList(
            context,
            colorStateListRes
        )!!
    )

    init {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    }

    override fun onStateChange(state: IntArray): Boolean {
        val rt = super.onStateChange(state)
        val newColor = mColors.getColorForState(state, Color.TRANSPARENT)
        if (mPaint.getColor() != newColor) {
            mPaint.setColor(newColor)
            mPaint.setAlpha(mAlpha)
            return true
        } else {
            return rt
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        if (mRoundedCornerRect != null) {
            mRoundedCornerRect!!.set(bounds)
        }
    }

    override fun draw(canvas: Canvas) {
        if (mRoundedCornerRect != null) {
            canvas.drawRoundRect(mRoundedCornerRect!!, mCornerRadius, mCornerRadius, mPaint)
        } else {
            canvas.drawRect(getBounds(), mPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        if (alpha == mAlpha) {
            return
        }
        mPaint.setAlpha(alpha)
        mAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(cf: ColorFilter?) {
        if (mPaint.getColorFilter() === cf) {
            return
        }
        mPaint.setColorFilter(cf)
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT // Dependent on the actual color being drawn, just assume alpha.
    }

    override fun isStateful(): Boolean {
        return true
    }
}
