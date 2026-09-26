package com.neverreader.util.android.view

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.RelativeLayout
import com.neverreader.app.App.Companion.from
import com.neverreader.app.R
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.util.android.view.MaxWidthHelper.MaxWidthView

open class ResizeDetectRelativeLayout : RelativeLayout, ResizeDetectView,
    ForegroundDrawableHelper.Setter, MaxWidthView {
    private val mMaxWidth: MaxWidthHelper

    private val mForegroundDrawable: ForegroundDrawableHelper? = ForegroundDrawableHelper(this)

    private var mListener: OnResizeListener? = null

    private var mFrag: AbsNeverReaderFragment? = null

    private var mMaxHeight = 0

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context!!, attrs, defStyleAttr, defStyleRes) {
        mMaxWidth = MaxWidthHelper(context, attrs, defStyleAttr, defStyleRes)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!,
        attrs,
        defStyle
    ) {
        mMaxWidth = MaxWidthHelper(context, attrs, defStyle)
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        mMaxWidth = MaxWidthHelper(context, attrs)

        val a = getContext().obtainStyledAttributes(attrs, R.styleable.NeverReaderTheme)
        mMaxHeight = a.getDimensionPixelSize(R.styleable.NeverReaderTheme_maxHeight, 0)
        a.recycle()
    }

    constructor(context: Context?) : super(context) {
        mMaxWidth = MaxWidthHelper()
    }

    override fun setForegroundDrawable(drawable: Drawable?) {
        mForegroundDrawable!!.setForegroundDrawable(drawable)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (mListener != null) {
            mListener!!.onViewSizeChanged(this, w, h, oldw, oldh)
        }

        mForegroundDrawable!!.onParentSizeChanged(w, h, oldw, oldh)
    }

    override fun setOnResizeListener(listener: OnResizeListener?) {
        mListener = listener
    }

    fun setFrag(frag: AbsNeverReaderFragment?) {
        mFrag = frag
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, from(getContext())!!.theme().getState(this, mFrag))
        return state
    }

    override fun getMaxWidth(): Int {
        return mMaxWidth.maxWidth
    }

    override fun setMaxWidth(maxWidth: Int) {
        mMaxWidth.maxWidth = maxWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        widthMeasureSpec = mMaxWidth.onMeasure(widthMeasureSpec)

        val measuredHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (mMaxHeight > 0 && mMaxHeight < measuredHeight) {
            val measureMode = MeasureSpec.getMode(heightMeasureSpec)
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(mMaxHeight, measureMode)
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        mForegroundDrawable!!.onParentDispatchDraw(canvas)
    }

    protected override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || (mForegroundDrawable != null && mForegroundDrawable.onParentVerifyDrawable(
            who
        ))
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        mForegroundDrawable!!.onParentJumpDrawablesToCurrentState()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mForegroundDrawable!!.onParentTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        mForegroundDrawable!!.onParentDrawableStateChanged()
    }
}
