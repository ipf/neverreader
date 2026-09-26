package com.neverreader.ui.util

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.annotation.CheckResult
import androidx.appcompat.widget.TooltipCompat
import com.neverreader.ui.R

/**
 * Helper for adding Checkableness to your view. A bare minimum implementation in your view subclass would look like this:
 *
 * <pre>
 *
 *   private final CheckableHelper mCheckable = new CheckableHelper(this);

private void init(AttributeSet attrs) { // Invoke this from your constructors
mCheckable.initAttributes(getContext(), attrs);
}

@Override
public void setChecked(boolean checked) {
mCheckable.setChecked(checked);
}

@Override
public void setCheckable(boolean value) {
mCheckable.setCheckable(value);
}

@Override
public boolean isChecked() {
if (mCheckable != null) {
return mCheckable.isChecked();
} else {
return false;
}
}

@Override
public boolean isCheckable() {
if (mCheckable != null) {
return mCheckable.isCheckable();
} else {
return false;
}
}

@Override
public void toggle() {
mCheckable.toggle();
}

@Override
public boolean performClick() {
toggle(); // Important to toggle before invoking the click listener.
boolean handled = super.performClick();
if (!handled) {
// View only makes a sound effect if the onClickListener was
// called, so we'll need to make one here instead.
playSoundEffect(SoundEffectConstants.CLICK);
}
return handled;
}

@Override
public int[] onCreateDrawableState(int extraSpace) {
final int[] drawableState = super.onCreateDrawableState(extraSpace + 2);
if (isChecked()) {
mergeDrawableStates(drawableState, CheckableHelper.CHECKED_STATE_SET);
}
if (isCheckable()) {
mergeDrawableStates(drawableState, CheckableHelper.CHECKABLE_STATE_SET);
}
return drawableState;
}

@Override
public void setOnCheckedChangeListener(CheckableHelper.OnCheckedChangeListener listener) {
mCheckable.setOnCheckedChangeListener(listener);
}
 *
 * </pre>
 *
 * To support the `checkedContentDescription` xml attr that lets you specify content descriptions that change
 * when in a checked state, change your constructor to look like this and add this additional override:
 *
 * <pre>
private final CheckableHelper mCheckable = new CheckableHelper(this, super::setContentDescription);

@Override
public void setContentDescription(CharSequence contentDescription) {
if (mCheckable != null) {
mCheckable.setContentDescriptions(contentDescription, null);
} else {
super.setContentDescription(contentDescription);
}
}

 * </pre>
 */
