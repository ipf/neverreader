package com.neverreader.ui.view.bottom

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.neverreader.ui.R
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.analytics.UiEntityableHelper
import com.neverreader.ui.util.NeverReaderUIViewUtil.runAfterNextLayoutOf
import com.neverreader.util.android.AccessibilityUtils.BottomSheetHelper
import com.neverreader.util.java.RangeF

/**
 * A bottom sheet styled for NeverReader.
 *
 *
 * Add your content view with [.setLayout] or declare it in xml with app:sheetLayout.
 * Your layout will be merged into a vertical LinearLayout.
 *
 *
 * A scrim for blacking out the content behind is available.
 * To enable it call [.setScrimAlpha].
 * To access it to modify or customize, use and [.getScrim].
 *
 *
 * Customize the bottom sheet behaviour with [.getBehavior]
 *
 *
 * Note: The internals of this view will lazily be inflated after the first call to [.expand] or [.collapse].
 * If you need it inflated before then, use [.inflate]. Subclasses can override and use [.onLazyInflated]
 * to do their setup.
 */
class BottomDrawer : CoordinatorLayout, UiEntityable {
    private val callbacks: MutableSet<BottomSheetBehavior.BottomSheetCallback> =
        HashSet<BottomSheetBehavior.BottomSheetCallback>()

    private var layoutId = 0
    protected var isInflated: Boolean = false
        private set

    private var scrim: View? = null
    protected var nav: View? = null
        private set
    protected var back: View? = null
        private set
    protected var title: TextView? = null
        private set
    private var content: ViewGroup? = null
    private var bottomSheetBehavior: AppBottomSheetBehavior<ViewGroup>? = null
    private var hideOnOutsideTouch = false
    private var scrimAlphaWhenHidden = 0f
    private var scrimAlphaWhenCollapsed = 0f
    private var scrimAlphaWhenExpanded = 0f

