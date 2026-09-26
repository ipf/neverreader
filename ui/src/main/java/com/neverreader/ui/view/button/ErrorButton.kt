package com.neverreader.ui.view.button

import android.content.Context
import android.util.AttributeSet
import com.neverreader.ui.R

class ErrorButton : BoxButtonBase {
    constructor(context: Context?) : super(context!!) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        setTextColor(getResources().getColorStateList(R.color.nr_button_text))
        setBackgroundDrawable(ButtonBoxDrawable(context!!, R.color.nr_button_box_error_fill, ButtonBoxDrawable.CornerStyle.ALL))
    }
}
