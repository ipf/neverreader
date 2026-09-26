package com.neverreader.util.android.drawable

import android.graphics.Color

object ColorUtil {

    /**
     * Convert [0...1] to [0...255]
     * @param percent
     * @return
     */
    fun to255(percent: Float): Int {
        return (255 * percent).toInt()
    }

    /**
     * Returns the color with the alpha channel set to the provided value.
     */
    fun setAlpha(alpha: Float, color: Int): Int {
        return setAlpha(to255(alpha), color)
    }

    /**
     * Returns the color with the alpha channel set to the provided value.
     */
    fun setAlpha(alpha: Int, color: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }

    fun toString(color: Int): String {
        return Color.alpha(color)
            .toString() + "|" + Color.red(color) + "|" + Color.green(color) + "|" + Color.blue(color)
    }
}
