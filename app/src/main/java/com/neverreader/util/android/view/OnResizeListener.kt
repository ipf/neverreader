package com.neverreader.util.android.view

import android.view.View

interface OnResizeListener {
    fun onViewSizeChanged(v: View?, newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int)
}
