package com.neverreader.ui.util

import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.ViewGroup

object NeverReaderUIViewUtil {
    /**
     * Removes a view and replaces it with a new view in the same position. Also copies layout params.
     */
    fun replaceView(replace: View, replacement: View) {
        val parent = replace.parent as ViewGroup
        val index = parent.indexOfChild(replace)
        parent.removeView(replace)

        replacement.layoutParams = replace.layoutParams
        parent.addView(replacement, index)
    }

    fun runAfterNextLayoutOf(view: View, block: Runnable) {
        view.addOnLayoutChangeListener(object : OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                block.run()
                view.removeOnLayoutChangeListener(this)
            }
        })
    }
}
