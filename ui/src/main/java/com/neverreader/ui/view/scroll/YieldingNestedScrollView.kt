package com.neverreader.ui.view.scroll

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.neverreader.ui.view.themed.ThemedNestedScrollView

/**
 * A [ThemedNestedScrollView] that will not intercept touch events if its content is not scrollable.
 */
class YieldingNestedScrollView : ThemedNestedScrollView {
    private var isScrollable = false

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        isScrollable =
            childCount > 0 && (height < getChildAt(0).height + paddingTop + paddingBottom ||
                    width < getChildAt(0).width + paddingLeft + paddingRight
                    )
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isScrollable) {
            return super.onInterceptTouchEvent(ev)
        } else {
            return false
        }
    }
}
