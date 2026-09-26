package com.neverreader.ui.view.themed

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import com.neverreader.ui.analytics.Engageable
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.analytics.EngageableHelper
import com.neverreader.ui.analytics.EngagementListener

open class ThemedConstraintLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), Engageable {
    @JvmField
    protected val engageable: EngageableHelper = EngageableHelper()

    init {
        engageable.obtainStyledAttributes(context, attrs)
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

    override var uiEntityType: UiEntityable.Type?
        get() = engageable.uiEntityType
        set(type) {
            engageable.uiEntityType = type
        }

    override var uiEntityComponentDetail: String?
        get() = engageable.uiEntityComponentDetail
        set(detail) {
            engageable.uiEntityComponentDetail = detail
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
