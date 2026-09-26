package com.neverreader.ui.view.badge

import android.content.Context
import com.neverreader.ui.R
import kotlin.math.max

object BadgeUtil {
    fun getBadgeSize(context: Context): Int {
        val res = context.resources
        return max(
            res.getDimensionPixelSize(R.dimen.nr_badge_height_min),
            res.getDimensionPixelSize(R.dimen.nr_badge_height)
        )
    }
}
