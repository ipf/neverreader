package com.neverreader.ui.util

import android.view.View

/**
 * A view that depending on what data is bound to it, can either have content or be empty.
 * Provides an interface for parent views to decide how to handle in their layouts.
 * This is mostly so when chained views don't have content, they are set to GONE so that their margins are also hidden.
 *
 * See [EmptiableViewHelper] for easy implementation.
 */
interface EmptiableView {
    fun setOnEmptyChangedListener(listener: OnEmptyChangedListener?)
    fun interface OnEmptyChangedListener {
        fun onEmptyChanged(view: View?, isEmpty: Boolean)
    }

    companion object {
        val GONE_WHEN_EMPTY: OnEmptyChangedListener =
            OnEmptyChangedListener { view: View?, isEmpty: Boolean -> view!!.setVisibility(if (isEmpty) View.GONE else View.VISIBLE) }
    }
}
