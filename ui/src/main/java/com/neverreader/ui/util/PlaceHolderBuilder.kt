package com.neverreader.ui.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.Region
import android.graphics.drawable.Drawable
import android.text.TextPaint
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import com.neverreader.ui.R
import com.neverreader.ui.text.Fonts
import com.neverreader.ui.text.Fonts.get
import java.util.Arrays
import kotlin.math.abs

/**
 * Article placeholder images, based on:
 *
 * https://www.figma.com/file/fWihuFRYxl21bUny8zr7we/Listen?node-id=1880%3A182
 */
object PlaceHolderBuilder {
    /**
     * Gets a new [PlaceHolderDrawable] of the provided character, color, and corner.
     *
     * @param context
     * @param character The char to display in the image.
     * @param color A [PktColor] to use for the background and text colors.
     * @param corner A [Corner] at which to align the character.
     * @return a [PlaceHolderDrawable]
     */
    fun getDrawable(context: Context, character: Char, color: PktColor, corner: Corner?): Drawable {
        return PlaceHolderDrawable(context, character, color.background, color.text, corner)
    }

    /**
     * Gets a new [PlaceHolderDrawable].
     *
     * @param context
     * @param id A unique id to use when creating the drawable.  The String's hashcode is used to determine
     * which color and corner will be displayed, ensuring that any String will have the same color / corner
     * configuration.
     * @param character the char to display in the drawable.
     * @return a [PlaceHolderDrawable]
     */
    fun getDrawable(context: Context, id: String, character: Char): Drawable {
        return getDrawable(context, id.hashCode(), character)
    }

    /**
     * Gets a new [PlaceHolderDrawable].
     *
     * @param context
     * @param id A unique id to use when creating the drawable.  This ensures that any id will
     * have the same color / corner configuration.
     * @param character the char to display in the drawable.
     * @return a [PlaceHolderDrawable]
     */
    fun getDrawable(context: Context, id: Int, character: Char): Drawable {
        // simple mod of the id by the number of colors

        val color = Arrays.asList<PktColor>(*PktColor.entries.toTypedArray())
            .get(abs(id) % PktColor.entries.size)

        // same for corners
        val corner = Arrays.asList<Corner?>(*Corner.entries.toTypedArray())
            .get(abs(id) % Corner.entries.size)

        return PlaceHolderDrawable(context, character, color.background, color.text, corner)
    }

    enum class PktColor(
        @field:ColorRes @param:ColorRes val background: Int,
        @field:ColorRes @param:ColorRes val text: Int
    ) {
        CORAL(R.color.nr_coral_5, R.color.nr_themed_coral_2),
        AMBER(R.color.nr_amber_faint, R.color.nr_themed_amber_1),
        TEAL(R.color.nr_teal_6, R.color.nr_themed_teal_3),
        BLUE(R.color.nr_lapis_faint, R.color.nr_themed_lapis_3)
    }

    enum class Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    private class PlaceHolderDrawable(
        context: Context,
        character: Char,
        @ColorRes backgroundColor: Int,
        @ColorRes textColor: Int,
        private val corner: Corner?
    ) : Drawable() {
        private val backgroundPaint = Paint()
        private val textPaint = TextPaint()
        private val textbounds = Rect()

        private val backgroundColor: ColorStateList?
        private val textColor: ColorStateList?
        private val character: String

        private var visibleSideLength = 0
        private var outsideMargin = 0
        private var halfFullLength = 0

        init {
            this.character = character.toString()
            this.backgroundColor = ContextCompat.getColorStateList(context, backgroundColor)
            this.textColor = ContextCompat.getColorStateList(context, textColor)

            backgroundPaint.setAntiAlias(true)

            textPaint.setAntiAlias(true)
            textPaint.setTextAlign(Paint.Align.LEFT)
            textPaint.setTypeface(get(context, Fonts.Font.DOYLE_MEDIUM))

            updatePaint(getState())
        }

        override fun isStateful(): Boolean {
            return true
        }

        override fun onStateChange(state: IntArray): Boolean {
            updatePaint(state)
            return true
        }

        fun updatePaint(state: IntArray?) {
            backgroundPaint.setColor(backgroundColor!!.getColorForState(state, Color.TRANSPARENT))
            textPaint.setColor(textColor!!.getColorForState(state, Color.TRANSPARENT))
        }

        override fun onBoundsChange(bounds: Rect) {
            visibleSideLength = bounds.height()
            outsideMargin =
                ((visibleSideLength * OUTER_REC_MULTIPLIER).toInt() - visibleSideLength) / 2
            halfFullLength = (visibleSideLength + 2 * outsideMargin) / 2
            textPaint.setTextSize((FONT_SIZE_MULTIPLIER * visibleSideLength).toInt().toFloat())
        }

        override fun draw(canvas: Canvas) {
            val bgbounds = getBounds()
            canvas.clipRect(
                bgbounds.left.toFloat(),
                bgbounds.top.toFloat(),
                bgbounds.right.toFloat(),
                bgbounds.bottom.toFloat(),
                Region.Op.INTERSECT
            )
            canvas.drawRect(
                bgbounds.left.toFloat(),
                bgbounds.top.toFloat(),
                bgbounds.right.toFloat(),
                bgbounds.bottom.toFloat(),
                backgroundPaint
            )

            textPaint.getTextBounds(character, 0, 1, textbounds)

            canvas.translate(
                getXTranslate(textbounds.width()).toFloat(),
                getYTranslate(textbounds.height()).toFloat()
            )
            canvas.drawText(
                character,
                -textbounds.left.toFloat(),
                -textbounds.top.toFloat(),
                textPaint
            )
        }

        fun getXTranslate(textWidth: Int): Int {
            val translate: Int
            if (corner == Corner.BOTTOM_LEFT || corner == Corner.TOP_LEFT) {
                // if the width of the text is less than half the full width (including outside margin), align the right side of the text to the center of the view
                if (textWidth < halfFullLength) {
                    translate = (visibleSideLength / 2) - textWidth
                } else {
                    translate = -outsideMargin
                }
            } else { // BOTTOM_RIGHT || TOP_RIGHT
                // if the width of the text is less than half the full width (including outside margin), align the left side of the text to the center of the view
                if (textWidth < halfFullLength) {
                    translate = visibleSideLength / 2
                } else {
                    translate = (visibleSideLength - textWidth) + outsideMargin
                }
            }
            return translate
        }

        fun getYTranslate(textHeight: Int): Int {
            val translate: Int
            // if the height of the text is less than half the full height (including outside margin), just center vertically
            if (textHeight < halfFullLength) {
                translate = (visibleSideLength - textHeight) / 2
            } else {
                // otherwise, follow vertical shifting rules
                if (corner == Corner.BOTTOM_LEFT || corner == Corner.BOTTOM_RIGHT) {
                    translate = (visibleSideLength - textHeight) + outsideMargin
                } else { // TOP_LEFT || TOP_RIGHT
                    translate = -outsideMargin
                }
            }
            return translate
        }

        override fun setAlpha(alpha: Int) {
            backgroundPaint.setAlpha(alpha)
        }

        override fun setColorFilter(cf: ColorFilter?) {
            backgroundPaint.setColorFilter(cf)
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }

        companion object {
            /** The percentage greater that the outside bounds is than the actual size of the image  */
            private const val OUTER_REC_MULTIPLIER = 1.55555555556f

            /** The percentage to increase the size of the font, as compared to the length of a side  */
            private const val FONT_SIZE_MULTIPLIER = 2.00892857143f
        }
    }
}
