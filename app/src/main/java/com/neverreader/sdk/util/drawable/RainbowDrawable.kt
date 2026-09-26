package com.neverreader.sdk.util.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.view.animation.AccelerateInterpolator
import android.view.animation.Interpolator
import com.neverreader.app.App.Companion.getContext as appContextFn
import com.neverreader.app.settings.Theme
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil
import com.neverreader.util.android.FormFactor.dpToPx
import com.neverreader.util.android.animation.AnimationUtil.AnimationValues
import com.neverreader.util.android.animation.AnimationUtil.getAnimationValue
import java.util.Arrays

/**
 *
 */
class RainbowDrawable(callback: Callback?) : Drawable() {
    private enum class AnimateState {
        /** Not animating  */
        IDLE,

        /** Moving the static rainbow bar out of the way  */
        STARTING,

        /** Going at normal consistent speed  */
        ACTIVE,

        /** Moving the static rainbow bar out of the way  */
        STOPPING_BEFORE_ACTIVE,

        /** The static rainbow bar is returning  */
        STOPPING
    }

    private val mSegments: FloatArray

    private var mIsBorderVisible = true
    private var mIsDark = false

    /**
     * Whether to draw dimmed. The host view sets this from the activity theme;
     * it used to be scraped out of the view's drawable state, which depended on
     * the state_dark attribute that no longer exists.
     */
    var isDark: Boolean
        get() = mIsDark
        set(value) {
            if (mIsDark == value) return
            mIsDark = value
            applyAlphas()
        }
    private var mAnimationState = AnimateState.IDLE
    private var mAnimationStart: Long = 0
    private var mAnimationDuration: Long = 0
    private val mAnimationValues = AnimationValues()
    private var mAnimationLastRound = 0

    /**
     * If you are going to want to animate this rainbow, with [.startProgressAnimation], make sure you supply
     * a [Callback].
     *
     *
     * Be sure to also override [View.verifyDrawable] in that case too.
     * @param callback
     */
    init {
        setCallback(callback)
        mSegments = FloatArray(ANIMATION_SEGMENT_COUNT)
    }

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(cf: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        super.onStateChange(state)

        mIsDark = isDark

        applyAlphas()
        return true
    }

    private fun applyAlphas() {
        val alpha = if (mIsDark) DARK_MODE_ALPHA else 255
        PAINT_MINT.alpha = alpha
        PAINT_TURQUOISE.alpha = alpha
        PAINT_GOLD.alpha = alpha
        PAINT_CORAL.alpha = alpha
    }

    fun setBorderVisible(visible: Boolean) {
        mIsBorderVisible = visible
    }

    fun startProgressAnimation() {
        mAnimationState =
            AnimateState.ACTIVE // No longer using the start animation, so just go straight into the active state
        mAnimationStart = SystemClock.uptimeMillis()
        invalidateSelf()
    }

