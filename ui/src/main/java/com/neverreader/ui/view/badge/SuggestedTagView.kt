package com.neverreader.ui.view.badge

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.neverreader.ui.R
import com.neverreader.ui.view.button.ButtonBoxDrawable

/**
 * A badge that shows a tag name, for use in "suggested tags". Use normal text view setText methods to set the tag.
 *
 * https://www.figma.com/file/Qqwh8xKl4Gy4YMv6mzw2gCO9/CLEAN?node-id=93%3A633
 */
class SuggestedTagView : TextBadgeView {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    constructor(context: Context?) : super(context!!)

    override fun init() {
        super.init()
        // using setBackground directly instead of TextBadgeView.setBadgeColor in order to add the stroke outline with ButtonBoxDrawable
        background = ButtonBoxDrawable(
            context!!, R.color.nr_opaque_touchable_area, R.color.nr_themed_grey_5, 4f)
        setTextColor(ContextCompat.getColorStateList(context, R.color.nr_themed_grey_1))
    }
}
