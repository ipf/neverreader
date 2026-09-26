package com.neverreader.ui.view.badge

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.databinding.ViewBadgeBinding
import com.neverreader.ui.view.themed.ThemedLinearLayout

class BadgeView(
    context: Context,
    attrs: AttributeSet? = null,
) : ThemedLinearLayout(
    context,
    attrs,
    entityType = UiEntityable.Type.BUTTON
) {

    private val binding: ViewBadgeBinding = ViewBadgeBinding.inflate(
        LayoutInflater.from(context),
        this,
    ).also {
        // setting params here because our root layout in xml is a <merge> tag
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    enum class Type {
        TAG,
        EMPHASIZED_TAG
    }
}
