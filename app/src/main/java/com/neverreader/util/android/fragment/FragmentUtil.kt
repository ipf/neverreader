package com.neverreader.util.android.fragment

import android.app.Activity
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.util.android.fragment.FragmentUtil.addFragmentAsDialog

object FragmentUtil {
    /**
     * if the fragment is not attached to an activity, or if the activity is finishing, or if this
     * fragment is finishing itself.
     * @return
     */
    fun isDetachedOrFinishing(frag: Fragment?): Boolean {
        if (frag == null) {
            return false
        }
        val activity: Activity? = frag.getActivity()
        return activity == null || activity.isFinishing() || frag.isDetached() || frag.isRemoving()
    }

    /**
     * If this fragment is attached but the activity it is attached to is finishing.
     * **IMPORTANT** if the fragment is not attached, this will return false because it doesn't know for sure. Do not use this
     * as a null check for a fragment's activity. See [.isDetachedOrFinishing].
     *
     * @return
     */
    fun isFinishing(frag: Fragment): Boolean {
        return frag.getActivity() != null && frag.getActivity()!!.isFinishing()
    }

    /**
     * Adds a fragment to an Activity. Immediately executes pending transactions afterwards.
     *
     * @param frag
     * @param activity
     * @param contentView
     * @param tag
     * @param addToBackStack
     */
    @JvmOverloads
    fun addFragment(
        frag: Fragment,
        activity: FragmentActivity,
        contentView: Int,
        tag: String?,
        addToBackStack: Boolean,
        executeImmediately: Boolean = true
    ) {
        val manager = activity.getSupportFragmentManager()
        val transaction = manager.beginTransaction()

        transaction.add(contentView, frag, tag)
        if (addToBackStack) {
            transaction.addToBackStack(null) // We could provide a param for name here if anyone wants to use it.
        }
        transaction.commit()

        if (executeImmediately) {
            activity.getSupportFragmentManager().executePendingTransactions()
        }
    }

    /**
     * Same as [.addFragmentAsDialog] with a null tag.
     * @param frag
     * @param activity
     */
    @JvmOverloads
    fun addFragmentAsDialog(
        frag: DialogFragment?,
        activity: FragmentActivity,
        tag: String? = null,
        addToBackStack: Boolean = true,
        executeImmediately: Boolean = true
    ) {
        val manager = activity.getSupportFragmentManager()
        val transaction = manager.beginTransaction()

        if (addToBackStack) {
            transaction.addToBackStack(null) // We could provide a param for name here if anyone wants to use it.
        }
        frag!!.show(transaction, tag)

        if (executeImmediately) {
            activity.getSupportFragmentManager().executePendingTransactions()
        }
    }

    fun removeFragment(frag: Fragment, activity: FragmentActivity) {
        val manager = activity.getSupportFragmentManager()
        val transaction = manager.beginTransaction()
        transaction.remove(frag)
        transaction.commit()
    }

    fun getRootView(fragment: Fragment?): View? {
        val f = fragment ?: return null
        return if (f is AbsNeverReaderFragment) {
            f.viewRoot
        } else {
            f.view
        }
    }

    enum class FragmentLaunchMode {
        /**
         * Show as DialogFragment within current activity. Can use [addFragmentAsDialog].
         */
        DIALOG,

        /**
         * Start a new Activity with this fragment as the content view. This typically means there is a Activity of the same name as the fragment available.
         */
        ACTIVITY,

        /**
         * This is for special cases, where no matter what, it should be launched as a new activity. Once in the activity, if [FormFactor.showSecondaryScreensInDialogs] then have this fragment appear as a DialogFragment with a rainbow background in the activity.
         * If false then have it display as a normal content fragment.
         *
         *
         * **Two important suggestions for this mode:**
         *
         *  1. Use [AbsNeverReaderActivity.setContentFragment] to easily handle the standard UI/UX for these types of fragments.
         *  1. In your fragment's [Fragment.onActivityCreated], invoke [FragmentUtil.cancelOutsideTouch] to prevent the activity from finishing if they touch outside of the fragment
         *
         */
        ACTIVITY_DIALOG
    }
}
