package com.neverreader.ui.text

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextPaint
import android.text.style.ClickableSpan

abstract class ThemedClickableSpan(
    private val colorStateList: ColorStateList,
    private val stateSource: StateSource
) : ClickableSpan() {
    interface StateSource {
        val drawableState: IntArray?
    }

    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        ds.setUnderlineText(false)
        ds.setColor(colorStateList.getColorForState(stateSource.drawableState, Color.TRANSPARENT))
    }
}