class CheckableHelper
/**
 * Use if your checked view wants to support the checkedContentDescription xml attr.
 * See main java doc on this class for details.
 */ @JvmOverloads constructor(
    private val mView: View,
    private val mContentDescriptionSetter: SuperSetContentDescription? = null
) : Checkable {
    private var mIsChecked = false
    private var mIsBroadcasting = false
    private var mIsCheckable = false
    private var mListener: OnCheckedChangeListener? = null
    private var mUncheckedContentDescription: CharSequence? = null
    private var mCheckedContentDescription: CharSequence? = null
    private var propagateChecks = true

    fun initAttributes(context: Context?, attrs: AttributeSet?) {
        if (attrs != null) {
            var a = context!!.obtainStyledAttributes(attrs, R.styleable.CheckableHelper)
            this.isCheckable =
                a.getBoolean(R.styleable.CheckableHelper_isCheckable, false)
            setContentDescriptions(
                mView.getContentDescription(),
                a.getText(R.styleable.CheckableHelper_checkedContentDescription)
            )
            a.recycle()

            // TODO need to document and find a sane default for how this handles clickability. These views absorb touch events unless you explictly set clickable and longClickable to false and lead to confusing issues.
            // Need to set checkable views to clickable for them to toggle by default. But want to avoid overridding clickable:false if it was set in the xml.
            val set = intArrayOf(
                android.R.attr.clickable
            )
            a = context.obtainStyledAttributes(attrs, set)
            mView.setClickable(a.getBoolean(0, true))
            a.recycle()
        } else {
            mView.setClickable(true)
        }
    }

    override fun setChecked(checked: Boolean) {
        if (mIsChecked != checked) {
            mIsChecked = checked

            // Avoid infinite recursions if setChecked() is called from a listener
            if (!mIsBroadcasting) {
                mIsBroadcasting = true
                if (mListener != null) {
                    mListener!!.onCheckedChanged(mView, checked)
                }
                mIsBroadcasting = false
            }
            reapplyContentDescription()

            mView.refreshDrawableState()
            mView.invalidate()

            if (propagateChecks) {
                setChildrenChecked(mView, checked)
            }
        }
    }

    override fun isChecked(): Boolean {
        return mIsChecked
    }

    override fun toggle() {
        if (this.isCheckable) {
            setChecked(!mIsChecked)
        }
    }

    var isCheckable: Boolean
        get() = mIsCheckable
        /**
         * Whether or not [.toggle] will have any effect.
         *
         * @param value
         */
        set(value) {
            if (mIsCheckable != value) {
                mIsCheckable = value
                mView.refreshDrawableState()
                mView.invalidate()
            }
        }

    fun shouldPropagateChecks(propagateChecks: Boolean) {
        this.propagateChecks = propagateChecks
    }

    fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?) {
        mListener = listener
    }

    fun setContentDescriptions(regular: CharSequence?, checked: CharSequence?) {
        mUncheckedContentDescription = regular
        mCheckedContentDescription = checked
        reapplyContentDescription()
    }

    private fun reapplyContentDescription() {
        if (mContentDescriptionSetter != null) {
            val contentDescription =
                if (isChecked() && !TextUtils.isEmpty(mCheckedContentDescription)) mCheckedContentDescription else mUncheckedContentDescription
            mContentDescriptionSetter.setSuperContentDescription(contentDescription)
            TooltipCompat.setTooltipText(mView, contentDescription)
        } else {
            // Ignore, this feature is not in use on this helper.
        }
    }

    fun onSaveInstanceState(superState: Parcelable?): Parcelable {
        val ss = SavedState(superState)
        ss.checked = isChecked()
        return ss
    }

    @CheckResult
    fun onRestoreInstanceState(state: Parcelable?): Parcelable? {
        if (state !is SavedState) {
            return state
        }
        val ss = state
        setChecked(ss.checked)
        this.isCheckable = ss.checkable
        return ss.getSuperState()
    }

    interface Checkable : android.widget.Checkable {
        fun setOnCheckedChangeListener(listener: OnCheckedChangeListener?)
        fun setCheckable(value: Boolean)
        fun isCheckable(): Boolean
    }

    fun interface OnCheckedChangeListener {
        fun onCheckedChanged(view: View?, isChecked: Boolean)
    }

    fun interface SuperSetContentDescription {
        /**
         * Invoke your view's `super.setContentDescription()` method with the provided value.
         */
        fun setSuperContentDescription(value: CharSequence?)
    }

    // REVIEW
    private class SavedState : View.BaseSavedState {
        var checked: Boolean = false
        var checkable: Boolean = false

        internal constructor(superState: Parcelable?) : super(superState)

        private constructor(`in`: Parcel) : super(`in`) {
            checked = (`in`.readValue(null) as kotlin.Boolean?)!!
            checkable = (`in`.readValue(null) as kotlin.Boolean?)!!
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeValue(checked)
            out.writeValue(checkable)
        }

        companion object {
            val CREATOR: Parcelable.Creator<SavedState?> =
                object : Parcelable.Creator<SavedState?> {
                    override fun createFromParcel(`in`: Parcel): SavedState {
                        return SavedState(`in`)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls<SavedState>(size)
                    }
                }
        }
    }

    companion object {
        @JvmField
        val CHECKED_STATE_SET: IntArray = intArrayOf(android.R.attr.state_checked)
        @JvmField
        val CHECKABLE_STATE_SET: IntArray = intArrayOf(android.R.attr.state_checkable)

        private fun setChildrenChecked(parent: View?, checked: Boolean) {
            if (parent is ViewGroup) {
                var i = 0
                val count = parent.getChildCount()
                while (i < count) {
                    val child = parent.getChildAt(i)
                    if (child is android.widget.Checkable) {
                        (child as android.widget.Checkable).setChecked(checked)
                    }
                    setChildrenChecked(child, checked)
                    i++
                }
            }
        }
    }
}
