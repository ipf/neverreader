package com.neverreader.util.android.view

import android.view.View
import android.view.ViewTreeObserver
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.ViewTreeObserver.OnScrollChangedListener

/**
 * TODO Documentation
 */
class ViewTreeObserverHelper(view: View, listener: Listener) {
    private val mObserver: ViewTreeObserver
    private val mLayoutListener: OnGlobalLayoutListener
    private val mScrollListener: OnScrollChangedListener
    private var mIsEnabled = true

    init {
        mLayoutListener = object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (mIsEnabled) {
                    listener.onGlobalLayout()
                }
            }
        }
        mScrollListener = object : OnScrollChangedListener {
            override fun onScrollChanged() {
                if (mIsEnabled) {
                    listener.onScrollChanged()
                }
            }
        }
        mObserver = view.getViewTreeObserver()
        mObserver.addOnGlobalLayoutListener(mLayoutListener)
        mObserver.addOnScrollChangedListener(mScrollListener)
    }

    fun stop() {
        mIsEnabled = false
        if (mObserver.isAlive()) {
            mObserver.removeOnGlobalLayoutListener(mLayoutListener)
            mObserver.removeOnScrollChangedListener(mScrollListener)
        }
    }

    interface Listener {
        fun onGlobalLayout()
        fun onScrollChanged()
    }
}
