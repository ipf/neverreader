package com.neverreader.ui.text

import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.TypefaceSpan

/**
 * A [TypefaceSpan] that also carries a Typeface, so the family name is not
 * re-resolved. [mFakeEffectsEnabled] is true for allowFakeEffects.
 */
class CustomTypefaceSpan(
    family: String?,
    private val mNewType: Typeface,
    private val mFakeEffectsEnabled: Boolean = true
) : TypefaceSpan(family) {
    /**
     * [.CustomTypefaceSpan] with "" and true for other params.
     */
    constructor(type: Typeface) : this("", type)

    /**
     * [.CustomTypefaceSpan] with "" for family
     */
    constructor(type: Typeface, allowFakeEffects: Boolean) : this("", type, allowFakeEffects)

    /**
     * @param family @see [TypefaceSpan]
     * @param type The typeface to apply to this span
     * @param allowFakeEffects true if fake bold and italics are allowed. If you are supplying a variant typeface like an italic or bold font you likely want this set to false.
     */

    override fun updateDrawState(ds: TextPaint) {
        applyCustomTypeFace(ds, mNewType, mFakeEffectsEnabled)
    }

    override fun updateMeasureState(paint: TextPaint) {
        applyCustomTypeFace(paint, mNewType, mFakeEffectsEnabled)
    }

    companion object {
        private fun applyCustomTypeFace(paint: Paint, tf: Typeface, fakeEffectsEnabled: Boolean) {
            if (fakeEffectsEnabled) {
                val oldStyle: Int
                val old = paint.getTypeface()
                if (old == null) {
                    oldStyle = 0
                } else {
                    oldStyle = old.getStyle()
                }

                val fake = oldStyle and tf.getStyle().inv()
                if ((fake and Typeface.BOLD) != 0) {
                    paint.setFakeBoldText(true)
                }

                if ((fake and Typeface.ITALIC) != 0) {
                    paint.setTextSkewX(-0.25f)
                }
            }

            paint.setTypeface(tf)
        }
    }
}
