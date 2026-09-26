package com.neverreader.util.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.neverreader.app.R
import com.neverreader.ui.view.themed.ThemedNestedScrollView
import kotlin.math.min

class MaxHeightScrollView : ThemedNestedScrollView {
    private var maxHeight = 0

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initAttrs(attrs)
    }

    constructor(context: Context) : super(context)

    private fun initAttrs(attrs: AttributeSet?) {
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.NeverReaderTheme)

        setMaxHeight(a.getDimensionPixelSize(R.styleable.NeverReaderTheme_maxHeight, 0))

        a.recycle()
    }

    fun setMaxHeight(px: Int) {
        maxHeight = px
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val mode = View.MeasureSpec.getMode(heightMeasureSpec)
        val measuredHeight = View.MeasureSpec.getSize(heightMeasureSpec)
        val adjustedHeight = min(measuredHeight, maxHeight)
        val adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(adjustedHeight, mode)
        super.onMeasure(widthMeasureSpec, adjustedHeightMeasureSpec)
    }
}
