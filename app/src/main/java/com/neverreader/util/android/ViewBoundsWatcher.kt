package com.neverreader.util.android

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.View.OnLayoutChangeListener
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.ViewTreeObserver.OnScrollChangedListener

/**
 * Helper class for tracking when a view when a view's absolute bounds (position and/or size) on screen has changed.
 * **Note** this does not currently track animation/transformation changes, only hard layout changes. Perhaps support could be added.
 */
class ViewBoundsWatcher private constructor(
    private val mView: View,
    private var mViewTreeObserver: ViewTreeObserver?,
    private val mListener: OnViewAbsoluteBoundsChangedListener,
    private val mIsScrollable: Boolean
) {
    private val mBounds = Rect()
    private val mCallbacks: Callbacks = Callbacks()
    private val mRecycleXY = IntArray(2)

    private var mIsEnabled = true
    private var mIsVisible = false

    /**
     * @see .create
     */
    init {
        mView.addOnAttachStateChangeListener(mCallbacks)
        mView.addOnLayoutChangeListener(mCallbacks)

        invalidateGlobalListeners(mView.isAttachedToWindow())
        invalidateBounds()
    }

    /**
     * Stop listening. This cannot be undone. If you need it again, create a new instance.
     */
    fun stop() {
        mIsEnabled = false
        releaseGlobalListeners()
    }

    /**
     * Check if the bounds or visibility changed.
     */
    private fun invalidateVisibilityAndBounds() {
        setVisibility(ViewUtil.isVisibleToUserCompat(mView, 0f))
        invalidateBounds()
    }

    /**
     * Check if the bounds changed.
     */
    private fun invalidateBounds() {
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int

        if (mIsVisible) {
            mView.getLocationOnScreen(mRecycleXY)
            left = mRecycleXY[0]
            top = mRecycleXY[1]
            right = mRecycleXY[0] + mView.getMeasuredWidth()
            bottom = mRecycleXY[1] + mView.getMeasuredHeight()
        } else {
            left = 0
            top = 0
            right = 0
            bottom = 0
        }

        if (mBounds.left != left || mBounds.top != top || mBounds.right != right || mBounds.bottom != bottom) {
            mBounds.set(left, top, right, bottom)
            if (mIsEnabled) {
                mListener.onViewAbsoluteBoundsChanged(left, top, right, bottom)
            }
        }
    }

    /**
     * Set the visibility state to a specific value. This does not trigger any listeners.
     *
     * @param isVisible The new state
     */
    private fun setVisibility(isVisible: Boolean) {
        if (isVisible == mIsVisible) {
            return  // No change
        }
        mIsVisible = isVisible
    }

    /**
     * Global listeners are only registered while the view is attached to a window as an optimization. This
     * method will check if the global listeners should be registered or not and updates as needed.
     *
     * @param isAttachedToWindow Whether or not this view is attached to a window.
     */
    private fun invalidateGlobalListeners(isAttachedToWindow: Boolean) {
        if (!mIsEnabled) {
            releaseGlobalListeners()
            return
        }

        if (isAttachedToWindow) {
            if (mViewTreeObserver == null) {
                mViewTreeObserver = mView.getViewTreeObserver()
            }
            if (!mViewTreeObserver!!.isAlive()) {
                return  // Can't register listeners. REVIEW retry later?
            }
            if (mIsScrollable) {
                mViewTreeObserver!!.addOnScrollChangedListener(mCallbacks)
            }
            mViewTreeObserver!!.addOnGlobalLayoutListener(mCallbacks)
        } else if (mViewTreeObserver != null) {
            releaseGlobalListeners()
        } else {
            // Already in the correct state.
        }
    }

    /**
     * Release the global listeners.
     */
    @SuppressLint("NewApi")
    private fun releaseGlobalListeners() {
        if (mViewTreeObserver != null && mViewTreeObserver!!.isAlive()) {
            if (mIsScrollable) {
                mViewTreeObserver!!.removeOnScrollChangedListener(mCallbacks)
            }
            mViewTreeObserver!!.removeOnGlobalLayoutListener(mCallbacks)
        }
        mViewTreeObserver = null
    }

    interface OnViewAbsoluteBoundsChangedListener {
        /**
         * The view's bounds or position have changed. Values may be 0 if the view is invisible.
         *
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        fun onViewAbsoluteBoundsChanged(left: Int, top: Int, right: Int, bottom: Int)
    }

    private inner class Callbacks : OnScrollChangedListener, OnAttachStateChangeListener,
        OnLayoutChangeListener, OnGlobalLayoutListener {
        override fun onScrollChanged() {
            invalidateVisibilityAndBounds()
        }

        override fun onGlobalLayout() {
            invalidateVisibilityAndBounds()
        }

        override fun onViewAttachedToWindow(v: View) {
            invalidateGlobalListeners(true)
            invalidateVisibilityAndBounds()
        }

        override fun onViewDetachedFromWindow(v: View) {
            invalidateGlobalListeners(false)
            setVisibility(false) // View's internal implementation invokes this listener before clearing its mAttachInfo field, so calls to View.isAttachedToWindow() will actually return true right now. So just force the visibility to false otherwise, it will get the wrong value if it checks.
            invalidateBounds()
        }

        override fun onLayoutChange(
            v: View?,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            invalidateVisibilityAndBounds()
        }
    }

    companion object {
        /**
         * @param view The view to listen to
         * @param listener The callback when when the view bounds change
         * @param isScrollable If you know the view is within a scrollable area, pass true. Otherwise false will allow it to optimize for a non-scrolling case.
         * @return The new tracker or null if the view's [View.getViewTreeObserver] returned a null or non-alive observer and we aren't able to track.
         */
        fun create(
            view: View,
            listener: OnViewAbsoluteBoundsChangedListener,
            isScrollable: Boolean
        ): ViewBoundsWatcher? {
            val observer = view.getViewTreeObserver()
            if (observer == null || !observer.isAlive()) {
                return null
            } else {
                return ViewBoundsWatcher(view, observer, listener, isScrollable)
            }
        }
    }
}
