package com.neverreader.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import com.neverreader.ui.R
import com.neverreader.ui.view.button.IconButton

/**
 * TODO need to confirm what style design wants here
 */
class RadioButton : IconButton {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    private fun init() {
        scaleType = ScaleType.CENTER
        setDrawableColor(R.color.nr_themed_teal_2)
        setImageResource(R.drawable.btn_radio_mtrl)
    }
}
