package com.neverreader.ui.util

import android.view.ViewGroup

object EnabledUtil {
    fun setChildrenEnabled(parent: ViewGroup, enabled: Boolean, deep: Boolean) {
        var i = 0
        val count = parent.childCount
        while (i < count) {
            val child = parent.getChildAt(i)
            child.isEnabled = enabled
            if (deep && child is ViewGroup) {
                setChildrenEnabled(child, enabled, true)
            }
            i++
        }
    }
}
