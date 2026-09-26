package com.neverreader.ui.view.button

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.DimenRes
import androidx.appcompat.widget.TooltipCompat
import com.neverreader.ui.R
import com.neverreader.ui.view.checkable.CheckableImageView
import org.apache.commons.lang3.ArrayUtils

open class IconButton : CheckableImageView {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context,
        attrs,
        R.attr.iconButtonStyle
    ) {
        init(attrs)
    }

    constructor(context: Context?) : super(context!!) {
        init(null)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(attrs, R.styleable.IconButton)
            val checkedOverride = a.getColorStateList(R.styleable.IconButton_checkedDrawableColor)
            if (checkedOverride != null) {
                setDrawableColorOverride(ColorOverride { state: IntArray?, color: Int ->
                    if (isEnabled() && ArrayUtils.contains(state, android.R.attr.state_checked)) {
                        // Only override checked color when view is enabled
                        checkedOverride!!.getColorForState(
                            state,
                            color
                        )
                    } else {
                        color
                    }
                })
            }
            a.recycle()
        }
    }

    public override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        TooltipCompat.setTooltipText(this, contentDescription)
    }

    /**
     * This is intended for icons which are aligned to the left/start of the screen.
     *
     * Icon buttons are 50dp, but their icons are a variable width, creating a visual margin/padding that depends on the size of the icon.
     * This sets the margin to account for the icon size in order to visually align an icon to the left/start with the app's standard side margin,
     * nr_side_grid.
     */
    fun setSideMarginStart() {
        setIconSideMargin(R.dimen.nr_side_grid, true)
    }

    /**
     * This is intended for icons which are aligned to the right/end of the screen.
     *
     * Icon buttons are 50dp, but their icons are a variable width, creating a visual margin/padding that depends on the size of the icon.
     * This sets the margin to account for the icon size in order to visually align an icon to the right/end with the app's standard side margin,
     * nr_side_grid.
     */
    fun setSideMarginEnd() {
        setIconSideMargin(R.dimen.nr_side_grid, false)
    }

    fun setVisualMarginStart(@DimenRes margin: Int) {
        setIconSideMargin(margin, true)
    }

    fun setVisualMarginEnd(@DimenRes margin: Int) {
        setIconSideMargin(margin, false)
    }

    private fun setIconSideMargin(@DimenRes margin: Int, start: Boolean) {
        val context = getContext()
        val iconWidth = context.getResources()
            .getDimensionPixelSize(R.dimen.nr_icon_button_width) // full width of the icon (eg 50dp)
        val drawableWidth =
            getDrawable().getIntrinsicWidth() // width of the image inside it (variable width)
        val visualMargin = (iconWidth - drawableWidth) / 2 // intrinsic visual margin
        val params = getLayoutParams() as MarginLayoutParams
        if (start) {                                                                                // set margin to nr_side_grid minus what's already visually there
            params.leftMargin = context.getResources().getDimensionPixelSize(margin) - visualMargin
        } else {
            params.rightMargin = context.getResources().getDimensionPixelSize(margin) - visualMargin
        }
        setLayoutParams(params)
    }
}
