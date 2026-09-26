package com.neverreader.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import com.facebook.shimmer.ShimmerFrameLayout

/**
 * A NeverReader themed version of Facebook's [ShimmerFrameLayout]
 */
open class ThemedShimmerFrameLayout : ShimmerFrameLayout {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }
}
