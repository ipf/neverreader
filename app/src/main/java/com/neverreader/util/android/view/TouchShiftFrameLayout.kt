package com.neverreader.util.android.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.neverreader.util.android.MotionUtil.motionEventToShortString
import com.neverreader.util.java.Logs

/**
 * If this FrameLayout's top position is moved while the user is touching down or moving, this will offset the touch events
 * until a ACTION_UP or a ACTION_CANCEL occurs. This is to avoid a jump in scrolling when doing a relayout in the middle of a
 * scroll.
 *
 * @author max
 */
class TouchShiftFrameLayout : FrameLayout {
    private var mIsTracking = false
    private var mTopAtDown = 0

    constructor(
        context: Context, attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        val topChange = getTop() - mTopAtDown
        if (mIsTracking && topChange != 0) {
            if (ScrollTracker.Companion.DEBUG) Logs.i(
                "Scroll",
                "OFFSET " + motionEventToShortString(ev, true) + " BY " + topChange
            )
            ev.offsetLocation(0f, topChange.toFloat())
            if (ScrollTracker.Companion.DEBUG) Logs.i(
                "Scroll",
                "OFFSET TO" + motionEventToShortString(ev, true)
            )
        }

        val action = ev.getAction()
        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> mIsTracking = false
            MotionEvent.ACTION_DOWN -> {
                mTopAtDown = getTop()
                mIsTracking = true
            }
        }

        return super.onInterceptTouchEvent(ev)
    }
}
