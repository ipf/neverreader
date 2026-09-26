package com.neverreader.ui.view.button

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
import com.neverreader.ui.util.DimenUtil.dpToPx
import com.neverreader.ui.util.NestedColorStateList

class ButtonBoxDrawable constructor(
    context: Context,
    fillColors: Int,
    strokeColors: Int,
    cornerRadius: Float,
    outlineStroke: Float,
    cornerStyle: CornerStyle
) : Drawable() {
    private val mRectFill = RectF()
    private val mRectStroke = RectF()
    private val mPaintFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mPaintStroke = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mOutlineStroke: Float

    private val mColorFill: ColorStateList?
    private val mColorStroke: ColorStateList?
    private var mHasOutline = false
    private val mCornerRadius: Float
    private val mCornerStyle: CornerStyle
    private var mAlpha = 255

    enum class CornerStyle {
        /** Round the top corners  */
        TOP,

        /** Round the bottom corners  */
        BOTTOM,

        /** Round all corners  */
        ALL
    }

    constructor(
        context: Context,
        fillColors: Int,
        strokeColors: Int,
        cornerRadius: Float,
    ) : this(
        context,
        fillColors,
        strokeColors,
        cornerRadius,
        dpToPx(context, 1f),
        CornerStyle.ALL
    )

    constructor(context: Context, fillColors: Int, cornerStyle: CornerStyle) : this(
        context,
        fillColors,
        0,
        dpToPx(context, 4f),
        0f,
        cornerStyle
    )

    init {
        mPaintFill.isAntiAlias = true
        mPaintFill.isDither = true
        mPaintFill.style = Paint.Style.FILL

        mPaintStroke.style = Paint.Style.FILL

        mCornerRadius = cornerRadius
        mCornerStyle = cornerStyle
        mOutlineStroke = outlineStroke

        mColorFill = if (fillColors != 0) NestedColorStateList.get(context, fillColors) else null
        mColorStroke =
            if (strokeColors != 0) NestedColorStateList.get(context, strokeColors) else null
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
        val currentStroke = mPaintStroke.getColor()

        val state = getState()
        val fill = mColorFill?.getColorForState(
            state,
            Color.TRANSPARENT
        ) ?: Color.TRANSPARENT
        val stroke = mColorStroke?.getColorForState(
            state,
            Color.TRANSPARENT
        )
            ?: Color.TRANSPARENT

        mPaintFill.color = fill
        mPaintStroke.color = stroke
        mPaintFill.alpha = mAlpha
        mPaintStroke.alpha = mAlpha
        mHasOutline = stroke != Color.TRANSPARENT

        if (currentFill != fill || currentStroke != stroke) {
            /*
			 * Workaround for a quirk in the TextView. If the text color selector doesn't change
			 * color, then it doesn't invalidate on state change. So if we detect our custom paints
			 * have changed color, we force the invalidating to ensure it redraws.
			 */
            invalidateSelf()
        }

        val strokeWidth = if (mHasOutline) mOutlineStroke else 0f
        mRectStroke.set(bounds)
        mRectFill.set(mRectStroke)
        mRectFill.inset(strokeWidth, strokeWidth)
    }

    override fun draw(canvas: Canvas) {
        /*
		 * Note:
		 * Borders are drawn as rectangles instead of strokes because on some devices, strokes end up being different for different corners
		 * and producing inconsistent results. For example on a first gen N7, every single corner would be a different curve and radius.
		 * This method looks better.
		 */
        if (mHasOutline) {
            canvas.drawRoundRect(mRectStroke, mCornerRadius, mCornerRadius, mPaintStroke)
        }
        canvas.drawRoundRect(mRectFill, mCornerRadius, mCornerRadius, mPaintFill)

        when (mCornerStyle) {
            CornerStyle.TOP -> canvas.drawRect(
                mRectFill.left,
                mRectFill.bottom - mCornerRadius,
                mRectFill.right,
                mRectFill.bottom,
                mPaintFill
            )

            CornerStyle.BOTTOM -> canvas.drawRect(
                mRectFill.left,
                mRectFill.top,
                mRectFill.right,
                mRectFill.top + mCornerRadius,
                mPaintFill
            )

            CornerStyle.ALL -> {}
        }
    }

    override fun setAlpha(alpha: Int) {
        mAlpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mPaintFill.colorFilter = colorFilter
        mPaintStroke.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }
}
