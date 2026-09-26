package com.neverreader.ui.util

import android.view.View
import kotlin.math.min

/**
 * Create an instance in your view class and then implement onMeasure like so:
 *
 * <pre>
 * @Override
 * protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
 * widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec);
 * heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec);
 * super.onMeasure(widthMeasureSpec, heightMeasureSpec);
 * }
</pre> *
 */
class IntrinsicSizeHelper
/**
 * @param width Intrinsic width in px, or -1 to not declare one
 * @param height Intrinsic height in px, or -1 to not declare one
 */(private val width: Int, private val height: Int) {
    constructor(diameter: Int) : this(diameter, diameter)

    fun applyWidth(measureSpec: Int): Int {
        return applyDimension(measureSpec, width)
    }

    fun applyHeight(measureSpec: Int): Int {
        return applyDimension(measureSpec, height)
    }

    private fun applyDimension(measureSpec: Int, defaultSize: Int): Int {
        if (defaultSize < 0) {
            return measureSpec
        }
        val specMode = View.MeasureSpec.getMode(measureSpec)
        var specSize = View.MeasureSpec.getSize(measureSpec)
        if (specMode == View.MeasureSpec.EXACTLY) {
            return measureSpec
        } else {
            if (specMode == View.MeasureSpec.AT_MOST) {
                specSize = min(defaultSize, specSize)
            } else {
                specSize = defaultSize
            }
            return View.MeasureSpec.makeMeasureSpec(specSize, View.MeasureSpec.EXACTLY)
        }
    }
}
