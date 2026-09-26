package com.neverreader.util.android.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import com.neverreader.app.App.Companion.from
import com.neverreader.app.R
import com.neverreader.util.android.drawable.StatefulPaint
import com.neverreader.util.android.view.MaxWidthHelper.MaxWidthView

class ResizeDetectLinearLayout : LinearLayout, ResizeDetectView, ForegroundDrawableHelper.Setter,
    MaxWidthView {
    private val mForegroundDrawable = ForegroundDrawableHelper(this)
    private val mDividerPaint = StatefulPaint()
    private val mMaxWidth: MaxWidthHelper

    private var mListener: OnResizeListener? = null
    private var mDividerInset = 0
    private var mDrawDivider = false

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
        mMaxWidth = MaxWidthHelper(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
        mMaxWidth = MaxWidthHelper(context, attrs, defStyleAttr)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context!!, attrs, defStyleAttr, defStyleRes) {
        init(attrs)
        mMaxWidth = MaxWidthHelper(context, attrs, defStyleAttr, defStyleRes)
    }

    constructor(context: Context?) : super(context) {
        init(null)
        mMaxWidth = MaxWidthHelper()
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(attrs, R.styleable.ResizeDetectLinearLayout)

            val dividerColor =
                a.getColorStateList(R.styleable.ResizeDetectLinearLayout_dividerColor)
            val stroke =
                a.getDimensionPixelSize(R.styleable.ResizeDetectLinearLayout_dividerStroke, 0)
            val inset =
                a.getDimensionPixelSize(R.styleable.ResizeDetectLinearLayout_dividerInset, 0)
            if (dividerColor != null) {
                setDividerStroke(dividerColor, stroke, inset)
            }

            setForegroundDrawable(a.getDrawable(R.styleable.ResizeDetectLinearLayout_android_foreground))

            a.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mListener != null) {
            mListener!!.onViewSizeChanged(this, w, h, oldw, oldh)
        }
        mForegroundDrawable.onParentSizeChanged(w, h, oldw, oldh)
    }

    override fun setOnResizeListener(listener: OnResizeListener?) {
        mListener = listener
    }


    fun setDividerStroke(color: ColorStateList?, stroke: Int, inset: Int) {
        mDrawDivider = true
        mDividerPaint.setStatefulColor(color, getDrawableState())
        mDividerPaint.setStyle(Paint.Style.STROKE)
        mDividerPaint.setStrokeWidth(stroke.toFloat())
        mDividerInset = inset
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        if (mDrawDivider) {
            mDividerPaint.setState(getDrawableState())
            val size = getChildCount()

            for (i in 1..<size) {
                val child = getChildAt(i)
                val leftChild = getChildAt(i - 1)
                if (getOrientation() == HORIZONTAL) {
                    if (leftChild != null && leftChild.getVisibility() == VISIBLE) {
                        val x = child.getLeft().toFloat()
                        canvas.drawLine(
                            x,
                            mDividerInset.toFloat(),
                            x,
                            (getHeight() - mDividerInset).toFloat(),
                            mDividerPaint
                        )
                    }
                } else {
                    val y = child.getTop().toFloat()
                    canvas.drawLine(
                        mDividerInset.toFloat(),
                        y,
                        (getWidth() - mDividerInset).toFloat(),
                        y,
                        mDividerPaint
                    )
                }
            }
        }

        mForegroundDrawable.onParentDispatchDraw(canvas)
    }

    override fun setForegroundDrawable(drawable: Drawable?) {
        mForegroundDrawable.setForegroundDrawable(drawable)
    }

    protected override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || (mForegroundDrawable != null && mForegroundDrawable.onParentVerifyDrawable(
            who
        ))
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        mForegroundDrawable.onParentJumpDrawablesToCurrentState()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mForegroundDrawable.onParentTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        mForegroundDrawable.onParentDrawableStateChanged()
    }

    override fun getMaxWidth(): Int {
        return mMaxWidth.maxWidth
    }

    override fun setMaxWidth(maxWidth: Int) {
        mMaxWidth.maxWidth = maxWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        widthMeasureSpec = mMaxWidth.onMeasure(widthMeasureSpec)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
