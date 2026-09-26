package com.neverreader.ui.view.progress

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.IntrinsicSizeHelper
import com.neverreader.ui.util.NestedColorStateList
import org.apache.commons.lang3.ArrayUtils
import java.util.Random
import kotlin.math.max

class RainbowProgressCircleView : View, AnimatorUpdateListener {
    private val mIntrinsicSizeHelper = IntrinsicSizeHelper(dpToPxInt(getContext(), 55f))
    private val mBounds = RectF()
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mRandom = Random()

    private var mColorStateLists = arrayOf<ColorStateList?>(
        NestedColorStateList.get(getContext(), R.color.nr_themed_teal_4),
        NestedColorStateList.get(getContext(), R.color.nr_themed_teal_3),
        NestedColorStateList.get(getContext(), R.color.nr_themed_coral_2),
        NestedColorStateList.get(getContext(), R.color.nr_themed_amber_1)
    )

    private var mIsStarting = false
    private var mRotationAnimator: ValueAnimator? = null
    private var mSweepAngleAnimator: ValueAnimator? = null
    private var mProgressAnimator: ValueAnimator? = null
    private var mCurrentPrimaryColorIndex = 0
    private var mLastSweepAngle = 0f

    private var mIsIndeterminate = true
    private var mStartAsArc = true
    private var mIsAttached = false
    private var mProgress = 0f

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initAttrs(attrs, defStyleAttr)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initAttrs(attrs, 0)
    }

    constructor(context: Context?) : super(context) {
        initAttrs(null, 0)
    }

    private fun initAttrs(attrs: AttributeSet?, defStyleAttr: Int) {
        if (isInEditMode()) {
            return
        }

        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(
                attrs,
                R.styleable.RainbowProgressCircleView,
                defStyleAttr,
                0
            )
            if (a.getBoolean(
                    R.styleable.RainbowProgressCircleView_progressColorsExcludeCoral,
                    false
                )
            ) {
                mColorStateLists = ArrayUtils.remove<ColorStateList?>(mColorStateLists, 2)
            }
            mStartAsArc =
                a.getBoolean(R.styleable.RainbowProgressCircleView_progressStartAsArc, true)
            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = mIntrinsicSizeHelper.applyWidth(widthMeasureSpec)
        heightMeasureSpec = mIntrinsicSizeHelper.applyHeight(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setProgress(progress: Float) {
        setProgressIndeterminate(false)

        var start = mProgress
        if (mProgressAnimator != null) {
            start = (mProgressAnimator!!.getAnimatedValue() as Float)
            mProgressAnimator!!.cancel()
        }

        mProgressAnimator = ValueAnimator.ofFloat(
            start,
            progress
        ) // TODO can we just change the values instead of creating a new animator?
        mProgressAnimator!!.setInterpolator(DecelerateInterpolator())
        mProgressAnimator!!.setDuration(400)
        mProgressAnimator!!.start()
        mProgress = progress

        invalidate()
    }

    fun setProgressIndeterminate(indeterminate: Boolean) {
        mIsIndeterminate = indeterminate
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mIsAttached = true
        updateAnimationStatus()
    }

    private fun newDegreesAnimator(duration: Long): ValueAnimator {
        val animator = ValueAnimator.ofFloat(0f, 360f)
        animator.setDuration(duration)
        animator.setRepeatCount(ValueAnimator.INFINITE)
        animator.setRepeatMode(ValueAnimator.RESTART)
        animator.setInterpolator(LinearInterpolator())
        return animator
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mIsAttached = false
        updateAnimationStatus()
    }

    private fun startAnimation() {
        if (mSweepAngleAnimator != null) {
            return  // Already running.
        }

        mPaint.setStyle(Paint.Style.STROKE)

        mIsStarting = mStartAsArc
        mCurrentPrimaryColorIndex = mRandom.nextInt(mColorStateLists.size)

        mSweepAngleAnimator = newDegreesAnimator(SWEEP_SPEED)
        mRotationAnimator = newDegreesAnimator(ROTATION_SPEED)

        mSweepAngleAnimator!!.addUpdateListener(this)
        mSweepAngleAnimator!!.start()
        mRotationAnimator!!.start()
    }

    private fun cancelAnimation() {
        if (mSweepAngleAnimator == null) {
            return  // Already canceled
        }
        mSweepAngleAnimator!!.removeAllUpdateListeners()
        mSweepAngleAnimator!!.cancel()
        mRotationAnimator!!.cancel()

        mSweepAngleAnimator = null
        mRotationAnimator = null
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        updateAnimationStatus()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimationStatus()
    }

    override fun onAnimationUpdate(animator: ValueAnimator) {
        invalidate()
    }

    private fun updateAnimationStatus(): Boolean {
        if (getVisibility() == VISIBLE && mIsAttached && isShown()) {
            startAnimation()
            return true
        } else {
            cancelAnimation()
            return false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        mPaint.setStrokeWidth(h * STROKE_RATIO)
        mBounds.set(0f, 0f, w.toFloat(), h.toFloat())
        mBounds.inset(mPaint.getStrokeWidth() / 2, mPaint.getStrokeWidth() / 2)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (mSweepAngleAnimator == null) {
            return
        }

        var sweepAngle = (mSweepAngleAnimator!!.getAnimatedValue() as Float)
        val rotate = (mRotationAnimator!!.getAnimatedValue() as Float)

        if (!mIsIndeterminate) {
            val progress = (mProgressAnimator!!.getAnimatedValue() as Float)
            sweepAngle = max(progress * 360, 5f)
        }

        if (sweepAngle < mLastSweepAngle) {
            // It reset/repeated
            mIsStarting = false
            mCurrentPrimaryColorIndex++
            if (mCurrentPrimaryColorIndex >= mColorStateLists.size) {
                mCurrentPrimaryColorIndex = 0
            }


            // Recheck if still visible
            val stillRunning = updateAnimationStatus()
            if (!stillRunning) {
                return
            }
        }

        canvas.save()
        canvas.rotate(rotate, mBounds.centerX(), mBounds.centerY())

        mPaint.setColor(
            mColorStateLists[mCurrentPrimaryColorIndex]!!.getColorForState(
                getDrawableState(),
                Color.TRANSPARENT
            )
        )
        canvas.drawArc(
            mBounds,
            0f,
            sweepAngle,
            false,
            mPaint
        ) // For progress mode (!mIsIndeterminate) this is the progress so far


        // Draw the rest of the circle
        val fillRestOfCircle = !mIsStarting || !mIsIndeterminate
        if (fillRestOfCircle) {
            val color: Int
            val alpha: Int
            if (mIsIndeterminate) {
                // Get another color to draw along side the primary/growing one.
                var secondaryIndex = mCurrentPrimaryColorIndex - 1
                if (secondaryIndex < 0) {
                    secondaryIndex = mColorStateLists.size - 1
                }
                color = mColorStateLists[secondaryIndex]!!.getColorForState(
                    getDrawableState(),
                    Color.TRANSPARENT
                )
                alpha = 255
            } else {
                // Draw the primary color, but translucent
                color = mColorStateLists[mCurrentPrimaryColorIndex]!!.getColorForState(
                    getDrawableState(),
                    Color.TRANSPARENT
                )
                alpha = 50
            }
            mPaint.setColor(color)
            mPaint.setAlpha(alpha)
            canvas.drawArc(mBounds, sweepAngle, 360 - sweepAngle + 1, false, mPaint)
        } else {
            // During the start animation we only draw one arc.
        }

        canvas.restore()

        mLastSweepAngle = sweepAngle
    }


    companion object {
        private const val SWEEP_SPEED: Long = 1250
        private const val ROTATION_SPEED: Long = 1750

        /** The percentage of the view's height that gives you the stroke width  */
        private const val STROKE_RATIO = 0.08f
    }
}
