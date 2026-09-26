package com.neverreader.ui.text

import android.text.InputFilter
import android.text.InputFilter.AllCaps
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.widget.TextView
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

object TextViewUtil {
    /**
     * Set padding so that is visually spaced from edges of the text.
     * TODO test how this works with accented languages
     *
     * @param textView
     * @param horizontalPaddingResId
     * @param verticalPaddingResId
     */
    fun setVisualTextPadding(
        textView: TextView,
        verticalPaddingResId: Int,
        horizontalPaddingResId: Int
    ) {
        // Tweak internal padding to visually look like NeverReader's common space dimensions
        val res = textView.getResources()
        val fm = textView.getPaint().getFontMetrics()
        val vert = res.getDimensionPixelSize(verticalPaddingResId)
        val hori = res.getDimensionPixelSize(horizontalPaddingResId)
        textView.setPadding(
            hori, (vert + fm.top - fm.ascent).toInt(),
            hori, (vert - fm.descent).toInt()
        )
    }

    /**
     * Set padding so that is visually spaced from edges of the text.
     */
    fun setVisualTextPadding(textView: TextView, left: Int, top: Int, right: Int, bottom: Int) {
        // Tweak internal padding to visually look like NeverReader's common space dimensions
        val res = textView.getResources()
        val fm = textView.getPaint().getFontMetrics()
        textView.setPadding(
            left,
            max(0f, top + fm.top - fm.ascent).toInt(),
            right,
            max(0f, (bottom - fm.descent)).toInt()
        )
    }

    /**
     * Modify the padding of this text view so the ascent and descent are visually equal.
     * This changes top and bottom padding. Left and right padding are not changed.
     */
    fun verticallyCenterPadding(textView: TextView) {
        val ascent = ceil(ascent(textView).toDouble()).toInt()
        val descent = ceil(descent(textView).toDouble()).toInt()
        val top: Int
        val bottom: Int
        if (ascent > descent) {
            top = 0
            bottom = ascent - descent
        } else {
            top = descent - ascent
            bottom = 0
        }
        textView.setPadding(
            textView.getPaddingLeft(),
            top,
            textView.getPaddingRight(),
            bottom
        )
    }

    fun descent(view: TextView): Float {
        return view.getPaint().getFontMetrics().descent
    }

    fun bottom(view: TextView): Float {
        return view.getPaint().getFontMetrics().bottom
    }

    fun ascent(view: TextView): Float {
        val fm = view.getPaint().getFontMetrics()
        return abs(fm.ascent - fm.top)
    }

    /**
     * Gets the total height a TextView will have on screen.
     *
     * @param paint a [TextPaint] object, which should include the TextView's [Typeface] and text size.
     * @param text The text that will be placed in the TextView.
     * @param alignment The text alignment.
     * @param viewWidthPx The width of the TextView on the screen.
     * @param lineHeightPx The line height of the TextView.
     * @return the total height in pixels a TextView with the given parameters will cover.
     */
    fun getExpectedTextViewHeight(
        paint: TextPaint,
        text: CharSequence?,
        alignment: Layout.Alignment?,
        viewWidthPx: Int,
        lineHeightPx: Float
    ): Float {
        val metrics = paint.getFontMetrics()
        val fontHeightPx = metrics.descent - metrics.ascent
        return StaticLayout(
            text,
            paint,
            viewWidthPx,
            alignment,
            1f,
            lineHeightPx - fontHeightPx,
            true
        ).getHeight().toFloat()
    }

    /**
     * We've seen a number of text duplication bugs from third party keyboards, including Galaxy, Fleksy, and Kika from
     * our original implementation of a lower casing [InputFilter].
     *
     * This version is an adaptation of [AllCaps], modified to do the opposite, which is
     * to lower case the text, while preserving text spans.
     */
    class AllLowerCase : InputFilter {
        override fun filter(
            source: CharSequence,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val wrapper: CharSequence = CharSequenceWrapper(source, start, end)
            var upperOrTitleFound = false
            val length = end - start
            var i = 0
            var cp: Int
            while (i < length) {
                // We access 'wrapper' instead of 'source' to make sure no code unit beyond 'end' is
                // ever accessed.
                cp = Character.codePointAt(wrapper, i)
                if (Character.isUpperCase(cp) || Character.isTitleCase(cp)) {
                    upperOrTitleFound = true
                    break
                }
                i += Character.charCount(cp)
            }

            if (!upperOrTitleFound) {
                return null // keep original
            }

            // lower case the text
            val lower: CharSequence = wrapper.toString().lowercase(Locale.getDefault())

            if (lower.toString() == wrapper.toString()) {
                return null // Nothing was changed in the lowercase operation, keep original
            } else {
                if (source is Spanned) {
                    // copy spans
                    val spannable = SpannableString(lower)
                    TextUtils.copySpansFrom(source, start, end, null, spannable, 0)
                    return spannable
                } else {
                    return lower
                }
            }
        }
    }

    /**
     * Copy / pasted from [InputFilter.AllCaps], for use in [AllLowerCase]
     */
    private class CharSequenceWrapper(
        private val mSource: CharSequence,
        private val mStart: Int,
        private val mEnd: Int
    ) : CharSequence, Spanned {
        private val mLength: Int = mEnd - mStart

        override val length: Int
            get() = mLength

        override fun get(index: Int): Char {
            if (index < 0 || index >= mLength) {
                throw IndexOutOfBoundsException()
            }
            return mSource[mStart + index]
        }

        override fun subSequence(start: Int, end: Int): CharSequence {
            if (start < 0 || end < 0 || end > mLength || start > end) {
                throw IndexOutOfBoundsException()
            }
            return CharSequenceWrapper(mSource, mStart + start, mStart + end)
        }

        override fun toString(): String {
            return mSource.subSequence(mStart, mEnd).toString()
        }

        override fun <T> getSpans(start: Int, end: Int, type: Class<T?>?): Array<T?>? {
            return (mSource as Spanned).getSpans<T?>(mStart + start, mStart + end, type)
        }

        override fun getSpanStart(tag: Any?): Int {
            return (mSource as Spanned).getSpanStart(tag) - mStart
        }

        override fun getSpanEnd(tag: Any?): Int {
            return (mSource as Spanned).getSpanEnd(tag) - mStart
        }

        override fun getSpanFlags(tag: Any?): Int {
            return (mSource as Spanned).getSpanFlags(tag)
        }

        override fun nextSpanTransition(start: Int, limit: Int, type: Class<*>?): Int {
            return ((mSource as Spanned).nextSpanTransition(mStart + start, mStart + limit, type)
                    - mStart)
        }
    }
}