    fun stopProgressAnimation() {
        if (mAnimationState == AnimateState.IDLE || mAnimationState == AnimateState.STOPPING) {
            return
        }

        if (mAnimationState == AnimateState.STARTING) {
            mAnimationState = AnimateState.STOPPING_BEFORE_ACTIVE
        } else {
            mAnimationState = AnimateState.STOPPING
        }

        getAnimationValue(mAnimationValues, mAnimationStart, mAnimationDuration)
        mAnimationLastRound = mAnimationValues.repeatCount + 1

        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val bounds = getBounds()
        val width = bounds.width().toFloat()
        val height = bounds.height().toFloat()

        if (mIsDark) {
            // draw black background behind rainbow
            canvas.drawRect(bounds, PAINT_BLACK)
        }

        var values: AnimationValues? = null
        if (mAnimationState != AnimateState.IDLE) {
            // Animating, get the latest values
            values = mAnimationValues
            getAnimationValue(values, mAnimationStart, mAnimationDuration)


            // Check if the animation needs to change state
            if (mAnimationState == AnimateState.STARTING && values.repeatCount >= 1) {
                // The start animation has completed
                mAnimationState = AnimateState.ACTIVE
            } else if (mAnimationState == AnimateState.STOPPING_BEFORE_ACTIVE && values.repeatCount == mAnimationLastRound) {
                // Completed the starting round, ready to start the stop animation
                mAnimationState = AnimateState.STOPPING
            } else if (mAnimationState == AnimateState.STOPPING && values.repeatCount > mAnimationLastRound) {
                // The end animation has completed
                mAnimationState = AnimateState.IDLE
            }
        }


        /*
		 * Note, this does not currently respect the bounds of the view. It assumes this will always go edge to edge.
		 * If you need it to respect the bounds, you will need to update this as it will draw out of bounds as is.
		 */
        val y = bounds.top.toFloat()
        var x = bounds.left.toFloat()
        if (mAnimationState == AnimateState.IDLE) {
            // Static Rainbow Bar
            drawStaticRainbow(canvas, x, y, width, height)
        } else {
            // Animating values should be not null as set above.
            // Calculate the position of the segments. Segments are the small dividers/spaces between bars.
            val positions = mSegments
            val len = positions.size
            val segmentSize = 1f / len
            for (i in 0..<len) {
                var p = values!!.currentPercent + segmentSize * i
                if (p > 1) {
                    p -= 1f
                }
                positions[i] = ANIMATION_INTERPOLATOR.getInterpolation(p) * width
                positions[i] -= DimenUtil.dpToPx(appContextFn(), 2f)
            }
            val center = positions[0]
            Arrays.sort(positions)


            // Determine which color to use for segments before the center point and after
            val colorIndex: Int = if (values!!.repeatCount > ANIMATION_COLORS.size - 1) {
                values.repeatCount % ANIMATION_COLORS.size
            } else {
                values.repeatCount
            }
            val rightPaint: Paint?
            if (mAnimationState == AnimateState.STARTING || mAnimationState == AnimateState.STOPPING_BEFORE_ACTIVE) {
                rightPaint = null
                drawClippedRainbow(canvas, x, y, width, height, x + center, x + width)
            } else {
                rightPaint = ANIMATION_COLORS[colorIndex]
            }
            val leftPaint: Paint?
            if (mAnimationState == AnimateState.STOPPING && values.repeatCount >= mAnimationLastRound) {
                leftPaint = null
                val speedUp: Float =
                    ANIMATION_INTERPOLATOR.getInterpolation(values.currentPercent) * (width * 1.75f)
                drawClippedRainbow(canvas, x, y, width, height, x, x + center + speedUp)
                x += speedUp // offset the segments so they move faster at the end of the animation.
            } else {
                leftPaint =
                    ANIMATION_COLORS[if (colorIndex < ANIMATION_COLORS.size - 1) colorIndex + 1 else 0]
            }


            // Draw the segments
            for (i in 0..<len) {
                var left: Float
                if (i == 0) {
                    left = 0f
                } else {
                    left = positions[i - 1] + DimenUtil.dpToPx(appContextFn(), 2f)
                    if (left < 0) {
                        left = 0f
                    }
                }

                if (rightPaint == null && left >= center) {
                    break
                }

                val right = positions[i]
                if (right > left) {
                    val paint = if (left >= center) rightPaint else leftPaint
                    if (paint != null) {
                        canvas.drawRect(x + left, y, x + right, y + height, paint)
                    }
                }
            }
            val last = positions[len - 1] + DimenUtil.dpToPx(appContextFn(), 2f)
            if (last < width) {
                val paint = if (last >= center) rightPaint else leftPaint
                if (paint != null) {
                    canvas.drawRect(x + last, y, width, y + height, paint)
                }
            }
        }

        if (mIsBorderVisible && !mIsDark) {
            // REVIEW is this still needed? canvas.drawLine(0, height+1, width, height+1, PAINT_WHITE);
        }

        if (mAnimationState != AnimateState.IDLE) {
            invalidateSelf()
        }
    }

