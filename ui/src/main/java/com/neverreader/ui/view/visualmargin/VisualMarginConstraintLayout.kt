package com.neverreader.ui.view.visualmargin

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedConstraintLayout
import kotlin.math.max

/**
 * A ConstraintLayout that has tools for getting visually pixel perfect spacing between elements.
 *
 *
 * This mostly aids when trying to get an exact vertical spacing between a TextView and another element.
 * Android's TextView's bounding box includes space for the ascent and descent of text, but often
 * designers want the vertical space between elements to be measured from the baseline or ascent/top
 * of text. This is challenging to accomplish in xml without having to manually tweak each margin to
 * get it just right.
 *
 *
 * With this view, you can specify the visual margin you want between elements and this will calculate
 * it and adjust the position so the margin accounts for ascent or descent of text. For example:
 *
 * <TextView android:id="@+id/above" android:layout_width="wrap_content" android:layout_height></TextView>"wrap_content" />
 *
 * <TextView android:id="@+id/below" android:layout_width="wrap_content" android:layout_height></TextView>"wrap_content"
 * app:layout_constraintTop_toBottomOf="@+id/above"
 * app:visualMargin_top="@dimen/nr_space_md" />
 *
 * With a normal layout_marginTop, visually this would end up with more space between the two views as desired, it
 * would have nr_space_md plus the descent of the above view plus the ascent of the below view.
 *
 *
 * Using "visualMargin_top", this view will take into account the ascent and descent and layout
 * the two views so they visually appear exactly nr_space_md apart.
 *
 *
 * <h2>Implementation Notes/Limitations</h2>
 * Currently, this only supports layout_constraintTop_toBottomOf.
 *
 *
 * You must also use views that implement [VisualMargin]. [com.neverreader.ui.view.themed.ThemedTextView] supports
 * it, so if you are  already using that for text views, it is ready to use.
 *
 *
 * Also see docs on [VisualMargin] and its methods for some additional details.
 * <h2>Gone and Chains</h2>
 * If it is anchored to a view that is gone, it will calculate its margin onto the next non-gone anchor
 * in the chain, following layout_constraintTop_toBottomOf. You can also set a visualMargin_goneTop value
 * for a visual margin to use when all anchors above it in a chain are gone.
 */
open class VisualMarginConstraintLayout : ThemedConstraintLayout, VisualMargin {
    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Prepare anchors.
        // This is done before the first measure so any changes here are accounted for.
        // TODO  optimize? since this duplicates a bit of process that happens in the final step
        run {
            var i = 0
            val count = childCount
            while (i < count) {
                val child = getChildAt(i)
                if (child.visibility == GONE) {
                    i++
                    continue
                }
                if ((child.layoutParams as LayoutParams).visualMarginTop != 0) {
                    val anchor = resolveTopAnchorOf(child)
                    prepareAscent(child)
                    prepareDescent(anchor)
                }
                i++
            }
        }


        // Do a measure pass, so child views are measured and given position data
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)


        // Then adjust margins to be visual margins
        var changed = false
        var i = 0
        val count = childCount
        while (i < count) {
            val child = getChildAt(i)
            if (child.visibility == GONE) {
                i++
                continue
            }
            val lp = child.layoutParams as LayoutParams
            if ((child.layoutParams as LayoutParams).visualMarginTop != 0) {
                val anchor = resolveTopAnchorOf(child)
                changed = prepareAscent(child) || changed
                changed = prepareDescent(anchor) || changed
                var included: Int = calculateAscentOf(child)
                included += calculateDescentOf(anchor)
                val targetMargin =
                    (if (lp.visualMarginGoneTop != null && (anchor == null || anchor === this)) lp.visualMarginGoneTop else lp.visualMarginTop)!!
                val visualMargin = max(
                    0,
                    targetMargin - included
                ) // ConstraintLayout does not support negative margins.
                if (visualMargin != lp.topMargin) {
                    lp.topMargin = visualMargin
                    child.layoutParams = lp
                    changed = true
                }
            }
            i++
        }

        if (changed) {
            // Measure again with new margins
            requestLayout()
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun prepareAscent(view: View?): Boolean {
        return view is VisualMargin && (view as VisualMargin).prepareVisualAscent()
    }

    private fun prepareDescent(view: View?): Boolean {
        return view is VisualMargin && (view as VisualMargin).prepareVisualDescent()
    }

    private fun resolveTopAnchorOf(view: View): View? {
        val lp = view.getLayoutParams() as LayoutParams
        val anchor = findViewById<View>(lp.topToBottom)
        if (anchor == null) {
            return null
        } else if (anchor.visibility == VISIBLE || anchor.visibility == INVISIBLE) {
            return anchor
        } else {
            return resolveTopAnchorOf(anchor) // Check if there is one up in a chain.
        }
    }

    /**
     * @return visual space between this view's getTop() and its visual/content top
     */
    private fun calculateAscentOf(view: View?): Int {
        return if (view is VisualMargin && view.visibility == VISIBLE) (view as VisualMargin).visualAscent() else 0
    }

    /**
     * @return visual space between this view's getBottom() and its visual/content bottom
     */
    private fun calculateDescentOf(view: View?): Int {
        return if (view is VisualMargin && view.getVisibility() == VISIBLE) (view as VisualMargin).visualDescent() else 0
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

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return LayoutParams(getContext(), attrs)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): LayoutParams {
        return generateDefaultLayoutParams()
    }

    override fun prepareVisualAscent(): Boolean {
        return false
    }

    override fun prepareVisualDescent(): Boolean {
        return false
    }

    override fun visualAscent(): Int {
        return 0
    }

    override fun visualDescent(): Int {
        return 0
    }

    class LayoutParams : ConstraintLayout.LayoutParams {
        @JvmField
        var visualMarginTop: Int = 0
        var visualMarginGoneTop: Int? = null

        constructor(source: LayoutParams) : super(source) {
            this.visualMarginTop = source.visualMarginTop
            this.visualMarginGoneTop = source.visualMarginGoneTop
        }

        constructor(source: ViewGroup.LayoutParams?) : super(source)

        constructor(width: Int, height: Int) : super(width, height)

        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            if (attrs != null) {
                val a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.VisualMarginConstraintLayout_Layout
                )
                visualMarginTop = a.getDimensionPixelSize(
                    R.styleable.VisualMarginConstraintLayout_Layout_visualMargin_top,
                    0
                )
                if (a.hasValue(R.styleable.VisualMarginConstraintLayout_Layout_visualMargin_goneTop)) {
                    visualMarginGoneTop = a.getDimensionPixelSize(
                        R.styleable.VisualMarginConstraintLayout_Layout_visualMargin_goneTop,
                        0
                    )
                }
                a.recycle()
            }

            if (visualMarginTop > 0) {
                topMargin =
                    visualMarginTop // Start with the visual margin as a top margin, we'll likely need to reduce it, but this gets the layout close in the first pass.
            }
        }
    }
}
