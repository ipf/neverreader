package com.neverreader.util.android.view

import android.view.MotionEvent
import com.neverreader.util.android.webkit.BaseWebView
import com.neverreader.util.java.Logs
import kotlin.math.abs


/**
 * A helper class for determining the start and stop of scrolling in a view.
 *
 * The view should override onTouchEvent(), onScrollChanged(), and onDraw() and then from each of those overridden methods call the ones of same name here.
 *
 * The way this detection works is as follows.
 *
 * 1. The start of a scroll is detected in onScrollChanged.
 * 2. After onScrollChanged is received, there will be an onDraw. onDraw always follows a onScrollChanged event.
 * 3. If it is scrolling, the onDraw will invalidate itself to ensure onDraw is called again in the near future.
 * 4. If the next onDraw is received without another onScrollChanged in between, it is pretty confident scrolling has completed.
 * 5. However, as a fail safe for things like possible lag, it will invalidate it one more time but by posting it to ensure it happens in a following cycle.
 * 6. If once again onDraw happens without a onScrollChanged in between, it considers the scrolling to have stopped.
 *
 * We can investigate if the extra invalidation has any negative performance issues, but I think invalidation just flags it to be drawn and shouldn't cause too much of an issue.
 * It will cause the view to be redrawn two extra times more than needed at the end of the scroll, but that isn't bad.
 *
 *
 * @author max
 */
class ScrollTracker(private val mView: BaseWebView, private val mDirectionChangeSlop: Int) {
    private val AT_REST = -1
    private val SCROLLING = 0
    private val SCROLLING_COMFIRMATIONS_REQUIRED = 2

    protected var mListener: OnScrollListener? = null

    private var mScrollingState = AT_REST
    var isTouching: Boolean = false
        private set

    private var mDirection = 0
    private var mLastDirectionChangedAtY = 0

    private var mOverscrolled = false
    private var mOverscrollDirection = 0
    private var mDownY = 0f

