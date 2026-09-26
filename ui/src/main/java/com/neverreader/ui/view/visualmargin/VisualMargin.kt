package com.neverreader.ui.view.visualmargin

import android.view.View
import android.view.ViewGroup.MarginLayoutParams

/**
 * A view that supports visual margins in [VisualMarginConstraintLayout].
 */
interface VisualMargin {
    /**
     * @return The number of pixels the visual top of your view is from the bounding box top.
     */
    fun visualAscent(): Int

    /**
     * @return The number of pixels the visual bottom of your view is from the bounding box bottom.
     */
    fun visualDescent(): Int

    /**
     * This will be used as the bottom view of a visual margin calculation,
     * and will later have [.visualAscent] invoked.
     * Most views don't need to do anything to prepare.
     * This is provided for views that by default have some padding or margin along their
     * top, but when used as a visual margin anchor are happy to discard it in favor of
     * the visual margin that the parent view wants to use between this view and the one above it.
     * @return true if layout changes were made, false if not
     */
    fun prepareVisualAscent(): Boolean

    /**
     * This will be used as the top view of a visual margin calculation,
     * and will later have [.visualDescent] invoked.
     * Most views don't need to do anything to prepare.
     * This is provided for views that by default have some padding or margin along their
     * bottom, but when used as a visual margin anchor are happy to discard it in favor of
     * the visual margin that the parent view wants to use between this view and the one below it.
     * @return true if layout changes were made, false if not
     */
    fun prepareVisualDescent(): Boolean

    companion object {
        /**
         * Helper for setting a top margin to 0.
         * @return true if changes were made
         */
        fun removeTopMargin(view: View): Boolean {
            val lp = view.layoutParams as MarginLayoutParams
            if (lp.topMargin != 0) {
                lp.topMargin = 0
                if (lp is VisualMarginConstraintLayout.LayoutParams) {
                    lp.visualMarginTop = 0
                }
                view.layoutParams = lp
                return true
            }
            return false
        }
    }
}
