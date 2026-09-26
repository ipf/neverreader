package com.neverreader.ui.view.checkable

import android.content.Context
import android.util.AttributeSet
import android.view.SoundEffectConstants
import com.neverreader.ui.util.CheckableHelper
import com.neverreader.ui.util.CheckableHelper.SuperSetContentDescription
import com.neverreader.ui.view.visualmargin.VisualMarginConstraintLayout

/**
 */
open class CheckableConstraintLayout : VisualMarginConstraintLayout, CheckableHelper.Checkable {
    private val mCheckable: CheckableHelper? = CheckableHelper(
        this,
        SuperSetContentDescription { contentDescription: CharSequence? ->
            super.setContentDescription(
                contentDescription
            )
        })

    constructor(context: Context?) : super(context) {
        init(null)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        mCheckable!!.initAttributes(getContext(), attrs)
    }

    override fun setChecked(checked: Boolean) {
        mCheckable!!.setChecked(checked)
    }

    override fun setCheckable(value: Boolean) {
        mCheckable!!.isCheckable = value
    }

    override fun isChecked(): Boolean {
        if (mCheckable != null) {
            return mCheckable.isChecked()
        } else {
            return false
        }
    }

    override fun isCheckable(): Boolean {
        if (mCheckable != null) {
            return mCheckable.isCheckable
        } else {
            return false
        }
    }

    override fun toggle() {
        mCheckable!!.toggle()
    }

    override fun performClick(): Boolean {
        toggle() // Important to toggle before invoking the click listener.
        val handled = super.performClick()
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK)
        }
        return handled
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        if (isChecked()) {
            mergeDrawableStates(drawableState, CheckableHelper.CHECKED_STATE_SET)
        }
        if (isCheckable()) {
            mergeDrawableStates(drawableState, CheckableHelper.CHECKABLE_STATE_SET)
        }
        return drawableState
    }

    override fun setOnCheckedChangeListener(listener: CheckableHelper.OnCheckedChangeListener?) {
        mCheckable!!.setOnCheckedChangeListener(listener)
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        if (mCheckable != null) {
            mCheckable.setContentDescriptions(contentDescription, null)
        } else {
            super.setContentDescription(contentDescription)
        }
    }

    fun shouldPropagateChecks(propagateChecks: Boolean) {
        mCheckable!!.shouldPropagateChecks(propagateChecks)
    }
}
