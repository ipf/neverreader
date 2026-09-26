package com.neverreader.ui.view.badge

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.AttributeSet
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.IntrinsicSizeHelper
import com.neverreader.ui.view.badge.BadgeUtil.getBadgeSize
import com.neverreader.ui.util.updateEnabledAlpha
import com.neverreader.ui.view.themed.ThemedTextView

/**
 * Base class for badge views whose label is text.
 * Be sure to call [.setBadgeColor] and [.setTextColor].
 * For subclasses, a good place to do this from is overriding [.init].
 */
open class TextBadgeView : ThemedTextView {
    private var sizeHelper: IntrinsicSizeHelper? = null

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

    protected open fun init() {
        sizeHelper = IntrinsicSizeHelper(-1, getBadgeSize(getContext()))
        setTextAppearance(getContext(), R.style.App_Text_Small_LightTitle)
        setMaxLines(1)
        setEllipsize(TextUtils.TruncateAt.END)


        //  Set the padding so the text will be vertically center, not, including its ascent. This gives it a more visually centered look.
        val sidePadding = getResources().getDimensionPixelSize(R.dimen.nr_space_sm)
        val fm = getPaint().getFontMetrics()
        val height = getBadgeSize(getContext())
        val topPadding = (((height - -fm.ascent) / 2f) + (fm.top - fm.ascent)).toInt() - dpToPxInt(
            getContext(),
            1f
        )
        setPadding(sidePadding, topPadding, sidePadding, 0)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = sizeHelper!!.applyWidth(widthMeasureSpec)
        heightMeasureSpec = sizeHelper!!.applyHeight(heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun setBadgeColor(colors: ColorStateList): TextBadgeView {
        setBackgroundDrawable(BadgeDrawable(getContext(), colors))
        return this
    }

    public override fun visualAscent(): Int {
        return 0
    }

    public override fun visualDescent(): Int {
        return 0
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        this.updateEnabledAlpha()
    }
}
