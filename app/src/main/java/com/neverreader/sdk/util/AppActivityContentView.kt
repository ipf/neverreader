package com.neverreader.sdk.util

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout

/**
 * The content view of [NeverReaderActivityRootView].
 * Even if this implementation stays empty, its main function is to
 * serve as a strong type for what ViewGroup the content view is.
 */
class AppActivityContentView : FrameLayout {

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)
}
