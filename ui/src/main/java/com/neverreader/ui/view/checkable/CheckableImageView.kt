package com.neverreader.ui.view.checkable

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.view.SoundEffectConstants
import com.neverreader.ui.util.CheckableHelper
import com.neverreader.ui.util.CheckableHelper.SuperSetContentDescription
import com.neverreader.ui.view.themed.ThemedImageView

open class CheckableImageView : ThemedImageView, CheckableHelper.Checkable {
    private val mCheckable: CheckableHelper? = CheckableHelper(
        this,
        SuperSetContentDescription { contentDescription: CharSequence? ->
            super.setContentDescription(
                contentDescription
            )
        })

    constructor(context: Context) : this(context, null) {
        mCheckable!!.initAttributes(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        mCheckable!!.initAttributes(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        mCheckable!!.initAttributes(context, attrs)
    }

    override fun toggle() {
        mCheckable!!.toggle()
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

    override fun performClick(): Boolean {
        toggle()
        val handled = super.performClick()
        if (!handled) {
            // View only makes a sound effect if the onClickListener was
            // called, so we'll need to make one here instead.
            playSoundEffect(SoundEffectConstants.CLICK)
        }
        return handled
    }

    override fun setChecked(checked: Boolean) {
        mCheckable!!.setChecked(checked)
    }

    override fun setCheckable(value: Boolean) {
        mCheckable!!.isCheckable = value
    }

    /**
     * Register a callback to be invoked when the checked state of this button changes.
     *
     * @param listener
     * the callback to call on checked state change
     */
    override fun setOnCheckedChangeListener(listener: CheckableHelper.OnCheckedChangeListener?) {
        mCheckable!!.setOnCheckedChangeListener(listener)
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 2)
        if (isChecked()) {
            mergeDrawableStates(drawableState, CheckableHelper.CHECKED_STATE_SET)
        }
        if (isCheckable()) {
            mergeDrawableStates(drawableState, CheckableHelper.CHECKABLE_STATE_SET)
        }
        return drawableState
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate()
    }

    public override fun onSaveInstanceState(): Parcelable? {
        return mCheckable!!.onSaveInstanceState(super.onSaveInstanceState())
    }

    public override fun onRestoreInstanceState(state: Parcelable?) {
        var state = state
        state = mCheckable!!.onRestoreInstanceState(state)
        super.onRestoreInstanceState(state)
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        if (mCheckable != null) {
            mCheckable.setContentDescriptions(contentDescription, null)
        } else {
            super.setContentDescription(contentDescription)
        }
    }
}
