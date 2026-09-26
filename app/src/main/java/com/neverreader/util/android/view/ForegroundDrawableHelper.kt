package com.neverreader.util.android.view

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.MotionEvent
import android.view.View
import com.neverreader.util.android.drawable.DrawableUtil

/**
 * Helper for adding a Foreground drawable to a view.
 *
 *
 * To use in your custom view:
 *
 *  * Create a final instance of this during construction.
 *  * Set your foreground drawable with [.setForegroundDrawable]. The drawable is ok to change or become null at anytime.
 *  * Invoke all of the onParent... methods of this class from the matching View methods in your custom class. For example, override jumpDrawablesToCurrentState() in your View, and invoke the onParentJumpDrawablesToCurrentState() method in this class at the end of the method override.
 *  * It is safe to invoke all onParent methods even while a foreground drawable is null and not set.
 *
 */
class ForegroundDrawableHelper(private val mParent: View) {
    private var mForegroundDrawable: Drawable? = null

    fun setForegroundDrawable(drawable: Drawable?) {
        if (mForegroundDrawable != null) {
            mForegroundDrawable!!.setCallback(null)
        }
        mForegroundDrawable = drawable
        if (drawable != null) {
            drawable.setCallback(mParent)
        }
        mParent.invalidate()
    }

    fun onParentSizeChanged(
        w: Int,
        h: Int,
        @Suppress("unused") oldw: Int,
        @Suppress("unused") oldh: Int
    ) {
        if (mForegroundDrawable != null) {
            mForegroundDrawable!!.setBounds(0, 0, w, h)
        }
    }

    fun onParentDrawableStateChanged() {
        if (mForegroundDrawable != null && mForegroundDrawable!!.isStateful()) {
            mForegroundDrawable!!.setState(mParent.getDrawableState())
        }
    }

    fun onParentTouchEvent(event: MotionEvent) {
        DrawableUtil.setHotspot(mForegroundDrawable, event)
    }

    fun onParentJumpDrawablesToCurrentState() {
        if (mForegroundDrawable != null) {
            mForegroundDrawable!!.jumpToCurrentState()
        }
    }

    fun onParentVerifyDrawable(who: Drawable?): Boolean {
        return who === mForegroundDrawable
    }


    fun onParentDispatchDraw(canvas: Canvas) {
        if (mForegroundDrawable != null) {
            mForegroundDrawable!!.draw(canvas)
        }
    }

    interface Setter {
        fun setForegroundDrawable(drawable: Drawable?)
    }
}