    fun onTouchEvent(ev: MotionEvent) {
        when (ev.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                if (DEBUG) Logs.v("Scroll", "touch down")
                this.isTouching = true
                mDownY = ev.getY()
                val y = mView.getScrollY()

                if (y <= 0) {
                    mOverscrollDirection = -1
                } else if (y >= mView.maxContentScrollY) {
                    mOverscrollDirection = 1
                } else {
                    mOverscrollDirection = 0
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (DEBUG) Logs.v("Scroll", "touch move")
                this.isTouching = true

                if (mOverscrollDirection != 0 && !mOverscrolled) {
                    val moved = mDownY - ev.getY()
                    if (abs(moved) > mDirectionChangeSlop && Integer.signum(moved.toInt()) == mOverscrollDirection) {
                        mOverscrolled = true
                        if (mListener != null) {
                            mListener!!.onOverscroll(mOverscrollDirection)
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (DEBUG) Logs.v("Scroll", "touch up")
                this.isTouching = false
                mOverscrollDirection = 0
                mOverscrolled = false
                if (mScrollingState != AT_REST) {
                    mView.invalidate()
                }
            }
        }
    }

    fun onScrollChanged(x: Int, y: Int, oldX: Int, oldY: Int) {
        if (DEBUG) Logs.v("Scroll", "onScroll to " + x + ", " + y + " from " + oldX + ", " + oldY)

        if (mScrollingState == AT_REST) {
            if (DEBUG) Logs.e("Scroll", "onScrollStart")

            mLastDirectionChangedAtY = oldY
            mDirection = 0

            if (mListener != null) {
                mListener!!.onScrollStart()
            }
        }
        mScrollingState = SCROLLING

        val direction = Integer.signum(y - oldY)
        if (DEBUG) Logs.e("Scroll", "direction:" + direction + " mDirection:" + mDirection)

        if (direction != 0 && mDirection != direction) {
            val change = y - mLastDirectionChangedAtY
            if (DEBUG) Logs.e(
                "Scroll",
                "direction change: " + change + " lastY:" + mLastDirectionChangedAtY
            )

            if (abs(change) > mDirectionChangeSlop) {
                if (DEBUG) Logs.e("Scroll", "direction CHANGED ")

                if (onScrollDirectionChanged(direction)) {
                    mDirection = direction
                }
                mLastDirectionChangedAtY = y
            }
        } else {
            mLastDirectionChangedAtY = y
        }

        //		if (mListener != null) { REVIEW this wasn't invoked before.... why?
//			mListener.onScroll(x, y, oldX, oldY);
//		}
    }

    private fun onScrollDirectionChanged(direction: Int): Boolean {
        if (DEBUG) Logs.v("Scroll", "onScrollDirectionChanged " + direction)

        if (mListener != null) {
            return mListener!!.onScrollDirectionChanged(direction, this.isTouching)
        }
        return false
    }

    fun onDraw() {
        //if (DEBUG) Dev.w("Scroll", "onDraw");

        if (mScrollingState == AT_REST) {
            return  // Don't need to check for end of scrolling
        }

        if (this.isTouching) {
            if (mScrollingState > SCROLLING) {
                // Set back to the SCROLLING state while the user is touching the screen.
                mScrollingState = SCROLLING
            } else {
                // We don't need to check for the end of a scroll until the user stops touching the screen.
                return
            }
        }

        if (mScrollingState < SCROLLING_COMFIRMATIONS_REQUIRED - 1) {
            // Might still be scrolling. We invalidate the view to see if we get an onScrolled call between now and the next draw.
            mScrollingState++
            mView.invalidate()
        } else if (mScrollingState == SCROLLING_COMFIRMATIONS_REQUIRED - 1) {
            /*
			 *  This means two onDraws occurred without a onScroll event between them. Likely scrolling is complete, but we post one last invalidate to
			 *  make sure that it isn't just caused by lag.
			 */
            mScrollingState++
            mView.postInvalidate()
        } else {
            // If another onDraw has occurred, we can pretty much assume the scroll is complete.
            if (DEBUG) Logs.e("Scroll", "onScrollFinished")

            mScrollingState = AT_REST
            if (mListener != null) {
                mListener!!.onScrollFinished()
            }
        }
    }

    interface OnScrollListener {
        /**
         * The scroll position has changed.
         * @param l
         * @param t
         * @param oldl
         * @param oldt
         */
        fun onScroll(l: Int, t: Int, oldl: Int, oldt: Int)

        /**
         * The user has attempted to overscroll. In this case it means touch began when the view
         * was scrolled an edge, and then they tried to move beyond it.
         *
         * @param direction -1 for up 1 for down.
         */
        fun onOverscroll(direction: Int)

        /**
         * Scrolling has completed (Note: this isn't reliable yet)
         */
        fun onScrollFinished()

        /**
         * Scrolling has started (May not be reliable until onScrollFinished is better.
         */
        fun onScrollStart()

        /**
         * The scrolling vertical (y) direction has changed. This will occur at the start of a new scroll event as well as during scrolling if the direction changes. This will be called after [.onScroll] and [.onScrollStart].
         * @param direction 1 for down, -1 for up.
         * @param isTouching true if the user currently has a pointer down.
         * @return true if the new direction was accepted, if false it will act as if the direction hasn't changed and will continue to call this method if the direction continues to move in this direction
         */
        fun onScrollDirectionChanged(direction: Int, isTouching: Boolean): Boolean
    }

    fun setOnScrollListener(listener: OnScrollListener?) {
        mListener = listener
    }

    val isFlinging: Boolean
        /**
         * @return true if currently in the middle of a fling animation after lifting a finger from a fling, but before it has stopped scrolling.
         */
        get() = !this.isTouching && mScrollingState != AT_REST

    companion object {
        const val DEBUG: Boolean = false
    }
}
