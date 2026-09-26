package com.neverreader.util.android.webkit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.webkit.WebView
import com.neverreader.app.App
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.view.RainbowBar
import com.neverreader.util.android.view.ManuallyUpdateTheme
import com.neverreader.util.android.view.ScrollTracker
import kotlin.math.roundToInt


class BaseWebView : WebView, ManuallyUpdateTheme {

    protected var mResizeListener: ResizeListener? = null

    protected var mInteractionListener: OnInteractionListener? = null

    private var mScrollingLocked = false

    private var mProgressBar: RainbowBar? = null
    private var mProgress = 0

    protected var mOnContentDisplayedListener: OnContentDisplayedListener? = null
    protected var mHasContentDisplayed: Boolean = false

    private var mDelayUntilNextDraw: Runnable? = null
    private var mFrozen = false
    private val mFixedHorizontally = ArrayList<View?>()
    private val mFixedVertically = ArrayList<View?>()
    private var mScrollTracker: ScrollTracker? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    @SuppressLint("SetJavaScriptEnabled")
    protected fun init() {
        val settings = getSettings()
        settings.javaScriptEnabled = true
        setFileAccessEnabled(false) // Default off for security.

        overScrollMode = OVER_SCROLL_NEVER
        scrollBarStyle = SCROLLBARS_INSIDE_OVERLAY

        updateThemeManually()

        registerForThemeUpdates()

        mScrollTracker =
            ScrollTracker(this, ViewConfiguration.get(getContext()).scaledTouchSlop)
    }

    fun setFileAccessEnabled(enabled: Boolean) {
        val settings = getSettings()
        settings.allowFileAccess = enabled
        settings.allowUniversalAccessFromFileURLs = enabled
    }

    private fun checkContentDisplay() {
        if (contentHeight > 0) {
            if (!mHasContentDisplayed) {
                mHasContentDisplayed = true
                if (mOnContentDisplayedListener != null) {
                    mOnContentDisplayedListener!!.onContentFirstDisplayedSinceLoad()
                }
            }

            if (mOnContentDisplayedListener != null) {
                mOnContentDisplayedListener!!.onContentDisplayed()
            }
        } else if (contentHeight <= 0) {
            mHasContentDisplayed = false
        }
    }

    override fun invalidate() {
        super.invalidate()
        checkContentDisplay()
    }

    // Interaction Detection
    interface OnInteractionListener {
        fun onInteraction()

        /**
         * Called anytime a touch-down, or a key down is made.
         */
        fun onPossibleSelect() // REVIEW these methods are a bit redundant. onInteraction covers scrolls, but why?
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (mInteractionListener != null) {
                mInteractionListener!!.onPossibleSelect()
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onTouchEvent(ev: MotionEvent?): Boolean {
        if (ev == null) {
            return false
        }

        mScrollTracker!!.onTouchEvent(ev)

        if (mInteractionListener != null) {
            mInteractionListener!!.onInteraction()
            mInteractionListener!!.onPossibleSelect()
        }

        when (ev.getAction()) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {}
            MotionEvent.ACTION_DOWN -> {}
            MotionEvent.ACTION_MOVE -> if (mScrollingLocked) {
                return true
            }
        }

        return super.onTouchEvent(ev)
    }


    // Scroll Detection
    override fun onScrollChanged(x: Int, y: Int, oldx: Int, oldy: Int) {
        mScrollTracker!!.onScrollChanged(x, y, oldx, oldy)

        if (mInteractionListener != null) {
            mInteractionListener!!.onInteraction()
        }

        super.onScrollChanged(x, y, oldx, oldy)
    }

    override fun scrollTo(x: Int, y: Int) {
        var y = y
        if (y < 0) y = 0

        super.scrollTo(x, y)
    }


    interface ResizeListener {
        fun onComputeVerticalScrollExtent()
        fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int)
    }

    override fun onSizeChanged(newWidth: Int, newHeight: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight)
        if (mResizeListener != null) mResizeListener!!.onSizeChanged(
            newWidth,
            newHeight,
            oldWidth,
            oldHeight
        )
    }

    override fun computeVerticalScrollExtent(): Int {
        if (mResizeListener != null) mResizeListener!!.onComputeVerticalScrollExtent()
        return super.computeVerticalScrollExtent()
    }

    override fun draw(canvas: Canvas) {
        if (!mFrozen) {
            super.draw(canvas)
        }
    }

    @SuppressLint("WrongCall")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        mScrollTracker!!.onDraw()

        if (mDelayUntilNextDraw != null) {
            mDelayUntilNextDraw!!.run()
            mDelayUntilNextDraw = null
        }
    }


    // Javascript Commands
    /**
     * Deprecated in favor of [JavascriptFunction].
     *
     *
     * Convenience method that ensures the JavaScript will be executed with "javascript: " at the beginning, and from the UI Thread.
     * @param query
     */
    fun execJS(query: String) {
        App.from(getContext())!!.threads().runOrPostOnUiThread(Runnable {
            val js = "javascript: $query"
            loadUrl(js)
        })
    }

    override fun getProgress(): Int {
        return mProgress
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (mFixedHorizontally.contains(child)) {
            child.offsetLeftAndRight(scrollX - child.left)
        }

        if (mFixedVertically.contains(child)) {
            child.offsetTopAndBottom(scrollY - child.top)
        }

        if (child === mProgressBar) {
            mProgressBar!!.offsetTopAndBottom(scrollY - mProgressBar!!.top)
        }
        return super.drawChild(canvas, child, drawingTime)
    }

    fun webPixelsPerScreenPixels(): Float {
        return 100f / (scale * 100f)
    }

    /**
     * Convert a dimension in web to how many Android pixels it represents.
     *
     * For example, if an element in the webpage is "100px" tall,
     * the number of how many actual physical pixels it takes up,
     * depends on the density of the device and the zoom/scale of the page.
     *
     * This tells you how many screen pixels are used to display a pixel value
     * in the page's css.
     *
     * @param webPx
     * @return
     */
    fun webPxToScreenPx(webPx: Int): Int {
        return (webPx / webPixelsPerScreenPixels()).roundToInt()
    }

    val maxContentScrollY: Int
        /**
         * This returns in web pixels, the maximum scroll Y that web can have.
         *
         * @return
         */
        get() = webPxToScreenPx(contentHeight) - height

    /**
     * A listener for when a BaseWebView first displays content while loading.
     * @author max
     */
    interface OnContentDisplayedListener {
        /**
         * Called when the WebView goes from not having any content displayed to having something displayed.
         */
        fun onContentFirstDisplayedSinceLoad()

        /**
         * Called whenever the WebView's Picture is updated and has content. Will be called after onContentFirstDisplayedSinceLoad()
         */
        fun onContentDisplayed()
    }

    override fun registerForThemeUpdates() {
        val activity = AbsNeverReaderActivity.from(context)
        activity?.registerViewForThemeChanges(this)
    }

    override fun updateThemeManually() {
        setBackgroundColor(App.from(getContext())!!.theme().getThemeBGColor(getContext()))
    }

}
