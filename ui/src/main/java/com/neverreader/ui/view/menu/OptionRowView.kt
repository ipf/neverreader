package com.neverreader.ui.view.menu

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import com.neverreader.ui.R
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.IntrinsicSizeHelper
import com.neverreader.ui.view.checkable.CheckableConstraintLayout
import com.neverreader.ui.view.themed.ThemedTextView

class OptionRowView : CheckableConstraintLayout {
    private val sizeHelper = IntrinsicSizeHelper(-1, dpToPxInt(getContext(), 54f))
    private var icon: ImageView? = null
    private var label: ThemedTextView? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_option_row_view, this, true)
        icon = findViewById<ImageView>(R.id.icon)
        label = findViewById<ThemedTextView>(R.id.label)
        setBackgroundResource(R.drawable.cl_nr_touchable_area)
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS)
        engageable.uiEntityType = UiEntityable.Type.BUTTON
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec)
        heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setLabel(stringResId: Int) {
        label!!.setTextAndUpdateEnUsLabel(stringResId)
    }

    fun setIcon(value: Drawable?) {
        icon!!.setImageDrawable(value)
        icon!!.setVisibility(if (icon!!.getDrawable() != null) VISIBLE else GONE)
    }

    fun setIcon(drawableResId: Int) {
        icon!!.setImageResource(drawableResId)
        icon!!.setVisibility(if (icon!!.getDrawable() != null) VISIBLE else GONE)
    }
}
