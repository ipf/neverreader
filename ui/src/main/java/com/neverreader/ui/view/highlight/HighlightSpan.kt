package com.neverreader.ui.view.highlight

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.Spannable
import android.text.Spanned
import android.text.TextUtils
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan
import android.text.style.UpdateAppearance
import android.widget.TextView
import com.neverreader.ui.util.DimenUtil.dpToPx
import kotlin.math.max
import kotlin.math.min

/**
 * Provides custom sizing of the highlighted or selection background color on text.
 *
 *
 * A typical [android.text.style.BackgroundColorSpan] only allows you to change the color,
 * not the size or positioning of the background rectangle. This gives finer control.
 *
 *
 * To use this class, you must apply this span to the entire length of the text, then
 * add [HighlightedRegion] spans to the areas to highlight.
 * You can use [.attach] as a convenience method.
 */
class HighlightSpan(
    private val sidePadding: Int,
    private val ascent: Float,
    private val descent: Float,
    private val highlightColor: ColorStateList,
    private val stateSource: StateSource,
    private val metrics: MetricsSource
) : LeadingMarginSpan.Standard(sidePadding), LineBackgroundSpan {
    private val rect = RectF()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    /**
     * A span with default sizing and states for typical for NeverReader view, but provided colors.
     */
    constructor(view: TextView, color: ColorStateList) : this(
        0,
        dpToPx(view.getContext(), 1f),
        dpToPx(view.getContext(), 2f),
        color,
        HighlightSpan.StateSource { view.getDrawableState() },
        MetricsSource { view.getPaint().getFontMetrics() })

    /**
     * @param sidePadding padding of highlight to left and right
     * @param ascent padding of highlight above the font's top
     * @param descent padding of highlight below the font's baseline
     * @param highlightColor The color
     * @param stateSource Getter of the current state (used to determine the right color)
     * @param metrics Getter of the current FontMetrics
     */
    init {
        paint.setStyle(Paint.Style.FILL)
    }

    /**
     * Add this span to the spannable. It will add this span covering the entire
     * spannable, and create a [HighlightedRegion] span over the region to highlight.
     *
     * @param spannable Where to add it.
     * @param startInclusive The first character of the region to highlight.
     * @param endExclusive 1 past the last character to highlight.
     */
    fun attach(spannable: Spannable, startInclusive: Int, endExclusive: Int) {
        spannable.setSpan(
            HighlightedRegion(),
            startInclusive,
            endExclusive,
            Spanned.SPAN_INCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(this, 0, spannable.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    override fun drawBackground(
        c: Canvas,
        p: Paint,
        left: Int,
        right: Int,
        top: Int,
        baseline: Int,
        bottom: Int,
        text: CharSequence,
        start: Int,
        end: Int,
        lnum: Int
    ) {

        val highlights = (text as Spanned).getSpans(start, end, HighlightedRegion::class.java)
        for (highlight in highlights) {
            // Determine the left and right edge of the highlight to draw, it may extend outside of this line
            val spanStart = (text as Spanned).getSpanStart(highlight)
            val spanEnd = (text as Spanned).getSpanEnd(highlight)
            if (spanStart > end || spanEnd < start) {
                // Not in this line
                continue
            } else if (spanStart <= start) {
                // Can draw to edge
                rect.left = left.toFloat()
            } else {
                // Need to figure out how much to inset
                rect.left = left + p.measureText(text, start, spanStart)
            }
            rect.left -= sidePadding.toFloat()

            rect.right = (rect.left
                    + p.measureText(
                text,
                max(spanStart, start),  // Either the beginning of the line or the span
                min(spanEnd, end)
            ) // Either the end of the line or the span
                    + sidePadding * 2)

            val fm = metrics.metrics()
            rect.top = baseline + fm.ascent - ascent
            rect.bottom = baseline + descent


            // TODO could consider expanding the rect each iteration and then draw once at the end
            paint.setColor(
                highlightColor.getColorForState(
                    stateSource.drawableState(),
                    Color.TRANSPARENT
                )
            )
            c.drawRect(rect, paint)
        }
    }

    fun interface StateSource {
        fun drawableState(): IntArray?
    }

    fun interface MetricsSource {
        fun metrics(): Paint.FontMetrics
    }

    class HighlightedRegion : UpdateAppearance

    companion object {
        /**
         * Remove all [HighlightSpan] and [HighlightedRegion] spans from this text.
         * @param text
         */
        fun removeAll(text: Spannable?) {
            if (TextUtils.isEmpty(text)) {
                return
            }
            for (o in text!!.getSpans<HighlightSpan?>(
                0,
                text.length,
                HighlightSpan::class.java
            )) {
                text.removeSpan(o)
            }
            for (o in text.getSpans<HighlightedRegion?>(
                0,
                text.length,
                HighlightedRegion::class.java
            )) {
                text.removeSpan(o)
            }
        }
    }
}