    protected val uiEntityable: UiEntityableHelper = UiEntityableHelper()

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        if (attrs != null) {
            uiEntityable.obtainStyledAttributes(getContext(), attrs)

            val a = getContext().obtainStyledAttributes(attrs, R.styleable.BottomDrawer)
            val layout = a.getResourceId(R.styleable.BottomDrawer_sheetLayout, 0)
            if (layout != 0) {
                setLayout(layout)
            }
            a.recycle()
            // TODO support behavior_hideable, behavior_peekHeight and other bottom sheet attrs directly on this view
            // TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout);
        }
    }

    /**
     * Ensure the view is inflated and fully ready to use.
     *
     * @param startState the initial [BottomSheetBehavior] state to set to.
     */
    fun inflate(startState: Int) {
        if (isInflated) return

        isInflated = true
        LayoutInflater.from(getContext()).inflate(R.layout.view_bottom_sheet, this, true)
        scrim = findViewById<View>(R.id.bottom_sheet_scrim)
        nav = findViewById<View>(R.id.bottom_sheet_nav)
        back = findViewById<View>(R.id.bottom_sheet_back)
        title = findViewById<TextView?>(R.id.bottom_sheet_title)
        content = findViewById<ViewGroup>(R.id.bottom_sheet_content)
        bottomSheetBehavior =
            BottomSheetBehavior.from<ViewGroup?>(content!!) as AppBottomSheetBehavior<ViewGroup>
        bottomSheetBehavior!!.setBottomSheetCallback(BottomSheetCallback())

        content!!.setClickable(true) // Prevent touches from passing through to content underneath
        scrim!!.setOnTouchListener(OnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN && hideOnOutsideTouch) {
                hide()
                true
            } else {
                false
            }
        })

        content!!.setBackground(BottomSheetBackgroundDrawable(getContext()))

        if (layoutId != 0) {
            LayoutInflater.from(getContext()).inflate(layoutId, content, true)
        }

        onLazyInflated()

        // Start hidden and animate into start state after layout.
        this.behavior.setState(BottomSheetBehavior.STATE_HIDDEN)
        runAfterNextLayoutOf(this, Runnable { this.behavior.setState(startState) })
    }

    /**
     * The view has been inflated, subclasses should initialize their views.
     */
    protected fun onLazyInflated() {}

    @Suppress("unused")
    protected fun getScrim(): View {
        return scrim!!
    }

    fun contentParent(): ViewGroup {
        return content!!
    }

    /**
     * Change the content container to match_parent height.
     * This is useful if your content view wants to be as large as possible
     * or if it contains a RecyclerView.
     */
    protected fun matchParentHeight() {
        val lp = content!!.getLayoutParams()
        lp.height = LayoutParams.MATCH_PARENT
        content!!.setLayoutParams(lp)
    }

    protected val behavior: AppBottomSheetBehavior<ViewGroup>
        /**
         * Note: Use [.addBottomSheetCallback] instead of setting a callback on this object.
         */
        get() = bottomSheetBehavior!!

    /**
     * Set the layout res to be inflated, if and when it is lazily inflated. If already inflated, it will be set immediately.
     */
    protected fun setLayout(layout: Int) {
        layoutId = layout
        if (this.isInflated) {
            LayoutInflater.from(getContext()).inflate(layout, content, true)
        }
    }

    /**
     * Set a view to use as the content view. Must be invoked after [.onLazyInflated].
     */
    protected fun setLayout(layout: View?) {
        content!!.addView(layout)
    }

    fun addBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback?) {
        callbacks.add(callback!!)
    }

    @Suppress("unused")
    fun removeBottomSheetCallback(callback: BottomSheetBehavior.BottomSheetCallback?) {
        callbacks.remove(callback)
    }

    fun setHideOnOutsideTouch(value: Boolean) {
        hideOnOutsideTouch = value
    }

    fun setScrimAlpha(whenHidden: Float, whenCollapsed: Float, whenExpanded: Float) {
        scrimAlphaWhenHidden = whenHidden
        scrimAlphaWhenCollapsed = whenCollapsed
        scrimAlphaWhenExpanded = whenExpanded
        invalidate()
    }

    fun collapse() {
        if (!this.isInflated) {
            inflate(BottomSheetBehavior.STATE_COLLAPSED)
        } else {
            bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_COLLAPSED)
        }
    }

    fun hide() {
        if (!this.isInflated) return
        bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_HIDDEN)
    }

    fun expand() {
        if (!this.isInflated) {
            inflate(BottomSheetBehavior.STATE_EXPANDED)
        } else {
            bottomSheetBehavior!!.setState(BottomSheetBehavior.STATE_EXPANDED)
        }
    }

    private fun applyBottomSheetOffset(slideOffset: Float) {
        val alpha: Float
        if (slideOffset <= 0) {
            // Between hidden and collapsed
            alpha = RangeF.valueOf(
                scrimAlphaWhenHidden,
                scrimAlphaWhenCollapsed,
                slideOffset + 1,
                RangeF.Constrain.BOTH
            )
        } else {
            // Between collapsed and expanded
            alpha = RangeF.valueOf(
                scrimAlphaWhenCollapsed,
                scrimAlphaWhenExpanded,
                slideOffset,
                RangeF.Constrain.BOTH
            )
        }
        scrim!!.setVisibility(if (alpha > 0) VISIBLE else GONE)
        scrim!!.getBackground().setAlpha((alpha * 255).toInt())
    }

    val isExpanded: Boolean
        get() = isInflated && bottomSheetBehavior!!.state == BottomSheetBehavior.STATE_EXPANDED

    val isOpen: Boolean
        get() = isInflated && bottomSheetBehavior!!.state != BottomSheetBehavior.STATE_HIDDEN

    /**
     * Whether or not to hide the parent view tree from accessibility tools, such as Talkback,
     * while in the collapsed or expanded states.
     * @return true to hide the parent view tree from accessibility tools when collapsed or expanded.
     */
    protected fun hideParentFromAccessibility(): Boolean {
        return true
    }

    override var uiEntityIdentifier: String?
        get() = uiEntityable.uiEntityIdentifier
        set(uiEntityIdentifier) {
            uiEntityable.uiEntityIdentifier = uiEntityIdentifier
        }

    override val uiEntityType: UiEntityable.Type?
        get() = uiEntityable.uiEntityType

    override var uiEntityComponentDetail: String?
        get() = uiEntityable.uiEntityComponentDetail
        set(value) {
            uiEntityable.uiEntityComponentDetail = value
        }

    override val uiEntityLabel: String?
        get() = uiEntityable.uiEntityLabel

    private inner class BottomSheetCallback : BottomSheetBehavior.BottomSheetCallback() {
        private val bottomSheetAccessibilityHelper =
            if (hideParentFromAccessibility()) BottomSheetHelper() else null

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            when (newState) {
                BottomSheetBehavior.STATE_COLLAPSED -> applyBottomSheetOffset(0f)
                BottomSheetBehavior.STATE_HIDDEN -> applyBottomSheetOffset(-1f)
                BottomSheetBehavior.STATE_EXPANDED -> applyBottomSheetOffset(1f)
                BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_HALF_EXPANDED, BottomSheetBehavior.STATE_SETTLING -> {}
            }

            if (bottomSheetAccessibilityHelper != null) bottomSheetAccessibilityHelper.updateAccessibilityState(
                this@BottomDrawer,
                newState,
                true
            )

            for (callback in callbacks) {
                callback.onStateChanged(bottomSheet, newState)
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            if (slideOffset.isNaN()) {
                // Seems like maybe a support lib bug when near expanded state...?  Just ignore for now. The onStateChange dispatch should give us a final value.
                return
            }

            applyBottomSheetOffset(slideOffset)

            for (callback in callbacks) {
                callback.onSlide(bottomSheet, slideOffset)
            }
        }
    }

    fun showAsDialog() {
        val dialog = Dialog(getContext(), R.style.App_BottomDrawerDialog)
        addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(view: View, state: Int) {
                if (state == BottomSheetBehavior.STATE_HIDDEN) {
                    dialog.dismiss()
                }
            }

            override fun onSlide(view: View, v: kotlin.Float) {}
        })
        dialog.setOnKeyListener(DialogInterface.OnKeyListener { dialog1, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                hide()
                true
            } else {
                false
            }
        })
        dialog.setContentView(this)
        dialog.show()

        inflate(BottomSheetBehavior.STATE_EXPANDED)
    }
}
