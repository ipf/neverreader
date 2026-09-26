package com.neverreader.ui.view.progress.skeleton.row

import android.content.Context
import android.util.AttributeSet
import com.neverreader.ui.R

class SkeletonItemRow : AbsSkeletonRow {
    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override val layout: Int
        get() = R.layout.view_skeleton_item_row
}
