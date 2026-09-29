package com.neverreader.util.android.animation

import android.os.SystemClock
import kotlin.math.floor

object AnimationUtil {
    fun getAnimationValue(values: AnimationValues, start: Long, duration: Long) {
        values.elapsedTotal = SystemClock.uptimeMillis() - start
        values.repeatCount = floor(values.elapsedTotal / duration.toDouble()).toInt()
        values.currentElapsed = values.elapsedTotal - (values.repeatCount * duration)
        values.currentPercent = values.currentElapsed / duration.toFloat()
    }

    class AnimationValues {
        var elapsedTotal: Long = 0
        var repeatCount: Int = 0
        var currentElapsed: Long = 0
        var currentPercent: Float = 0f
    }
}
