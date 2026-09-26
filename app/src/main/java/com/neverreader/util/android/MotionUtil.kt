package com.neverreader.util.android

import android.view.MotionEvent

/**
 * TODO Documentation
 */
object MotionUtil {
    fun motionEventToShortString(ev: MotionEvent?, round: Boolean): String {
        if (ev == null) {
            return "{null}"
        }

        var s = "{"
        if (round) {
            s += (ev.getX().toInt()).toString() + "," + (ev.getY().toInt())
        } else {
            s += ev.getX().toString() + "," + ev.getY()
        }
        return s + ", " + motionEventActionToString(ev.getAction()) + "}"
    }

    /**
     * Copied from hidden method MotionEvent#actionToString(int)
     *
     * @param action
     * @return
     */
    fun motionEventActionToString(action: Int): String {
        when (action) {
            MotionEvent.ACTION_DOWN -> return "ACTION_DOWN"
            MotionEvent.ACTION_UP -> return "ACTION_UP"
            MotionEvent.ACTION_CANCEL -> return "ACTION_CANCEL"
            MotionEvent.ACTION_OUTSIDE -> return "ACTION_OUTSIDE"
            MotionEvent.ACTION_MOVE -> return "ACTION_MOVE"
            MotionEvent.ACTION_HOVER_MOVE -> return "ACTION_HOVER_MOVE"
            MotionEvent.ACTION_SCROLL -> return "ACTION_SCROLL"
            MotionEvent.ACTION_HOVER_ENTER -> return "ACTION_HOVER_ENTER"
            MotionEvent.ACTION_HOVER_EXIT -> return "ACTION_HOVER_EXIT"
        }
        val index =
            (action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_POINTER_DOWN -> return "ACTION_POINTER_DOWN(" + index + ")"
            MotionEvent.ACTION_POINTER_UP -> return "ACTION_POINTER_UP(" + index + ")"
            else -> return action.toString()
        }
    }
}
