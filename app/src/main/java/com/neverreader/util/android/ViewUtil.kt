package com.neverreader.util.android

import android.R
import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.view.ViewParent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.ProgressBar
import com.neverreader.util.java.Logs
import com.neverreader.util.java.Range.Companion.limit

object ViewUtil {
    /** A shared instance rect to be used by methods in this class, but only on the uithread to ensure things to break  */
    private val mUiThreadRect = Rect()

    /**
     * Helper for ensuring absolutely that the soft keyboard opens/closes when focusing/unfocusing.
     * @param focus whether
     */
    fun forceFocus(focus: Boolean, view: View): Boolean {
        if (focus) {
            view.requestFocus()
        } else {
            view.clearFocus()
        }

        return forceSoftKeyboard(focus, view)
    }

    /**
     * Force a soft keyboard open or closed for a view.
     *
     * @param open true if force open, false if force close
     * @param view the view focused or being unfocused
     */
    fun forceSoftKeyboard(open: Boolean, view: View): Boolean {
        val mgr =
            view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (open) {
            return mgr.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
        } else {
            return mgr.hideSoftInputFromWindow(view.getWindowToken(), 0)
        }
    }

    fun fadeView(view: View, visible: Boolean, duration: Long) {
        val fromAlpha = (if (visible) 0 else 1).toFloat()
        val toAlpha = (if (visible) 1 else 0).toFloat()
        val ani = AlphaAnimation(fromAlpha, toAlpha)
        ani.setDuration(duration)
        //ani.setFillEnabled(true);
        //ani.setFillAfter(true);
        ani.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationRepeat(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                if (!visible) view.setVisibility(View.GONE)
            }
        })

        view.setVisibility(View.VISIBLE)
        view.startAnimation(ani)
    }

    fun isVisible(view: View?): Boolean {
        return view != null && view.getVisibility() == View.VISIBLE
    }

    /**
     * Set the bottom padding of a view in px.
     * @param view
     * @param padding
     */
    fun setPaddingBottom(view: View, padding: Int) {
        view.setPadding(
            view.getPaddingLeft(), view.getPaddingTop(),
            view.getPaddingRight(), padding
        )
    }

    /**
     * Set the left padding of a view in px.
     * @param view
     * @param padding
     */
    fun setPaddingLeft(view: View, padding: Int) {
        view.setPadding(
            padding, view.getPaddingTop(),
            view.getPaddingRight(), view.getPaddingBottom()
        )
    }

    /**
     * Set the right padding of a view in px.
     * @param view
     * @param padding
     */
    fun setPaddingRight(view: View, padding: Int) {
        view.setPadding(
            view.getPaddingLeft(), view.getPaddingTop(),
            padding, view.getPaddingBottom()
        )
    }

    /**
     * Set the left and right padding of a view in px.
     */
    fun setPaddingHorizontal(view: View, leftAndRight: Int) {
        view.setPadding(
            leftAndRight, view.getPaddingTop(),
            leftAndRight, view.getPaddingBottom()
        )
    }

    /**
     * Convenience for showing [View.VISIBLE] and hiding [View.GONE] a view.
     *
     *
     * Safe to pass a null view. Nothing will happen in that case.
     * @param view
     * @param visible
     */
    fun setVisible(view: View?, visible: Boolean) {
        if (view == null) {
            return
        }
        setVisible(visible, view)
    }

    fun setVisible(visible: Boolean, vararg views: View?) {
        setVisibility(if (visible) View.VISIBLE else View.GONE, *views)
    }

    fun setVisibility(visibility: Int, vararg views: View?) {
        for (view in views) {
            if (view != null) {
                view.setVisibility(visibility)
            }
        }
    }

    /**
     * Searches a [ViewGroup] for a visible child at the provided screen coordinates.
     * If the parent is scrollable, adjust the x and y to be within the scrollable area if needed.
     * Otherwise this is just made with non scrollable views in mind.
     *
     * @param parent The parent to search within, will only return direct children of this view.
     * @param downX The x position on screen.
     * @param downY The y position on screen.
     * @return The child [View] or null if parent was not a [ViewGroup] or if no child was found.
     */
    fun getChildViewForCoord(parent: View?, downX: Float, downY: Float): View? {
        if (parent !is ViewGroup) {
            return null
        }

        val viewGroup = parent
        val rect = Rect()
        val parentLocation = IntArray(2)

        val size = viewGroup.getChildCount()
        parent.getLocationOnScreen(parentLocation)

        val x = downX.toInt() - parentLocation[0]
        val y = downY.toInt() - parentLocation[1]

        for (i in size - 1 downTo 0) { // Reverse order so it finds the top of the z index first
            val child = viewGroup.getChildAt(i)
            if (child.getVisibility() != View.VISIBLE) {
                continue
            }
            child.getHitRect(rect)
            if (rect.contains(x, y)) {
                return child
            }
        }
        return null
    }

    /**
     * Searches a View to see if it or a sub View is a specific View.
     * Recursively traverses children.
     *
     * @param parent
     * The view to search in.
     * @param view
     * The view to search for.
     * @return True if the view is a child of (or is) the parent.
     */
    fun containsView(parent: View?, view: View?): Boolean {
        if (view === parent) {
            return true
        }

        if (parent is ViewGroup) {
            val viewGroup = parent
            val children = viewGroup.getChildCount()
            // Search children
            for (i in 0..<children) {
                if (containsView(viewGroup.getChildAt(i), view)) {
                    return true
                }
            }
        }

        return false
    }

    fun setCancelOnOutsideTouch(popup: PopupWindow) {
        popup.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        popup.setOutsideTouchable(true)
        popup.setFocusable(true)
    }

    fun refreshDrawableStateDeep(view: View?) {
        view?.refreshDrawableState()
        view?.invalidate()
        if (view is ViewGroup) {
            val parent = view
            val children = parent.getChildCount()
            // Refresh children
            for (i in 0..<children) {
                refreshDrawableStateDeep(parent.getChildAt(i))
            }
        }
    }

    /**
     * Set a [ProgressBar]'s progress with a percent 0-1.
     *
     * @param bar
     * @param percent
     */
    fun setProgress(bar: ProgressBar, percent: Float) {
        bar.setProgress((bar.getMax() * percent).toInt())
    }

    fun setLayoutWidth(view: View, width: Int) {
        var lp = view.getLayoutParams()
        if (lp == null) {
            lp = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        lp.width = width
        view.setLayoutParams(lp)
    }

    fun setLayoutHeight(view: View, height: Int) {
        var lp = view.getLayoutParams()
        if (lp == null) {
            lp = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height)
        }
        lp.height = height
        view.setLayoutParams(lp)
    }

    /**
     * Changes touch events so they fall within another view instead. Changes if needed so it is within the bounds
     * of the view, trying to get as close to the original point as possible. Dispatches the event to the child.
     *
     *
     * As an example imagine a parent who invokes this on a child view. The child view has a left, top, right and bottom of
     * 10,10,20,20 with its parent. The touch event has a x of 5 and a y of 15.
     *
     *
     * The x falls out of the bounds of the child view (x is to the left).
     *
     *
     * This method will change the touch event so that x=10,y=15.
     *
     * @param view
     */
    fun moveTouchEventIntoBounds(event: MotionEvent, view: View) {
        var x = event.getX()
        var y = event.getY()

        x = limit(view.getLeft().toFloat(), view.getRight().toFloat(), x)
        y = limit(view.getTop().toFloat(), view.getBottom().toFloat(), y)

        event.setLocation(x, y)

        view.dispatchTouchEvent(event)
    }

    /**
     * Gets a parent up the chain. steps = 1 gets the direct parent. steps = 2 gets the parent of the parent and so on.
     *
     *
     * Handles nulls safely so if there are only 2 parents and you ask for 5, it will just return null.
     *
     * @param child null safe, will just return null
     * @param steps
     * @return
     */
    fun getParent(child: View?, steps: Int): View? {
        var child = child
        var steps = steps
        while (child != null && steps > 0) {
            if (child.getParent() is View) {
                child = child.getParent() as View?
            } else {
                child = null
            }
            steps--
        }

        return child
    }

    /**
     * For Views in ViewGroups using the android:animateLayoutChanges parameter, this provides the option to
     * remove the View from its parent without triggering layout transition animations.
     */
    /**
     * Removes a view from its parent and returns true. If it does not have a ViewGroup parent, it will
     * do nothing and return false.
     * @return true if removed
     */
    @JvmOverloads
    fun remove(view: View?, animate: Boolean = true): Boolean {
        if (view != null && view.getParent() is ViewGroup) {
            val parent = (view.getParent() as ViewGroup)
            var transition: LayoutTransition? = null
            if (!animate) {
                transition = parent.getLayoutTransition()
                parent.setLayoutTransition(null)
            }
            parent.removeView(view)
            if (transition != null) {
                parent.setLayoutTransition(transition)
            }
            return true
        } else {
            return false
        }
    }

    fun getPercentVisible(view: View?): Float {
        // First check basic visibility flags, starting from inexpensive to more expensive checks.
        if (view == null || view.getVisibility() != View.VISIBLE || view.getWidth() <= 0 || view.getHeight() <= 0 || view.getWindowVisibility() != View.VISIBLE || !view.isShown() || !view.isAttachedToWindow()) {
            return 0f
        }

        if (!view.getGlobalVisibleRect(mUiThreadRect)) {
            // Careful, this always returns true if the view isn't attached to a window. 'false' is the only state you can safely trust. True still needs to be double checked!
            return 0f
        } else {
            val visibleArea = (mUiThreadRect.width() * mUiThreadRect.height()).toFloat()
            val totalArea = (view.getWidth() * view.getHeight()).toFloat()
            return visibleArea / totalArea
        }
    }

    /**
     * Is this view visible to the user?
     *
     * @param view The view to check, if null it will return false.
     * @param minVisiblePercent [0-1] The minimum percentage of the view's area that must be seen in order to consider it visible.
     * 0 means even a single pixel will be considered visible. 1 means every pixel must be visible.
     * @return
     */
    fun isVisibleToUserCompat(view: View?, minVisiblePercent: Float): Boolean {
        if (view == null || !view.isShown() || view.getWindowVisibility() != View.VISIBLE || view.getVisibility() != View.VISIBLE || view.getWidth() <= 0 || view.getHeight() <= 0 || !isAttachedToViewRoot(
                view
            )
        ) {
            if (false) {
                val reason: String?
                if (view == null) {
                    reason = "null"
                } else if (!view.isShown()) {
                    reason = "not shown"
                } else if (view.getWindowVisibility() != View.VISIBLE) {
                    reason = "window vis"
                } else if (view.getVisibility() != View.VISIBLE) {
                    reason = "view vis"
                } else if (view.getWidth() <= 0 || view.getHeight() <= 0) {
                    reason = "view size"
                } else if (!isAttachedToViewRoot(view)) {
                    reason = "view root"
                } else {
                    reason = "unknown"
                }
                WIP.l("VISCHECK ~ HIDDEN ~ " + reason)
            }
            return false
        } else {
            val visible =
                view.getGlobalVisibleRect(mUiThreadRect) // Note this always returns true when the view is not attached to a window, hence the isAttachedToViewRoot() check above.
            if (!visible) {
                WIP.l("VISCHECK ~ HIDDEN ~ not visible in global rect")
                return false
            } else if (minVisiblePercent <= 0) {
                WIP.l("VISCHECK ~ VISIBLE ~ any percent allowed")
                return true
            } else {
                val visibleArea = (mUiThreadRect.width() * mUiThreadRect.height()).toFloat()
                val totalArea = (view.getWidth() * view.getHeight()).toFloat()
                val percent = visibleArea / totalArea
                if (percent >= minVisiblePercent) {
                    WIP.l("VISCHECK ~ VISIBLE ~ " + percent)
                    return true
                } else {
                    WIP.l("VISCHECK ~ HIDDEN ~ " + percent)
                    return false
                }
            }
        }
    }

    /**
     * @return The last parent returned by continually invoking [View.getParent] before it returned null. This can return null if
     * the view itself has no parent.
     */
    fun getRootParent(view: View): ViewParent? {
        var last: ViewParent? = null
        var current = view.getParent()
        while (current != null) {
            last = current
            current = current.getParent()
        }
        return last
    }

    /**
     * [View.getRootView] returns the DecorView but the DecorView is typically the full window size
     * and includes all of the screen decor like on screen nav, status bars, etc. If you need access instead
     * to the root view that is actually inset and the size of the visible area of your view/activity,
     * this method will try to find the content view. If it can't it will default to the decor view.
     */
    fun getContentRoot(of: View): ViewGroup {
        val decor = of.getRootView()
        val content = decor.findViewById<View?>(R.id.content)
        return (if (content != null) content else decor) as ViewGroup
    }

    /**
     * Is this view added to a view in the Activity?
     * // REVIEW some how make this work for additional windows. This is used because View.getGlobalVisibleRect() always returns true for views that
     * are detached for some reason. This method helps detect that case to avoid trusting its return value.
     *
     * @param view
     * @return true if this view is in the activity, false if it or its parents are detached from the activity views.
     */
    fun isAttachedToViewRoot(view: View): Boolean {
        if (view.getRootView() == null) {
            return false
        }
        val rootParent = getRootParent(view)
        if (rootParent == null) {
            return false
        }
        val activity = ContextUtil.getActivity(view)
        if (activity == null) {
            return false
        }
        val window = activity.getWindow()
        if (window == null) {
            return false
        }
        val decor = window.getDecorView()
        if (decor == null) {
            return false
        }
        if (decor === rootParent) {
            return true
        } else if (decor.getParent() === rootParent) {
            return true
        } else {
            return false
        }
    }

    fun makeMeasureSpec(size: Int, max: Int): Int {
        if (size >= 0) {
            return View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        } else if (max > 0) {
            return View.MeasureSpec.makeMeasureSpec(max, View.MeasureSpec.AT_MOST)
        } else {
            return View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        }
    }

    fun disableFocusAndClicks(view: View?) {
        if (view == null) {
            return
        }

        view.setClickable(false)
        view.setFocusable(false)
        view.setFocusableInTouchMode(false)
    }

    fun getAllViews(view: View?, output: MutableList<View>?): MutableList<View> {
        var output = output ?: ArrayList()
        if (view != null) {
            output.add(view)
            if (view is ViewGroup) {
                val group = view
                val children = group.getChildCount()
                for (i in 0..<children) {
                    getAllViews(group.getChildAt(i), output)
                }
            }
        }
        return output
    }

    @Suppress("unused")
    fun logViewHierarchy(view: View?) {
        val hierarchy = getAllViews(view, null)
        for (v in hierarchy) {
            Log.v(
                "View Hierarchy",
                "W:" + v.getMeasuredWidth() + " H:" + v.getMeasuredHeight() + " L:" + v.getLeft() + " T:" + v.getTop() + " " + v
            )
        }
    }

    /**
     * Log motion events in a view hierarchy to help determine where it is going and what view is consuming it.
     * Do not use outside of debugging.
     * @param view
     */
    @Suppress("unused")
    fun debugTouch(view: View) {
        var listener: OnTouchListener? = null
        try {
            val m = view.javaClass.getMethod("getListenerInfo")
            m.setAccessible(true)
            val info = m.invoke(view)
            val f = info!!.javaClass.getDeclaredField("mOnTouchListener")
            f.setAccessible(true)
            listener = f.get(info) as OnTouchListener?
        } catch (ignored: Exception) {
        }
        view.setOnTouchListener { v: View?, event: MotionEvent? ->
            Logs.v("TouchDebug", event!!.action.toString() + " " + view)
            if (listener != null) {
                listener.onTouch(v, event)
            } else {
                false
            }
        }
        if (view is ViewGroup) {
            val group = view
            for (i in 0..<group.getChildCount()) {
                debugTouch(group.getChildAt(i))
            }
        }
    }
}
