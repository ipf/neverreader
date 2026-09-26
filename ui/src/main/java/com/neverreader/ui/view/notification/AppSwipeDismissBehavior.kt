package com.neverreader.ui.view.notification

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.MotionEventCompat
import androidx.core.view.ViewCompat
import androidx.customview.widget.ViewDragHelper
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * A modified version of [SwipeDismissBehavior] which fixes not showing the slide animation while the user is swiping.
 */
class AppSwipeDismissBehavior<V : View> : CoordinatorLayout.Behavior<V>() {
    @IntDef(SWIPE_DIRECTION_START_TO_END, SWIPE_DIRECTION_END_TO_START, SWIPE_DIRECTION_ANY)
    @Retention(
        AnnotationRetention.SOURCE
    )
    private annotation class SwipeDirection

    private var mViewDragHelper: ViewDragHelper? = null
    private var mListener: OnDismissListener? = null
    private var mIgnoreEvents = false
    private var mSensitivity = 0f
    private var mSensitivitySet = false
    private var mSwipeDirection: Int = SWIPE_DIRECTION_ANY
    private var mDragDismissThreshold: Float = DEFAULT_DRAG_DISMISS_THRESHOLD

    /**
     * Callback interface used to notify the application that the view has been dismissed.
     */
    interface OnDismissListener {
        /**
         * Called when `view` has been dismissed via swiping.
         */
        fun onDismiss(view: View?)

        /**
         * Called when the drag state has changed.
         *
         * @param state the new state. One of
         * [ViewDragHelper.STATE_IDLE], [ViewDragHelper.STATE_DRAGGING] or [ViewDragHelper.STATE_SETTLING].
         */
        fun onDragStateChanged(state: Int)
    }

    /**
     * Set the listener to be used when a dismiss event occurs.
     *
     * @param listener the listener to use.
     */
    fun setListener(listener: OnDismissListener?) {
        mListener = listener
    }

    /**
     * Sets the swipe direction for this behavior.
     *
     * @param direction one of the [.SWIPE_DIRECTION_START_TO_END],
     * [.SWIPE_DIRECTION_END_TO_START] or [.SWIPE_DIRECTION_ANY]
     */
    fun setSwipeDirection(@SwipeDirection direction: Int) {
        mSwipeDirection = direction
    }

    /**
     * Set the threshold for telling if a view has been dragged enough to be dismissed.
     *
     * @param distance a ratio of a view's width, values are clamped to 0 >= x <= 1f;
     */
    fun setDragDismissDistance(distance: Float) {
        mDragDismissThreshold = clamp(0f, distance, 1f)
    }

