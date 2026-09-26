package com.neverreader.ui.view.button

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import com.neverreader.ui.R
import com.neverreader.ui.text.Fonts
import com.neverreader.ui.text.Fonts.get
import com.neverreader.ui.text.TextViewUtil.setVisualTextPadding
import com.neverreader.ui.util.updateEnabledAlpha
import com.neverreader.ui.view.checkable.CheckableTextView

open class BoxButtonBase : CheckableTextView {
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

    internal open fun init() {
        isClickable = true
        gravity = Gravity.CENTER
        setTypeface(get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM))
        setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            getResources().getDimensionPixelSize(R.dimen.nr_medium_text).toFloat()
        )
        setBackgroundDrawable(null)
        setVisualTextPadding(this, R.dimen.nr_space_sm, R.dimen.nr_space_md)
        /** TODO ripple? */
    }

    override fun visualAscent(): Int {
        return 0
    }

    override fun visualDescent(): Int {
        return 0
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.updateEnabledAlpha()
    }
}
