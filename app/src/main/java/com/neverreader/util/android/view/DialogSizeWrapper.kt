package com.neverreader.util.android.view

import android.content.Context
import android.util.AttributeSet
import com.neverreader.app.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.view.button.ButtonBoxDrawable
import android.widget.FrameLayout
import com.neverreader.util.android.FormFactor.dpToPx
import com.neverreader.util.android.WindowManagerUtil.getScreenHeight
import com.neverreader.util.android.WindowManagerUtil.getScreenWidth
import kotlin.math.min


/**
 * Handles the logic of creating a dialog size that fits the screen well
 */
class DialogSizeWrapper : FrameLayout {
    private var mMaxWidthPx = 0f
    private var mMaxHeight = 0f

    private val mCapHeight = true
    private val mPad = dpToPxInt(getContext(), 1f)

    constructor(context: Context) : super(context) {
        setMaxWidth(getResources().getDimension(R.dimen.dialog_max_width))
        setMaxHeight(getResources().getDimension(R.dimen.dialog_max_height))
        background = ButtonBoxDrawable(
            context,
            com.neverreader.ui.R.color.nr_bg,
            R.color.add_overlay_free_stroke,
            4f,
        )
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.DialogSizeWrapper)
        setMaxWidth(
            a.getDimension(
                R.styleable.DialogSizeWrapper_max_width, getResources().getDimension(
                    R.dimen.dialog_max_width
                )
            )
        )
        setMaxHeight(getResources().getDimension(R.dimen.dialog_max_height))
        a.recycle()
        background = ButtonBoxDrawable(
            context,
            com.neverreader.ui.R.color.nr_bg,
            R.color.add_overlay_free_stroke,
            4f,
        )
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val displayWidth = getScreenWidth(context)
        val displayHeight = getScreenHeight(context)

        val res = getResources()
        val minPadding = res.getDimension(R.dimen.dialog_min_padding).toInt()
        var maxWidth = mMaxWidthPx.toInt()
        var maxHeight = mMaxHeight.toInt()

        maxWidth = min(displayWidth - minPadding, maxWidth)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val measuredWidth =
            measure(widthMode, widthSpecSize, maxWidth, true) // OPT use the min values
        val adjustedWidthMeasureSpec = MeasureSpec.makeMeasureSpec(measuredWidth, widthMode)

        maxHeight = min(displayHeight - minPadding, maxWidth)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)
        val measuredHeight = measure(heightMode, heightSpecSize, maxHeight, mCapHeight)
        val adjustedHeightMeasureSpec = MeasureSpec.makeMeasureSpec(measuredHeight, heightMode)

        super.onMeasure(adjustedWidthMeasureSpec, adjustedHeightMeasureSpec)
    }

    private fun measure(specMode: Int, specSize: Int, max: Int, capToMax: Boolean): Int {
        var result = specSize

        if (specMode == MeasureSpec.EXACTLY) {
            // We were told how big to be
            result = specSize
        } else {
            if (capToMax) {
                result = min(max, result) // Must be <= than max
            }

            if (specMode == MeasureSpec.AT_MOST) {
                // Respect AT_MOST value if that was what is called for by measureSpec
                result = min(result, specSize)
            }
        }

        return result
    }

    fun setMaxHeight(height: Float) {
        mMaxHeight = height
        requestLayout()
        invalidate()
    }

    fun setMaxWidth(dp: Float) {
        mMaxWidthPx = dpToPx(dp).toFloat()
        requestLayout()
        invalidate()
    }
}
