package com.neverreader.ui.util

import android.view.View
import android.view.ViewGroup
import com.neverreader.ui.util.EmptiableView.OnEmptyChangedListener

/**
 * Helper for views that are [EmptiableView].
 * Create an instance, pass on calls of [.setOnEmptyChangedListener] to here,
 * and invoke [.setEmpty] as needed.
 */
class EmptiableViewHelper(private val view: View?, private var listener: OnEmptyChangedListener?) :
    EmptiableView {
    private var empty = false

    fun setEmpty(empty: Boolean) {
        if (this.empty != empty) {
            this.empty = empty
            if (listener != null) {
                listener!!.onEmptyChanged(view, empty)
            }
        }
    }

    override fun setOnEmptyChangedListener(listener: OnEmptyChangedListener?) {
        this.listener = listener
    }

    companion object {
        fun hasVisibleChildren(view: ViewGroup): Boolean {
            var i = 0
            val count = view.childCount
            while (i < count) {
                if (view.getChildAt(i).getVisibility() == View.VISIBLE) {
                    return true
                }
                i++
            }
            return false
        }
    }
}
