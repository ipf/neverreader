package com.neverreader.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout

open class ThemedTextInputLayout : TextInputLayout {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
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
