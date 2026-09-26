package com.neverreader.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import com.neverreader.ui.analytics.Engageable
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.analytics.EngageableHelper
import com.neverreader.ui.analytics.EngagementListener

/**
 * A themed version of [RelativeLayout] for Views which have not yet been fully ported over to the ui module.
 *
 *
 * New Views should generally use [ThemedConstraintLayout] rather than RelativeLayout.
 */
open class ThemedRelativeLayout : RelativeLayout, Engageable {
    protected val engageable: EngageableHelper = EngageableHelper()

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    constructor(context: Context?) : super(context) {
        init(null)
    }

    private fun init(attrs: AttributeSet?) {
        engageable.obtainStyledAttributes(getContext(), attrs)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }

    override var uiEntityIdentifier: String?
        get() = engageable.uiEntityIdentifier
        set(uiEntityIdentifier) {
            engageable.uiEntityIdentifier = uiEntityIdentifier
        }

    override val uiEntityType: UiEntityable.Type?
        get() = engageable.uiEntityType

    override var uiEntityComponentDetail: String?
        get() = engageable.uiEntityComponentDetail
        set(value) {
            engageable.uiEntityComponentDetail = value
        }

    override val uiEntityLabel: String?
        get() = engageable.uiEntityLabel

    override fun setEngagementListener(listener: EngagementListener?) {
        engageable.setEngagementListener(listener)
    }

    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(engageable.getWrappedClickListener(l))
    }
}
