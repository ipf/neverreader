package com.neverreader.util.android.view

import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Wrapper for accessing common methods like findFirstVisibleItemPosition from a variety of layout managers.
 */
object LayoutManagerUtil {
    fun findFirstVisibleItemPosition(view: RecyclerView): Int {
        return findFirstVisibleItemPosition(view.getLayoutManager())
    }

    fun findFirstVisibleItemPosition(layout: RecyclerView.LayoutManager?): Int {
        if (layout == null) {
            return 0
        }
        if (layout is GridLayoutManager) {
            return layout.findFirstVisibleItemPosition()
        } else if (layout is LinearLayoutManager) {
            return layout.findFirstVisibleItemPosition()
        } else {
            throw RuntimeException("unknown layout type " + layout)
        }
    }

    fun findLastVisibleItemPosition(view: RecyclerView): Int {
        return findLastVisibleItemPosition(view.getLayoutManager())
    }

    fun findLastVisibleItemPosition(layout: RecyclerView.LayoutManager?): Int {
        if (layout is GridLayoutManager) {
            return layout.findLastVisibleItemPosition()
        } else if (layout is LinearLayoutManager) {
            return layout.findLastVisibleItemPosition()
        } else {
            throw RuntimeException("unknown layout type " + layout)
        }
    }
}
