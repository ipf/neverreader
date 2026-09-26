package com.neverreader.ui.view.progress.skeleton.row

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.facebook.shimmer.Shimmer.AlphaHighlightBuilder
import com.neverreader.ui.R
import com.neverreader.ui.util.OnlyWhenVisibleHelper
import com.neverreader.ui.view.themed.ThemedConstraintLayout
import com.neverreader.ui.view.themed.ThemedShimmerFrameLayout

abstract class AbsSkeletonRow : ThemedShimmerFrameLayout {
    protected var content: ConstraintLayout? = null

    constructor(context: Context?) : super(context) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    private fun init(context: Context?) {
        content = ThemedConstraintLayout(getContext())
        LayoutInflater.from(context).inflate(this.layout, content, true)

        val padding = defaultPadding()
        content!!.setPadding(padding, 0, padding, 0)

        val builder = AlphaHighlightBuilder()
        builder.setRepeatDelay(2000)
        builder.setDuration(100)
        builder.setBaseAlpha(1f)
        builder.setHighlightAlpha(0.3f)
        builder.setAutoStart(false)

        setShimmer(builder.build())

        addView(content)

        OnlyWhenVisibleHelper.install(
            this,
            { this.startShimmer() },
            { this.stopShimmer() })
    }

    protected fun defaultPadding(): Int {
        return getResources().getDimension(R.dimen.nr_side_grid).toInt()
    }

    @get:LayoutRes
    protected abstract val layout: Int
}
