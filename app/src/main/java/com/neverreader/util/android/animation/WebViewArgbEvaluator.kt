/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.neverreader.util.android.animation

import android.animation.TypeEvaluator

/**
 * This evaluator can be used to perform type interpolation between integer
 * values that represent ARGB colors.
 *
 * This is a copy of [android.animation.ArgbEvaluator] but without the conversion between sRGB and linear
 * to match the web view implementation. (So that framework and web transitions can look identical in Reader.)
 */
class WebViewArgbEvaluator : TypeEvaluator<Int> {
    /**
     * This function returns the calculated in-between value for a color
     * given integers that represent the start and end values in the four
     * bytes of the 32-bit int. Each channel is separately linearly interpolated
     * and the resulting calculated values are recombined into the return value.
     *
     * @param fraction The fraction from the starting to the ending values
     * @param startValue A 32-bit int value representing colors in the
     * separate bytes of the parameter
     * @param endValue A 32-bit int value representing colors in the
     * separate bytes of the parameter
     * @return A value that is calculated to be the linearly interpolated
     * result, derived by separating the start and end values into separate
     * color channels and interpolating each one separately, recombining the
     * resulting values in the same way.
     */
    override fun evaluate(fraction: Float, startValue: Int, endValue: Int): Int {
        val startA = ((startValue shr 24) and 0xff) / 255.0f
        val startR = ((startValue shr 16) and 0xff) / 255.0f
        val startG = ((startValue shr 8) and 0xff) / 255.0f
        val startB = (startValue and 0xff) / 255.0f

        val endA = ((endValue shr 24) and 0xff) / 255.0f
        val endR = ((endValue shr 16) and 0xff) / 255.0f
        val endG = ((endValue shr 8) and 0xff) / 255.0f
        val endB = (endValue and 0xff) / 255.0f


        // compute the interpolated color
        var a = startA + fraction * (endA - startA)
        var r = startR + fraction * (endR - startR)
        var g = startG + fraction * (endG - startG)
        var b = startB + fraction * (endB - startB)

        // convert back to the [0..255] range
        a = a * 255.0f
        r = r * 255.0f
        g = g * 255.0f
        b = b * 255.0f

        return Math.round(a) shl 24 or (Math.round(r) shl 16) or (Math.round(g) shl 8) or Math.round(
            b
        )
    }
}
