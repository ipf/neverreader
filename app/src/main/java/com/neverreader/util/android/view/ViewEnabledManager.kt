package com.neverreader.util.android.view

import android.view.View

/**
 * Controls whether or not a view is enabled by checking a list of registered [ViewEnabledCondition]'s.
 * To be enabled, all registered enablers must return true.
 *
 *
 * Anytime an enablers status changes, it should invoke [.invalidate] to update the view's state..
 */
class ViewEnabledManager(private val mView: View) {
    private val mEnablers = ArrayList<ViewEnabledCondition>()

    fun addCondition(enabler: ViewEnabledCondition?) {
        mEnablers.add(enabler!!)
        invalidate()
    }

    fun invalidate() {
        mView.setEnabled(this.isEnabled)
    }

    private val isEnabled: Boolean
        get() {
            for (enabler in mEnablers) {
                if (!enabler.isEnabled) {
                    return false
                }
            }
            return true
        }

    interface ViewEnabledCondition {
        val isEnabled: Boolean
    }
}
