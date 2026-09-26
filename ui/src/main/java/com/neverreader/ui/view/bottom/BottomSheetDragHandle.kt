package com.neverreader.ui.view.bottom

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.view.themed.ThemedView
import kotlin.math.min

/**
 * A standard drag handle to include at the top of bottom sheets in NeverReader.
 * Use width and height set to wrap_content for standard sizing. Custom width and/or height (whether bigger or smaller)
 * will be respected for cases when it's needed.
 */
class BottomSheetDragHandle @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ThemedView(context, attrs) {
    private val paint = Paint()
    private val bounds = RectF()

    private val handleColor: ColorStateList?

    init {
        handleColor = ContextCompat.getColorStateList(context, R.color.nr_themed_grey_5)
    }

    override fun getSuggestedMinimumHeight(): Int {
        return dpToPxInt(getContext(), 6f) + getPaddingTop() + getPaddingBottom()
    }

    override fun getSuggestedMinimumWidth(): Int {
        return dpToPxInt(getContext(), 70f) + getPaddingLeft() + getPaddingRight()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            getSuggestedSize(getSuggestedMinimumWidth(), widthMeasureSpec),
            getSuggestedSize(getSuggestedMinimumHeight(), heightMeasureSpec)
        )

        bounds.set(
            getPaddingLeft().toFloat(),
            getPaddingTop().toFloat(),
            (getMeasuredWidth() - getPaddingRight()).toFloat(),
            (getMeasuredHeight() - getPaddingBottom()).toFloat()
        )
    }

    override fun onDraw(canvas: Canvas) {
        updatePaint(getDrawableState())
        val radius = min(bounds.bottom - bounds.top, bounds.right - bounds.left) / 2
        canvas.drawRoundRect(bounds, radius, radius, paint)
    }

    private fun updatePaint(state: IntArray?) {
        paint.setColor(handleColor!!.getColorForState(state, Color.TRANSPARENT))
    }

    companion object {
        private fun getSuggestedSize(size: Int, measureSpec: Int): Int {
            val specMode = MeasureSpec.getMode(measureSpec)
            val specSize = MeasureSpec.getSize(measureSpec)

            when (specMode) {
                MeasureSpec.EXACTLY -> return specSize

                MeasureSpec.AT_MOST -> return min(size, specSize)

                MeasureSpec.UNSPECIFIED -> return size
                else -> return size
            }
        }
    }
}
