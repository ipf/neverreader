package com.neverreader.ui.view.progress.skeleton

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.ColorRes
import com.neverreader.ui.R
import com.neverreader.ui.util.ColorStateListDrawable
import com.neverreader.ui.view.themed.ThemedView
import com.neverreader.util.java.RandomSingleton

class SkeletonView : ThemedView {
    private val binder: Binder = Binder()

    private var randomWidthPercentFloor = 1f
    private var randomWidthPercentCeil = 1f

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context, attrs)
    }

    constructor(context: Context) : super(context!!) {
        init(context, null)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.SkeletonView)

            randomWidthPercentFloor =
                ta.getFloat(R.styleable.SkeletonView_randomWidthPercentFloor, 1f)
            randomWidthPercentCeil =
                ta.getFloat(R.styleable.SkeletonView_randomWidthPercentCeil, 1f)

            require(!(randomWidthPercentFloor > randomWidthPercentCeil)) { "randomWidthPercentFloor must be less than randomWidthPercentCeil" }

            val colors = ta.getResourceId(
                R.styleable.SkeletonView_compatBackgroundColor,
                R.color.nr_themed_grey_6
            )
            val radius =
                ta.getDimensionPixelSize(R.styleable.SkeletonView_cornerRadius, 0).toFloat()

            bind().background(colors, radius)

            ta.recycle()
        } else {
            bind().clear()
        }
    }

    private fun getRandomWidth(originalWidth: Int): Int {
        if (originalWidth == 0) {
            return 0
        }
        val minWidth = (originalWidth * randomWidthPercentFloor).toInt()
        val maxWidth = (originalWidth * randomWidthPercentCeil).toInt()

        return RandomSingleton.get().nextInt(maxWidth - minWidth) + minWidth
    }

    fun bind(): Binder {
        return binder
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        if (randomWidthPercentFloor < 1f) {
            var measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
            measuredWidth = getRandomWidth(measuredWidth)
            val measureMode = MeasureSpec.getMode(widthMeasureSpec)
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth, measureMode)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    inner class Binder {
        fun clear(): Binder {
            background(R.color.nr_themed_grey_6, 0f)
            originalWidth()
            return this
        }

        fun background(@ColorRes color: Int, cornerRadius: Float): Binder {
            setBackground(ColorStateListDrawable(getContext(), color, cornerRadius))
            return this
        }

        fun originalWidth(): Binder {
            randomWidthPercentFloor = 1f
            randomWidthPercentCeil = 1f
            requestLayout()
            return this
        }

        fun randomWidth(floorPercent: Float, ceilPercent: Float): Binder {
            randomWidthPercentFloor = floorPercent
            randomWidthPercentCeil = ceilPercent
            requestLayout()
            return this
        }
    }
}
