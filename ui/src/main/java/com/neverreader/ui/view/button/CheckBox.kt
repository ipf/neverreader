package com.neverreader.ui.view.button

import android.content.Context
import android.util.AttributeSet
import com.neverreader.ui.R

class CheckBox : IconButton {
    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    ) {
        init()
    }

    private fun init() {
        setImageResource(R.drawable.ic_nr_check)
        setScaleType(ScaleType.CENTER)
        setCheckable(true)
        setDrawableColor(R.color.nr_checkbox)
    }
}
