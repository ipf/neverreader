package com.neverreader.util.android

import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior

class AccessibilityUtils {
    /**
     * Internally, [BottomSheetBehavior] expects that the [androidx.coordinatorlayout.widget.CoordinatorLayout] the Bottom Sheet View appears in will also be used to display the rest of the content on screen,
     * and contains logic to hide content behind it from accessibility tools while it is expanded.
     *
     * Because of the way that our [BottomSheetBehavior]'s have been implemented, as a discrete drop in Views in their own right, Views behind them are not excluded from
     * accessibility tools, such as Talkback. Thus Views underneath the drawer would be read aloud.
     *
     * To fix this, [BottomSheetHelper] does the same work that [BottomSheetBehavior] does internally, only using the parent of the [androidx.coordinatorlayout.widget.CoordinatorLayout] from which our Bottom Sheets
     * are composed.
     *
     * To use in a Bottom Sheet implementation *that does not itself host the rest of the screen content*, create an instance of BottomSheetHelper and call [updateAccessibilityState] within a
     * [BottomSheetBehavior.BottomSheetCallback] on the Bottom Sheet's [BottomSheetBehavior].
     */
    class BottomSheetHelper {
        private var importantForAccessibilityMap: MutableMap<View?, Int?>? = null

        /**
         * Updates the accessibility tools state of the parent view tree, based on the new state of the BottomSheetBehavior.
         * Adapted from [BottomSheetBehavior]'s setStateInternal method logic.
         */
        fun updateAccessibilityState(drawer: View, newState: Int, hideCollapsed: Boolean) {
            if (newState == BottomSheetBehavior.STATE_HALF_EXPANDED || newState == BottomSheetBehavior.STATE_EXPANDED ||
                (hideCollapsed && newState == BottomSheetBehavior.STATE_COLLAPSED)
            ) {
                updateImportantForAccessibility(drawer, true)
            } else {
                // update only on STATE_HIDDEN or STATE_COLLAPSED (when applicable), skip doing anything while dragging / settling
                if (newState == BottomSheetBehavior.STATE_HIDDEN || newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    updateImportantForAccessibility(drawer, false)
                }
            }
        }

        /**
         * This method is adapted from [BottomSheetBehavior]'s internal updateImportantForAccessibility method, which loops through all the Views in the Bottom Sheet's
         * parent, except for the Bottom Sheet itself, and marks them either IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, or back to their original state, depending on whether
         * the drawer is open / closed.
         */
        private fun updateImportantForAccessibility(drawer: View, hideFromAccessibility: Boolean) {
            val viewParent = drawer.parent
            if (viewParent is ViewGroup) {
                val childCount = viewParent.childCount
                if (hideFromAccessibility) {
                    if (importantForAccessibilityMap != null) {
                        return
                    }
                    importantForAccessibilityMap = HashMap<View?, Int?>(childCount)
                }
                for (i in 0..<childCount) {
                    val child = viewParent.getChildAt(i)
                    if (child !== drawer) {
                        if (!hideFromAccessibility) {
                            if (importantForAccessibilityMap != null && importantForAccessibilityMap!!.containsKey(
                                    child
                                )
                            ) {
                                ViewCompat.setImportantForAccessibility(
                                    child,
                                    importantForAccessibilityMap!![child]!!
                                )
                            }
                        } else {
                            importantForAccessibilityMap!![child] = child.importantForAccessibility
                            ViewCompat.setImportantForAccessibility(
                                child,
                                ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                            )
                        }
                    }
                }
                if (!hideFromAccessibility) {
                    importantForAccessibilityMap = null
                }
            }
        }
    }
}
