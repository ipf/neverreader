package com.neverreader.ui.view.progress.skeleton

import android.content.Context
import android.util.AttributeSet
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.view.themed.ThemedLinearLayout
import com.neverreader.util.java.RandomSingleton

class SkeletonParagraphView : ThemedLinearLayout {
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        setOrientation(VERTICAL)

        val ta = context.obtainStyledAttributes(attrs, R.styleable.SkeletonParagraphView)

        val minLines = ta.getInt(R.styleable.SkeletonParagraphView_minLines, 1)
        val maxLines = ta.getInt(R.styleable.SkeletonParagraphView_maxLines, 2)

        require(minLines <= maxLines) { "minLines must be less than maxLines" }

        val totalLines = RandomSingleton.get().nextInt(maxLines - minLines + 1) + minLines
        if (totalLines == 0) {
            // if no lines, set ourself to GONE to avoid extra padding
            setVisibility(GONE)
        } else {
            for (i in 0..<totalLines) {
                addLine(context, i == totalLines - 1)
            }
        }

        ta.recycle()
    }

    private fun addLine(context: Context, isLast: Boolean) {
        val view = SkeletonView(context)
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            context.getResources().getDimension(R.dimen.nr_skeleton_text_height).toInt()
        )
        val margin = dpToPxInt(context, 5f)
        params.setMargins(0, margin, 0, margin)
        view.setLayoutParams(params)
        view.bind().background(
            if (isLast) R.color.nr_themed_grey_5 else R.color.nr_themed_grey_6,
            context.getResources().getDimension(
                R.dimen.nr_skeleton_text_corner_radius
            )
        ).randomWidth(if (isLast) 0.2f else 0.7f, if (isLast) 0.7f else 1f)
        this.addView(view)
    }
}
