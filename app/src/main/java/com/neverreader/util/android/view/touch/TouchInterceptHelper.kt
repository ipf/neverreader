package com.neverreader.util.android.view.touch

import android.content.Context
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

/**
 * Detects when a touch event has gone beyond the touch slop and is no longer a possible tap.
 */
class TouchInterceptHelper(context: Context) {
    private val mTouchSlop: Int
    private val mDown = PointF()
    val record: MutableList<MotionEvent> = ArrayList<MotionEvent>()

    private var mIsRecordingEnabled = false

    init {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop()
    }

    fun setRecordingEnabled(record: Boolean) {
        mIsRecordingEnabled = record
    }

    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                clearRecord()
                mDown.set(ev.getX(), ev.getY())
            }

            MotionEvent.ACTION_MOVE -> {
                val xMove = abs(mDown.x - ev.getX())
                val yMove = abs(mDown.y - ev.getY())
                if (xMove > mTouchSlop || yMove > mTouchSlop) {
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {}
        }

        if (mIsRecordingEnabled) {
            record.add(MotionEvent.obtain(ev))
        }

        return false
    }

    private fun clearRecord() {
        for (ev in this.record) {
            ev.recycle()
        }
        record.clear()
    }
}
