package com.neverreader.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPx
import kotlin.math.max

class PaletteView : View {
    private val rows: MutableList<IntArray> = ArrayList()
    private val rect = RectF()
    private var swatchPx = 0
    private var spacePx = 0
    private var paint: Paint? = null
    private var cornerRadius = 0f

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        swatchPx = getResources().getDimensionPixelSize(R.dimen.nr_space_md)
        spacePx = getResources().getDimensionPixelSize(R.dimen.nr_space_sm)
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint!!.style = Paint.Style.FILL
        cornerRadius = dpToPx(context, 3f)
    }

    public override fun getSuggestedMinimumWidth(): Int {
        var longestRow = 0
        for (row in rows) {
            longestRow = max(longestRow, row.size)
        }
        return longestRow * (swatchPx + spacePx) - spacePx
    }

    override fun getSuggestedMinimumHeight(): Int {
        return rows.size * (swatchPx + spacePx) - spacePx
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        var x = 0f
        var y = 0f
        for (row in rows) {
            for (color in row) {
                paint!!.color = color
                rect.set(x, y, x + swatchPx, y + swatchPx)
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint!!)
                x += swatchPx.toFloat()
                x += spacePx.toFloat()
            }
            x = 0f
            y += swatchPx.toFloat()
            y += spacePx.toFloat()
        }
    }
}