    private fun drawStaticRainbow(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float
    ) {
        val quarter = width / 4.0f
        drawRainbowSegment(canvas, left + 0, top, quarter, height, PAINT_MINT)
        drawRainbowSegment(canvas, left + quarter, top, quarter, height, PAINT_TURQUOISE)
        drawRainbowSegment(canvas, left + quarter * 2, top, quarter, height, PAINT_CORAL)
        drawRainbowSegment(canvas, left + quarter * 3, top, quarter, height, PAINT_GOLD)
    }

    private fun drawClippedRainbow(
        canvas: Canvas,
        left: Float,
        top: Float,
        width: Float,
        height: Float,
        clipLeft: Float,
        clipRight: Float
    ) {
        val quarter = width / 4.0f
        drawRainbowSegment(canvas, left + 0, top, quarter, height, clipLeft, clipRight, PAINT_MINT)
        drawRainbowSegment(
            canvas,
            left + quarter,
            top,
            quarter,
            height,
            clipLeft,
            clipRight,
            PAINT_TURQUOISE
        )
        drawRainbowSegment(
            canvas,
            left + quarter * 2,
            top,
            quarter,
            height,
            clipLeft,
            clipRight,
            PAINT_CORAL
        )
        drawRainbowSegment(
            canvas,
            left + quarter * 3,
            top,
            quarter,
            height,
            clipLeft,
            clipRight,
            PAINT_GOLD
        )
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)


        // Calculate animation duration base
        val mod = bounds.width() / dpToPx(320f).toFloat()
        mAnimationDuration = (mod * ANIMATION_DURATION_AT_320DP).toLong()
        invalidateSelf()
    }

    companion object {
        val GREEN: Int
        val BLUE: Int
        val RED: Int
        val GOLD: Int

        var DARK_MODE_ALPHA: Int = (255 * 0.7f).toInt()

        private val PAINT_MINT: Paint
        private val PAINT_TURQUOISE: Paint
        private val PAINT_CORAL: Paint
        private val PAINT_GOLD: Paint
        private val PAINT_WHITE: Paint
        private val PAINT_BLACK: Paint

        private const val ANIMATION_SEGMENT_COUNT = 4

        /** The time milliseconds in which one segment would move completely across the screen on a normal phone in portrait  */
        private const val ANIMATION_DURATION_AT_320DP: Long = 1800
        private val ANIMATION_INTERPOLATOR: Interpolator = AccelerateInterpolator(1.6f)
        private val ANIMATION_COLORS: Array<Paint?>

        init {
            val res = appContextFn().resources
            BLUE = res.getColor(R.color.nr_teal_4)
            GREEN = res.getColor(R.color.nr_teal_3)
            RED = res.getColor(R.color.nr_coral_2)
            GOLD = res.getColor(R.color.amber_30)

            PAINT_MINT = newColorPaint(GREEN)
            PAINT_TURQUOISE = newColorPaint(BLUE)
            PAINT_CORAL = newColorPaint(RED)
            PAINT_GOLD = newColorPaint(GOLD)
            PAINT_WHITE = newColorPaint(Color.WHITE)
            PAINT_BLACK = newColorPaint(Color.BLACK)
            PAINT_WHITE.strokeWidth = 0f

            ANIMATION_COLORS = arrayOf<Paint>(PAINT_TURQUOISE, PAINT_CORAL, PAINT_GOLD, PAINT_MINT) as Array<Paint?>
        }

        private fun drawRainbowSegment(
            canvas: Canvas,
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            paint: Paint
        ) {
            canvas.drawRect(left, top, left + width, top + height, paint)
        }

        private fun drawRainbowSegment(
            canvas: Canvas,
            left: Float,
            top: Float,
            width: Float,
            height: Float,
            clipLeft: Float,
            clipRight: Float,
            paint: Paint
        ) {
            var left = left
            if (left < clipLeft) {
                left = clipLeft
            }
            var right = left + width
            if (right > clipRight) {
                right = clipRight
            }
            if (left < right) {
                canvas.drawRect(left, top, right, top + height, paint)
            }
        }

        private fun newColorPaint(color: Int): Paint {
            val paint = Paint()
            paint.color = color
            return paint
        }
    }
}