    /**
     * Set the sensitivity used for detecting the start of a swipe. This only takes effect if
     * no touch handling has occured yet.
     *
     * @param sensitivity Multiplier for how sensitive we should be about detecting
     * the start of a drag. Larger values are more sensitive. 1.0f is normal.
     */
    fun setSensitivity(sensitivity: Float) {
        mSensitivity = sensitivity
        mSensitivitySet = true
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent
    ): Boolean {
        when (MotionEventCompat.getActionMasked(event)) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->                 // Reset the ignore flag
                if (mIgnoreEvents) {
                    mIgnoreEvents = false
                    return false
                }

            else -> mIgnoreEvents = !parent.isPointInChildBounds(
                child!!,
                event.getX().toInt(), event.getY().toInt()
            )
        }
        if (mIgnoreEvents) {
            return false
        }
        ensureViewDragHelper(parent)
        return mViewDragHelper!!.shouldInterceptTouchEvent(event)
    }

    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent
    ): Boolean {
        if (mViewDragHelper != null) {
            mViewDragHelper!!.processTouchEvent(event)
            return true
        }
        return false
    }

    private val mDragCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {
        private var mOriginalCapturedViewLeft = 0
        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            mOriginalCapturedViewLeft = child.left
            return true
        }

        override fun onViewDragStateChanged(state: Int) {
            if (mListener != null) {
                mListener!!.onDragStateChanged(state)
            }
        }

        override fun onViewReleased(child: View, xvel: Float, yvel: Float) {
            val childWidth = child.width
            val targetLeft: Int
            var dismiss = false
            if (shouldDismiss(child, xvel)) {
                targetLeft = if (child.left < mOriginalCapturedViewLeft)
                    mOriginalCapturedViewLeft - childWidth
                else
                    mOriginalCapturedViewLeft + childWidth
                dismiss = true
            } else {
                // Else, reset back to the original left
                targetLeft = mOriginalCapturedViewLeft
            }
            if (mViewDragHelper!!.settleCapturedViewAt(targetLeft, child.getTop())) {
                ViewCompat.postOnAnimation(
                    child,
                    SettleRunnable(child, dismiss)
                )
            } else if (dismiss) {
                if (mListener != null) {
                    mListener!!.onDismiss(child)
                }
            }
        }

        private fun shouldDismiss(child: View, xvel: Float): Boolean {
            if (xvel != 0f) {
                val isRtl = (ViewCompat.getLayoutDirection(child)
                        == ViewCompat.LAYOUT_DIRECTION_RTL)
                if (mSwipeDirection == SWIPE_DIRECTION_ANY) {
                    // We don't care about the direction so return true
                    return true
                } else if (mSwipeDirection == SWIPE_DIRECTION_START_TO_END) {
                    // We only allow start-to-end swiping, so the fling needs to be in the
                    // correct direction
                    return if (isRtl) xvel < 0f else xvel > 0f
                } else if (mSwipeDirection == SWIPE_DIRECTION_END_TO_START) {
                    // We only allow end-to-start swiping, so the fling needs to be in the
                    // correct direction
                    return if (isRtl) xvel > 0f else xvel < 0f
                }
            } else {
                val distance = child.left - mOriginalCapturedViewLeft
                val thresholdDistance = Math.round(child.getWidth() * mDragDismissThreshold)
                return abs(distance) >= thresholdDistance
            }
            return false
        }

        override fun getViewHorizontalDragRange(child: View): Int {
            return child.getWidth()
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int): Int {
            val isRtl = (ViewCompat.getLayoutDirection(child)
                    == ViewCompat.LAYOUT_DIRECTION_RTL)
            val min: Int
            val max: Int
            if (mSwipeDirection == SWIPE_DIRECTION_START_TO_END) {
                if (isRtl) {
                    min = mOriginalCapturedViewLeft - child.getWidth()
                    max = mOriginalCapturedViewLeft
                } else {
                    min = mOriginalCapturedViewLeft
                    max = mOriginalCapturedViewLeft + child.getWidth()
                }
            } else if (mSwipeDirection == SWIPE_DIRECTION_END_TO_START) {
                if (isRtl) {
                    min = mOriginalCapturedViewLeft
                    max = mOriginalCapturedViewLeft + child.getWidth()
                } else {
                    min = mOriginalCapturedViewLeft - child.getWidth()
                    max = mOriginalCapturedViewLeft
                }
            } else {
                min = mOriginalCapturedViewLeft - child.getWidth()
                max = mOriginalCapturedViewLeft + child.getWidth()
            }
            return clamp(min, left, max)
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            return child.getTop()
        }
    }

    private fun ensureViewDragHelper(parent: ViewGroup) {
        if (mViewDragHelper == null) {
            mViewDragHelper = if (mSensitivitySet)
                ViewDragHelper.create(parent, mSensitivity, mDragCallback)
            else
                ViewDragHelper.create(parent, mDragCallback)
        }
    }

    private inner class SettleRunnable(private val mView: View, private val mDismiss: Boolean) :
        Runnable {
        override fun run() {
            if (mViewDragHelper != null && mViewDragHelper!!.continueSettling(true)) {
                ViewCompat.postOnAnimation(mView, this)
            } else {
                if (mDismiss) {
                    if (mListener != null) {
                        mListener!!.onDismiss(mView)
                    }
                }
            }
        }
    }

    val dragState: Int
        /**
         * Retrieve the current drag state of this behavior. One of [ViewDragHelper.STATE_IDLE],
         * [ViewDragHelper.STATE_DRAGGING] or [ViewDragHelper.STATE_SETTLING].
         *
         * @return The current drag state
         */
        get() = if (mViewDragHelper != null) mViewDragHelper!!.getViewDragState() else ViewDragHelper.STATE_IDLE

    companion object {
        /**
         * Swipe direction that only allows swiping in the direction of start-to-end. That is
         * left-to-right in LTR, or right-to-left in RTL.
         */
        const val SWIPE_DIRECTION_START_TO_END: Int = 0

        /**
         * Swipe direction that only allows swiping in the direction of end-to-start. That is
         * right-to-left in LTR or left-to-right in RTL.
         */
        const val SWIPE_DIRECTION_END_TO_START: Int = 1

        /**
         * Swipe direction which allows swiping in either direction.
         */
        const val SWIPE_DIRECTION_ANY: Int = 2
        private const val DEFAULT_DRAG_DISMISS_THRESHOLD = 0.5f
        private fun clamp(min: Float, value: Float, max: Float): Float {
            return min(max(min, value), max)
        }

        private fun clamp(min: Int, value: Int, max: Int): Int {
            return min(max(min, value), max)
        }
    }
}
