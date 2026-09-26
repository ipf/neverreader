package com.neverreader.ui.view.edittext

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.NestedColorStateList
import com.neverreader.ui.view.button.ButtonBoxDrawable
import com.neverreader.ui.view.themed.ThemedEditText

/**
 * A NeverReader themed EditText, with a thin grey oval background.
 *
 * https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=417%3A590
 */
class AppEditText : ThemedEditText {
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context) : super(context) {
        init()
    }

    private fun init() {
        setTextAppearance(getContext(), R.style.App_Text_EditText)
        val paddingHor = getResources().getDimension(R.dimen.nr_space_md).toInt()
        val paddingVer = dpToPxInt(getContext(), 14f)
        setPadding(paddingHor, paddingVer, paddingHor, paddingVer)
        setHintTextColor(NestedColorStateList.get(getContext(), R.color.nr_themed_grey_3))
        setBackgroundDrawable(
            ButtonBoxDrawable(
                getContext(),
                R.color.nr_bg,
                R.color.nr_focusable_grey_4,
                 4f
            )
        )
        setGravity(Gravity.TOP)
    }
}
