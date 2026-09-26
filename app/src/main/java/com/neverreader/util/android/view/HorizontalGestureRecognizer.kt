package com.neverreader.util.android.view

import android.content.Context
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.neverreader.util.android.FormFactor.dpToPx
import com.neverreader.util.java.Logs
import kotlin.math.abs
import kotlin.math.atan2

class HorizontalGestureRecognizer(viewContext: Context) {
    private var mIsMovingHorizontal = false
    private val mTouchSlop: Int
    private var mAbsorbTouches = false
    private var mStartX = 0f
    private var mStartY = 0f
    private var mDetectSwipe = false
    private var mDetectDirection = false
    var isPaging: Boolean = false
        private set
    private var mListener: SwipeListener? = null
    private var mHasMoved = false
    private val mSwipeLengthThresholdInHorizontal: Int
    private val mSwipeLengthThresholdInVertical: Int

    init {
        mTouchSlop = ViewConfiguration.get(viewContext).getScaledTouchSlop()
        mSwipeLengthThresholdInHorizontal = dpToPx(15f)
        mSwipeLengthThresholdInVertical = dpToPx(70f)
    }

    fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null || mListener == null || !mListener!!.isPagingEnabled) {
            return false
        }

        var isFirstAbsorb = false

        when (ev.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                if (DEBUG) Logs.d("Paging", "DOWN")
                mIsMovingHorizontal = false
                mAbsorbTouches = false
                if (mListener!!.onHorizontalGestureDown(ev.getX(), ev.getY())) {
                    mDetectDirection = true
                    mDetectSwipe = true
                    mStartX = ev.getX()
                    mStartY = ev.getY()
                    mHasMoved = false
                } else {
                    return false // Ignore swipes starting here.
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (!mHasMoved) {
                    if (DEBUG) Logs.l("Swipe " + "MOVE")
                }
                mHasMoved = true

                if (DEBUG) {
                    if (ev.getAction() == MotionEvent.ACTION_UP) {
                        Logs.d("Paging", "UP")
                    }
                }
                val distanceX = ev.getX() - mStartX
                if (abs(distanceX) > mTouchSlop) {
                    if (mDetectSwipe && mIsMovingHorizontal) {
                        val swipeLength =
                            (if (this.isPaging) mSwipeLengthThresholdInHorizontal else mSwipeLengthThresholdInVertical).toFloat()
                        if (DEBUG) Logs.d("Paging", "swiping " + abs(distanceX) + " " + swipeLength)
                        if (abs(distanceX) >= swipeLength) {
                            mListener!!.swiped(distanceX < 0)
                            mDetectSwipe = false
                        }
                    }

                    if (mDetectDirection) {
                        // Determine Angle
                        val angleInDegrees = abs(
                            atan2(
                                (ev.getY() - mStartY).toDouble(),
                                distanceX.toDouble()
                            ) * 180 / Math.PI
                        ).toFloat()


                        // Determine the sensitivity. If in horizontal mode, they should do something VERY vertical in order to get out of it
                        // The size of the threshold looks like this: >< basically the amount they can be move vertically before we decide it is not horizontal
                        // The angle goes both ways on the y axis. So if the value is 10, then it's 10 up or 10 down
                        val sensitivity = if (this.isPaging) 73 else 17

                        if (angleInDegrees < sensitivity || angleInDegrees > 180 - sensitivity) {
                            // Horizontal
                            mIsMovingHorizontal = true
                            isFirstAbsorb = true
                            mAbsorbTouches = true
                        } else {
                            // Vertical
                        }

                        if (DEBUG) Logs.d(
                            "Paging",
                            "direction " + angleInDegrees + " " + sensitivity + " " + mAbsorbTouches
                        )

                        mDetectDirection = false
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                if (DEBUG) {
                    if (ev.getAction() == MotionEvent.ACTION_UP) {
                        Logs.d("Paging", "UP")
                    }
                }
                val distanceX = ev.getX() - mStartX
                if (abs(distanceX) > mTouchSlop) {
                    if (mDetectSwipe && mIsMovingHorizontal) {
                        val swipeLength =
                            (if (this.isPaging) mSwipeLengthThresholdInHorizontal else mSwipeLengthThresholdInVertical).toFloat()
                        if (DEBUG) Logs.d("Paging", "swiping " + abs(distanceX) + " " + swipeLength)
                        if (abs(distanceX) >= swipeLength) {
                            mListener!!.swiped(distanceX < 0)
                            mDetectSwipe = false
                        }
                    }

                    if (mDetectDirection) {
                        val angleInDegrees = abs(
                            atan2(
                                (ev.getY() - mStartY).toDouble(),
                                distanceX.toDouble()
                            ) * 180 / Math.PI
                        ).toFloat()

                        val sensitivity = if (this.isPaging) 73 else 17

                        if (angleInDegrees < sensitivity || angleInDegrees > 180 - sensitivity) {
                            mIsMovingHorizontal = true
                            isFirstAbsorb = true
                            mAbsorbTouches = true
                        } else {
                        }

                        if (DEBUG) Logs.d(
                            "Paging",
                            "direction " + angleInDegrees + " " + sensitivity + " " + mAbsorbTouches
                        )

                        mDetectDirection = false
                    }
                }
            }
        }

        if (isFirstAbsorb) {
            // Let this one pass through but set it as a cancel
            ev.setAction(MotionEvent.ACTION_CANCEL)
            if (DEBUG) Logs.d("Paging", "touches absorbed")
            return false
        }
        return mAbsorbTouches
    }


    /**
     * When set in horizontal mode, swipes are much easier to perform (more sensitive).
     *
     * @param isHorizontalMode
     */
    fun setHorizontalMode(isHorizontalMode: Boolean) {
        this.isPaging = isHorizontalMode
    }

    interface SwipeListener {
        /**
         * A swipe has been performed.
         * @param left true if touches moved from right to left, false if left to right.
         */
        fun swiped(left: Boolean)

        /**
         * @return true if paging is allowed at this time.
         */
        val isPagingEnabled: Boolean

        /**
         * @param x The touches down x
         * @param y The touches down y
         * @return true if swipes are allowed starting at this location, false if not.
         */
        fun onHorizontalGestureDown(x: Float, y: Float): Boolean
    }

    fun setListener(listener: SwipeListener?) {
        mListener = listener
    }

    companion object {
        private const val DEBUG = false
    }
}
