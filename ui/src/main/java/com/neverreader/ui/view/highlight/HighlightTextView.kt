package com.neverreader.ui.view.highlight

import android.content.Context
import android.content.res.ColorStateList
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import com.neverreader.ui.R
import com.neverreader.ui.util.NestedColorStateList.get
import com.neverreader.ui.view.highlight.HighlightSpan.HighlightedRegion
import com.neverreader.ui.view.themed.ThemedTextView

/**
 * A TextView that displays a visually highlighted quote.
 */
class HighlightTextView : ThemedTextView {
    private var color: ColorStateList? = null

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context, attrs)
    }

    private fun init(context: Context?, attrs: AttributeSet?) {
        setTextAppearance(context, R.style.App_Text_Small_Medium)
        color = get(getContext(), R.color.nr_themed_amber_4)


        // Ensure there is a HighlightSpan on all text set on this view,
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (TextUtils.isEmpty(s)) {
                    return
                }


                // Ensure there is a single HighlightSpan and HighlightRegion covering the entire text
                // Ignore if it is already set up (to avoid infinite loops)
                val fulls = s!!.getSpans<HighlightSpan?>(0, s.length, HighlightSpan::class.java)
                val regions =
                    s.getSpans<HighlightedRegion?>(0, s.length, HighlightedRegion::class.java)
                if (fulls.size == 0 || regions.size == 0 || fulls.size > 1 || regions.size > 1 || s.getSpanEnd(
                        fulls[0]
                    ) != s.length - 1 || s.getSpanStart(fulls[0]) != 0 || s.getSpanEnd(regions[0]) != s.length - 1 || s.getSpanStart(
                        regions[0]
                    ) != 0
                ) {
                    // Not setup, or setup incorrectly, so setup again.

                    HighlightSpan.Companion.removeAll(s)
                    HighlightSpan(this@HighlightTextView, color!!)
                        .attach(s, 0, s.length)
                }
            }
        })
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(true) // We currently don't support a disabled state for this view
    }
}
