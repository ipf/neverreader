package com.neverreader.util.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.View.MeasureSpec
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.neverreader.app.R

/**
 * Encapsulate the necessary logic to add maxWidth functionality to a view.
 *
 * @author marcin
 */
class MaxWidthHelper {
    var maxWidth: Int = 0

    constructor()

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int = 0,
        @StyleRes defStyleRes: Int = 0
    ) {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.MaxWidthView,
            defStyleAttr,
            defStyleRes
        )
        maxWidth = a.getDimensionPixelSize(R.styleable.MaxWidthView_maxWidth, 0)
        a.recycle()
    }

    fun onMeasure(widthMeasureSpec: Int): Int {
        // Adjust width if necessary
        var widthMeasureSpec = widthMeasureSpec
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        if (maxWidth > 0 && maxWidth < measuredWidth) {
            val measureMode = MeasureSpec.getMode(widthMeasureSpec)
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(maxWidth, measureMode)
        }
        return widthMeasureSpec
    }

    internal interface MaxWidthView {
        fun getMaxWidth(): Int
        fun setMaxWidth(maxWidth: Int)
    }
}
