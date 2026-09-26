package com.neverreader.ui.view.edittext

import android.view.View
import android.widget.EditText
import android.widget.TextView

/**
 * An interface used to denote the essential components of a text finder or "find text in page" type view,
 * common on WebView / browser implementations.
 */
interface TextFinderLayout {
    fun root(): View?
    fun cancel(): View?
    fun input(): EditText?
    fun count(): TextView?
    fun back(): View?
    fun forward(): View?
}
