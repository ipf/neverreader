/*
 * From https://github.com/ApmeM/android-flowlayout/blob/master/libraries/layouts/src/main/java/org/apmem/tools/layouts/FlowLayout.java
 * Apache 2.0 License.
 */
package org.apmem.tools.layouts

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import com.neverreader.app.R
import kotlin.math.max

/**
 * From https://github.com/ApmeM/android-flowlayout
 * Apache 2.0 License.
 *
 *
 * This layout class wraps views like a text view wraps words. You can add views, and they are positioned one after another,
 * and when it gets to the edge, it wraps and continues onto the next line.
 *
 *
 * It has a horizontal and vertical layout mode.
 *
 *
 * Some additional modifications were made to the class, such as the ability to set [.maxLines]. This will truncate
 * the view to a maximum number of lines and not show any views that do not fit.
 */
open class FlowLayout : ViewGroup {
    private val mLineHeights = SparseIntArray()

    private var horizontalSpacing = 0
    private var verticalSpacing = 0
    private var orientation = 0
    private var debugDraw = false
    private var maxLines = 0
    private var layoutCenter = false

    constructor(context: Context) : super(context) {
        this.readStyleParameters(context, null)
    }

    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        this.readStyleParameters(context, attributeSet)
    }

    constructor(context: Context, attributeSet: AttributeSet?, defStyle: Int) : super(
        context,
        attributeSet,
        defStyle
    ) {
        this.readStyleParameters(context, attributeSet)
    }

    /**
     * -1 turns this into a single line. will endlessly expand horizontally.
     * 0 lets it expand vertically, wrapping new lines.
     * >0 Caps it to a max number of lines.
     * @param maxLines
     */
    fun setMaxLines(maxLines: Int) {
        this.maxLines = maxLines
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        mLineHeights.clear()

        val sizeWidth =
            MeasureSpec.getSize(widthMeasureSpec) - this.getPaddingRight() - this.getPaddingLeft()
        val sizeHeight =
            MeasureSpec.getSize(heightMeasureSpec) - this.getPaddingTop() - this.getPaddingBottom()

        val modeWidth = MeasureSpec.getMode(widthMeasureSpec)
        val modeHeight = MeasureSpec.getMode(heightMeasureSpec)

        val size: Int
        val mode: Int

        if (orientation == HORIZONTAL) {
            size = sizeWidth
            mode = modeWidth
        } else {
            size = sizeHeight
            mode = modeHeight
        }

        var line = 0
        var lineThicknessWithSpacing = 0
        var lineThickness = 0
        var lineLengthWithSpacing = 0
        var lineLength: Int

        var prevLinePosition = 0

        var controlMaxLength = 0
        var controlMaxThickness = 0
        var maxLinesLimit = 0

        val count = getChildCount()
        for (i in 0..<count) {
            val child = getChildAt(i)
            if (child.getVisibility() == GONE) {
                continue
            }

            val lp = child.getLayoutParams() as LayoutParams

            child.measure(
                getChildMeasureSpec(
                    widthMeasureSpec,
                    this.getPaddingLeft() + this.getPaddingRight(),
                    lp.width
                ),
                getChildMeasureSpec(
                    heightMeasureSpec,
                    this.getPaddingTop() + this.getPaddingBottom(),
                    lp.height
                )
            )

            val hSpacing = this.getHorizontalSpacing(lp)
            val vSpacing = this.getVerticalSpacing(lp)

            val childWidth = child.getMeasuredWidth()
            val childHeight = child.getMeasuredHeight()

            val childLength: Int
            val childThickness: Int
            val spacingLength: Int
            val spacingThickness: Int

            if (orientation == HORIZONTAL) {
                childLength = childWidth
                childThickness = childHeight
                spacingLength = hSpacing
                spacingThickness = vSpacing
            } else {
                childLength = childHeight
                childThickness = childWidth
                spacingLength = vSpacing
                spacingThickness = hSpacing
            }

            lineLength = lineLengthWithSpacing + childLength
            lineLengthWithSpacing = lineLength + spacingLength

            val newLine = maxLines != SINGLE_LINE_NO_CAP
                    && (lp.newLine || (mode != MeasureSpec.UNSPECIFIED && lineLength > size))
            if (newLine) {
                prevLinePosition = prevLinePosition + lineThicknessWithSpacing

                line++
                lineThickness = childThickness
                lineLength = childLength
                lineThicknessWithSpacing = childThickness + spacingThickness
                lineLengthWithSpacing = lineLength + spacingLength
            }

            lineThicknessWithSpacing =
                max(lineThicknessWithSpacing, childThickness + spacingThickness)
            lineThickness = max(lineThickness, childThickness)

            val posX: Int
            val posY: Int
            if (orientation == HORIZONTAL) {
                posX = getPaddingLeft() + lineLength - childLength
                posY = getPaddingTop() + prevLinePosition
            } else {
                posX = getPaddingLeft() + prevLinePosition
                posY = getPaddingTop() + lineLength - childHeight
            }
            lp.setPosition(posX, posY)

            controlMaxLength = max(controlMaxLength, lineLength)
            controlMaxThickness = prevLinePosition + lineThickness

            if (maxLines > 0 && line == maxLines - 1) {
                maxLinesLimit = controlMaxThickness
            }

            lp.line = line
            mLineHeights.put(line, lineThickness)
        }

        if (maxLinesLimit != 0) {
            controlMaxThickness = maxLinesLimit
        }

        /* need to take paddings into account */
        if (orientation == HORIZONTAL) {
            controlMaxLength += getPaddingLeft() + getPaddingRight()
            controlMaxThickness += getPaddingBottom() + getPaddingTop()
        } else {
            controlMaxLength += getPaddingBottom() + getPaddingTop()
            controlMaxThickness += getPaddingLeft() + getPaddingRight()
        }

        if (orientation == HORIZONTAL) {
            this.setMeasuredDimension(
                resolveSize(controlMaxLength, widthMeasureSpec),
                resolveSize(controlMaxThickness, heightMeasureSpec)
            )
        } else {
            this.setMeasuredDimension(
                resolveSize(controlMaxThickness, widthMeasureSpec),
                resolveSize(controlMaxLength, heightMeasureSpec)
            )
        }
    }

    private fun getVerticalSpacing(lp: LayoutParams): Int {
        val vSpacing: Int
        if (lp.verticalSpacingSpecified()) {
            vSpacing = lp.verticalSpacing
        } else {
            vSpacing = this.verticalSpacing
        }
        return vSpacing
    }

    private fun getHorizontalSpacing(lp: LayoutParams): Int {
        val hSpacing: Int
        if (lp.horizontalSpacingSpecified()) {
            hSpacing = lp.horizontalSpacing
        } else {
            hSpacing = this.horizontalSpacing
        }
        return hSpacing
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = getChildCount()
        for (i in 0..<count) {
            val child = getChildAt(i)
            val lp = child.getLayoutParams() as LayoutParams

            var left = lp.x
            var top = lp.y
            var right = lp.x + child.getMeasuredWidth()
            var bottom = lp.y + child.getMeasuredHeight()

            if (lp.center || layoutCenter) {
                val lineHeight = mLineHeights.get(lp.line)
                if (orientation == HORIZONTAL) {
                    top = ((lineHeight - child.getMeasuredHeight()) / 2f).toInt()
                    bottom = top + child.getMeasuredHeight()
                } else {
                    left = ((lineHeight - child.getMeasuredWidth()) / 2f).toInt()
                    right = left + child.getMeasuredWidth()
                }
            }
            child.layout(left, top, right, bottom)
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val more = super.drawChild(canvas, child, drawingTime)
        this.drawDebugInfo(canvas, child)
        return more
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun generateLayoutParams(attributeSet: AttributeSet?): LayoutParams {
        return LayoutParams(getContext(), attributeSet)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams {
        return LayoutParams(p)
    }

    private fun readStyleParameters(context: Context, attributeSet: AttributeSet?) {
        val a = context.obtainStyledAttributes(attributeSet, R.styleable.FlowLayout)
        try {
            applyAttributes(a)
        } finally {
            a.recycle()
        }
    }

    protected fun applyStyle(styleId: Int) {
        if (styleId == 0) {
            return
        }

        val a = getContext().obtainStyledAttributes(styleId, R.styleable.FlowLayout)
        try {
            applyAttributes(a)
        } finally {
            a.recycle()
        }
    }

    private fun applyAttributes(a: TypedArray) {
        horizontalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_horizontalSpacing, 0)
        verticalSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_verticalSpacing, 0)
        orientation = a.getInteger(R.styleable.FlowLayout_flow_orientation, HORIZONTAL)
        debugDraw = a.getBoolean(R.styleable.FlowLayout_debugDraw, false)
        layoutCenter = a.getBoolean(R.styleable.FlowLayout_flow_layout_center, false)
        maxLines = a.getInteger(R.styleable.FlowLayout_maxFlowLines, 0)
    }

    private fun drawDebugInfo(canvas: Canvas, child: View) {
        if (!debugDraw) {
            return
        }

        val childPaint = this.createPaint(-0x100)
        val layoutPaint = this.createPaint(-0xff0100)
        val newLinePaint = this.createPaint(-0x10000)

        val lp = child.getLayoutParams() as LayoutParams

        if (lp.horizontalSpacing > 0) {
            val x = child.getRight().toFloat()
            val y = child.getTop() + child.getHeight() / 2.0f
            canvas.drawLine(x, y, x + lp.horizontalSpacing, y, childPaint)
            canvas.drawLine(
                x + lp.horizontalSpacing - 4.0f,
                y - 4.0f,
                x + lp.horizontalSpacing,
                y,
                childPaint
            )
            canvas.drawLine(
                x + lp.horizontalSpacing - 4.0f,
                y + 4.0f,
                x + lp.horizontalSpacing,
                y,
                childPaint
            )
        } else if (this.horizontalSpacing > 0) {
            val x = child.getRight().toFloat()
            val y = child.getTop() + child.getHeight() / 2.0f
            canvas.drawLine(x, y, x + this.horizontalSpacing, y, layoutPaint)
            canvas.drawLine(
                x + this.horizontalSpacing - 4.0f,
                y - 4.0f,
                x + this.horizontalSpacing,
                y,
                layoutPaint
            )
            canvas.drawLine(
                x + this.horizontalSpacing - 4.0f,
                y + 4.0f,
                x + this.horizontalSpacing,
                y,
                layoutPaint
            )
        }

        if (lp.verticalSpacing > 0) {
            val x = child.getLeft() + child.getWidth() / 2.0f
            val y = child.getBottom().toFloat()
            canvas.drawLine(x, y, x, y + lp.verticalSpacing, childPaint)
            canvas.drawLine(
                x - 4.0f,
                y + lp.verticalSpacing - 4.0f,
                x,
                y + lp.verticalSpacing,
                childPaint
            )
            canvas.drawLine(
                x + 4.0f,
                y + lp.verticalSpacing - 4.0f,
                x,
                y + lp.verticalSpacing,
                childPaint
            )
        } else if (this.verticalSpacing > 0) {
            val x = child.getLeft() + child.getWidth() / 2.0f
            val y = child.getBottom().toFloat()
            canvas.drawLine(x, y, x, y + this.verticalSpacing, layoutPaint)
            canvas.drawLine(
                x - 4.0f,
                y + this.verticalSpacing - 4.0f,
                x,
                y + this.verticalSpacing,
                layoutPaint
            )
            canvas.drawLine(
                x + 4.0f,
                y + this.verticalSpacing - 4.0f,
                x,
                y + this.verticalSpacing,
                layoutPaint
            )
        }

        if (lp.newLine) {
            if (orientation == HORIZONTAL) {
                val x = child.getLeft().toFloat()
                val y = child.getTop() + child.getHeight() / 2.0f
                canvas.drawLine(x, y - 6.0f, x, y + 6.0f, newLinePaint)
            } else {
                val x = child.getLeft() + child.getWidth() / 2.0f
                val y = child.getTop().toFloat()
                canvas.drawLine(x - 6.0f, y, x + 6.0f, y, newLinePaint)
            }
        }
    }

    private fun createPaint(color: Int): Paint {
        val paint = Paint()
        paint.setAntiAlias(true)
        paint.setColor(color)
        paint.setStrokeWidth(2.0f)
        return paint
    }

    class LayoutParams : ViewGroup.LayoutParams {
        var x = 0
        var y = 0
        var line = 0
        var horizontalSpacing: Int = NO_SPACING
        var verticalSpacing: Int = NO_SPACING
        var center = false
        var newLine = false

        constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
            this.readStyleParameters(context, attributeSet)
        }

        constructor(width: Int, height: Int) : super(width, height)

        constructor(layoutParams: ViewGroup.LayoutParams?) : super(layoutParams)

        fun horizontalSpacingSpecified(): Boolean {
            return horizontalSpacing != NO_SPACING
        }

        fun verticalSpacingSpecified(): Boolean {
            return verticalSpacing != NO_SPACING
        }

        fun setPosition(x: Int, y: Int) {
            this.x = x
            this.y = y
        }

        private fun readStyleParameters(context: Context, attributeSet: AttributeSet?) {
            val a =
                context.obtainStyledAttributes(attributeSet, R.styleable.FlowLayout_LayoutParams)
            try {
                horizontalSpacing = a.getDimensionPixelSize(
                    R.styleable.FlowLayout_LayoutParams_layout_horizontalSpacing,
                    NO_SPACING
                )
                verticalSpacing = a.getDimensionPixelSize(
                    R.styleable.FlowLayout_LayoutParams_layout_verticalSpacing,
                    NO_SPACING
                )
                newLine = a.getBoolean(R.styleable.FlowLayout_LayoutParams_layout_newLine, false)
                center = a.getBoolean(R.styleable.FlowLayout_LayoutParams_layout_center, false)
            } finally {
                a.recycle()
            }
        }

        companion object {
            private val NO_SPACING = -1
        }
    }

    companion object {
        const val HORIZONTAL: Int = 0

        val SINGLE_LINE_NO_CAP: Int = -1
    }
}
