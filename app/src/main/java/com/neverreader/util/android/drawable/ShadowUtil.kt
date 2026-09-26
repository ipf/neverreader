package com.neverreader.util.android.drawable

import android.graphics.Paint
import android.widget.TextView
import kotlin.math.min

/**
 * Methods to account for a native crash that occurs if a shadow radius is over 25 px.
 *
 * See: https://code.google.com/p/android/issues/detail?id=73886
 */
object ShadowUtil {
    const val MAX_RADIUS: Float = 25f

    fun getSafeRadius(radius: Float): Float {
        return min(MAX_RADIUS, radius)
    }

    fun setShadowLayer(textView: TextView, radius: Float, dx: Float, dy: Float, color: Int) {
        textView.setShadowLayer(getSafeRadius(radius), dx, dy, color)
    }

    fun setShadowLayer(paint: Paint, radius: Float, dx: Float, dy: Float, color: Int) {
        paint.setShadowLayer(getSafeRadius(radius), dx, dy, color)
    }
}
