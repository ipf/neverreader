package com.neverreader.ui.view.bottom

import android.view.MotionEvent
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior

class AppBottomSheetBehavior<V : View> : BottomSheetBehavior<V>() {
    interface TouchCondition {
        fun canTouch(): Boolean
    }

    private var touchCondition: TouchCondition? = null

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent
    ): Boolean {
        if (touchCondition == null || touchCondition!!.canTouch()) {
            return super.onInterceptTouchEvent(parent, child, event)
        } else {
            super.onInterceptTouchEvent(parent, child, event)
            return false
        }
    }

}
