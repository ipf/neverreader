package com.neverreader.util.android.view

import android.view.View

/**
 * Controls whether or not a view is visible by checking a list of registered [ViewVisibilityCondition]'s.
 * To be enabled, all registered enablers must return true.
 *
 *
 * Anytime an enablers status changes, it should invoke [.invalidate] to update the view's state..
 */
class ViewVisibleManager
/**
 * @param view
 * @param hiddenVisibility The visibility flag that should be used while not visible. Either [View.GONE] or [View.INVISIBLE].
 */(private val mView: View, private val mHiddenVisibilityFlag: Int) {
    private val mEnablers = ArrayList<ViewVisibilityCondition>()

    fun addCondition(enabler: ViewVisibilityCondition?) {
        mEnablers.add(enabler!!)
        invalidate()
    }

    fun invalidate(): Boolean {
        val currentState = mView.getVisibility() == View.VISIBLE
        val newState = this.isVisible
        if (newState) {
            mView.setVisibility(View.VISIBLE)
        } else {
            mView.setVisibility(mHiddenVisibilityFlag)
        }
        return currentState != newState
    }

    private val isVisible: Boolean
        get() {
            for (enabler in mEnablers) {
                if (!enabler.isVisible) {
                    return false
                }
            }
            return true
        }

    interface ViewVisibilityCondition {
        val isVisible: Boolean
    }
}
