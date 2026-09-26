package com.neverreader.ui.view.info

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.ImageView
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPx
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.NestedColorStateList
import com.neverreader.ui.view.themed.ThemedLinearLayout
import kotlin.math.ceil

/**
 * A set of horizontal dots for use as page indicators.
 */
class PageIndicatorView : ThemedLinearLayout {
    private val binder: Binder = Binder()

    private var margin = 0
    private var currentIndex = 0

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
        margin = dpToPxInt(getContext(), 6f)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            pageCount(0)
            currentIndex(0)
            return this
        }

        fun pageCount(value: Int): Binder {
            removeAllViews()

            for (i in 0..<value) {
                val indicator = ImageView(getContext())
                val indicatorParams = LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                )
                indicatorParams.setMargins(margin, 0, margin, 0)
                indicator.setLayoutParams(indicatorParams)
                indicator.setImageDrawable(PageIndicatorDrawable(getContext()))
                addView(indicator)
            }

            currentIndex(0)

            return this
        }

        fun currentIndex(value: Int): Binder {
            safeSetChildSelected(currentIndex, false)
            safeSetChildSelected(value, true)
            currentIndex = value
            return this
        }

        private fun safeSetChildSelected(index: Int, selected: Boolean) {
            if (getChildCount() > 0 && getChildCount() > index) {
                getChildAt(index).setSelected(selected)
            }
        }
    }

    private inner class PageIndicatorDrawable(context: Context) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val color: ColorStateList?
        private val radius: Float

        init {
            paint.setStyle(Paint.Style.FILL_AND_STROKE)
            radius = dpToPx(context, 3.5f)
            color = NestedColorStateList.get(context, R.color.nr_page_indicator)
        }

        override fun getIntrinsicWidth(): Int {
            return ceil((radius * 2).toDouble()).toInt()
        }

        override fun getIntrinsicHeight(): Int {
            return getIntrinsicWidth()
        }

        override fun isStateful(): Boolean {
            return true
        }

        override fun onStateChange(state: IntArray): Boolean {
            super.onStateChange(state)
            return true
        }

        override fun draw(canvas: Canvas) {
            paint.setColor(color!!.getColorForState(getState(), Color.TRANSPARENT))
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
}
