package com.neverreader.ui.view.button

import android.content.Context
import android.util.AttributeSet

class BoxButton : BoxButtonBase {
    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        init()
    }
}
