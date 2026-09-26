package com.neverreader.util.android.drawable

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint

/**
 * A [Paint] that changes its color value based on state. Invoke [.setState] whenever
 * the state changes and it will update its color as needed.
 */
class StatefulPaint() : Paint() {
    private var mColors: ColorStateList? = null

    constructor(context: Context, colorResourceId: Int) : this(
        context.getResources(),
        colorResourceId
    )

    constructor(res: Resources, colorResourceId: Int) : this() {
        mColors = res.getColorStateList(colorResourceId)
    }

    init {
        setAntiAlias(true)
    }

    fun setStatefulColor(color: ColorStateList?, state: IntArray?) {
        mColors = color
        setState(state)
    }

    /**
     * Set the drawable state. The paint will automatically adjust to the color matching that state.
     *
     * @param state
     * @return true if changed, false if the color is the same as before.
     */
    fun setState(state: IntArray?): Boolean {
        val newColor: Int
        if (mColors != null) {
            newColor = mColors!!.getColorForState(state, Color.TRANSPARENT)
        } else {
            newColor = Color.TRANSPARENT
        }

        if (getColor() == newColor) {
            return false
        }

        setColor(newColor)
        return true
    }

    fun hasColor(): Boolean {
        return mColors != null
    }
}
