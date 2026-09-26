package com.neverreader.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.IntrinsicSizeHelper
import com.neverreader.ui.view.checkable.CheckableConstraintLayout

class RadioOptionRowView : CheckableConstraintLayout {
    private val sizeHelper = IntrinsicSizeHelper(-1, dpToPxInt(getContext(), 54f))
    private var label: TextView? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    constructor(context: Context?) : super(context!!) {
        init(null)
    }

    private fun init(attrs: AttributeSet?) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_radio_option_row_view, this, true)
        label = findViewById<TextView>(R.id.label)
        setBackgroundResource(R.drawable.cl_nr_touchable_area)
        setDescendantFocusability(FOCUS_BLOCK_DESCENDANTS)
    }

    protected override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = sizeHelper.applyWidth(widthMeasureSpec)
        heightMeasureSpec = sizeHelper.applyHeight(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setLabel(stringResId: Int) {
        label!!.setText(stringResId)
    }

    fun setLabel(value: CharSequence?) {
        label!!.setText(value)
    }
}
