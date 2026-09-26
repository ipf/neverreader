package com.neverreader.ui.view.badge

import android.content.Context
import android.util.AttributeSet
import com.neverreader.ui.R
import com.neverreader.ui.util.NestedColorStateList.get

/**
 * A badge that shows a tag name. Use normal text view setText methods to set the tag.
 */
class TagBadgeView : TextBadgeView {
    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    constructor(context: Context?) : super(context!!)

    override fun init() {
        super.init()
        setBadgeColor(get(context, R.color.nr_badge_tag)!!!!)
        setTextColor(get(context, R.color.nr_badge_tag_text)!!!!)
        uiEntityIdentifier = "badge_tag"
    }
}
