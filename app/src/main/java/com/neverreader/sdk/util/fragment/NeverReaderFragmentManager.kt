package com.neverreader.sdk.util.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.neverreader.app.App
import com.neverreader.util.java.Logs
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.util.android.fragment.FragmentUtil
import java.io.FileDescriptor
import java.io.PrintWriter

class NeverReaderFragmentManager(
    /** The wrapped manager  */
    private val mFragmentManager: FragmentManager, private val mActivity: AbsNeverReaderActivity
) : FragmentManager(), FragmentManager.OnBackStackChangedListener {
    /**
     * All the back stack entries and their fragments.
     */
    private val mBackStackEntryFragments: java.util.ArrayList<BackStackEntryFragments> =
        java.util.ArrayList<BackStackEntryFragments>()

    /**
     * The back stack entry count after the last back stack entry change.
     */
    private var mLastKnownBackStackCount = 0

    init {
        mBackStackEntryFragments.add(BackStackEntryFragments())
        mFragmentManager.addOnBackStackChangedListener(this)
    }

    fun onSaveInstanceState(outState: Bundle) {
        val state = Bundle()


        // back stack entries
        val size = mBackStackEntryFragments.size
        state.putInt(STATE_BACK_STACK_ENTRY_COUNT, mBackStackEntryFragments.size)
        val source = mFragmentManager.fragments
        for (i in 0..<size) {
            val entry = mBackStackEntryFragments[i]
            state.putIntArray(
                STATE_BACK_STACK_ENTRY_ADDED_INDEXES + i,
                getListIndex(source, entry.added)
            )
            state.putIntArray(
                STATE_BACK_STACK_ENTRY_VISIBLE_INDEXES + i,
                getListIndex(source, entry.visible)
            )
        }

        outState.putBundle(STATE, state)
    }

    fun onRestoreInstanceState(inState: Bundle) {
        val state = inState.getBundle(STATE)
        val size = state!!.getInt(STATE_BACK_STACK_ENTRY_COUNT)

        if (size <= 0) {
            return  // Nothing to restore;
        }


        // All active fragments already restored by the default fragment manager
        val source = mFragmentManager.fragments

        for (i in 0..<size) {
            if (i > 0) { // There is already one at 0 index
                mBackStackEntryFragments.add(BackStackEntryFragments())
            }

            val entry = mBackStackEntryFragments[i]

            Companion.addAllFromIndex(
                source, entry.added, state.getIntArray(
                    STATE_BACK_STACK_ENTRY_ADDED_INDEXES + i
                )!!
            )
            Companion.addAllFromIndex(
                source, entry.visible, state.getIntArray(
                    STATE_BACK_STACK_ENTRY_VISIBLE_INDEXES + i
                )!!
            )
        }

        mLastKnownBackStackCount = size
    }

    override fun addOnBackStackChangedListener(listener: OnBackStackChangedListener) {
        mFragmentManager.addOnBackStackChangedListener(listener)
    }

    @SuppressLint("CommitTransaction")
    override fun beginTransaction(): FragmentTransaction {
        return NeverReaderFragmentTransaction(mFragmentManager.beginTransaction(), this)
    }

    override fun dump(
        prefix: String, fd: FileDescriptor?, writer: PrintWriter,
        args: Array<String?>?
    ) {
        mFragmentManager.dump(prefix, fd, writer, args)
    }

    override fun executePendingTransactions(): Boolean {
        return mFragmentManager.executePendingTransactions()
    }

    override fun findFragmentById(id: Int): Fragment? {
        return mFragmentManager.findFragmentById(id)
    }

    override fun findFragmentByTag(tag: String?): Fragment? {
        return mFragmentManager.findFragmentByTag(tag)
    }

    override fun getBackStackEntryAt(index: Int): BackStackEntry {
        return mFragmentManager.getBackStackEntryAt(index)
    }

    override fun getBackStackEntryCount(): Int {
        return mFragmentManager.backStackEntryCount
    }

    override fun getFragment(bundle: Bundle, key: String): Fragment? {
        return mFragmentManager.getFragment(bundle, key)
    }

    override fun getFragments(): MutableList<Fragment?> {
        val fragments = mFragmentManager.fragments
        return fragments
    }

    override fun popBackStack() {
        mFragmentManager.popBackStack()
    }

    override fun popBackStack(name: String?, flags: Int) {
        mFragmentManager.popBackStack(name, flags)
    }

    override fun popBackStack(id: Int, flags: Int) {
        mFragmentManager.popBackStack(id, flags)
    }

    override fun popBackStackImmediate(): Boolean {
        return mFragmentManager.popBackStackImmediate()
    }

    override fun popBackStackImmediate(name: String?, flags: Int): Boolean {
        return mFragmentManager.popBackStackImmediate(name, flags)
    }

    override fun popBackStackImmediate(id: Int, flags: Int): Boolean {
        return mFragmentManager.popBackStackImmediate(id, flags)
    }

    override fun putFragment(bundle: Bundle, key: String, fragment: Fragment) {
        mFragmentManager.putFragment(bundle, key, fragment)
    }

    override fun removeOnBackStackChangedListener(listener: OnBackStackChangedListener) {
        mFragmentManager.removeOnBackStackChangedListener(listener)
    }

    override fun saveFragmentInstanceState(fragment: Fragment): Fragment.SavedState? {
        return mFragmentManager.saveFragmentInstanceState(fragment)
    }

    override fun isDestroyed(): Boolean {
        return mFragmentManager.isDestroyed
    }

    override fun registerFragmentLifecycleCallbacks(
        cb: FragmentLifecycleCallbacks,
        recursive: Boolean
    ) {
        mFragmentManager.registerFragmentLifecycleCallbacks(cb, recursive)
    }

    override fun unregisterFragmentLifecycleCallbacks(cb: FragmentLifecycleCallbacks) {
        mFragmentManager.unregisterFragmentLifecycleCallbacks(cb)
    }

    override fun isStateSaved(): Boolean {
        return mFragmentManager.isStateSaved
    }

    override fun getPrimaryNavigationFragment(): Fragment? {
        return mFragmentManager.primaryNavigationFragment
    }

    val visibleFragments: ArrayList<Fragment?>
        /**
         * Returns the currently visible/active fragments. This may contain fragments added during multiple back stack entries.
         *
         *
         * Note: Do not modify this list.
         */
        get() = this.currentBackStackEntry.visible

    fun onCommit(
        added: java.util.ArrayList<Fragment?>,
        removed: java.util.ArrayList<Fragment?>,
        addedToBackStack: Boolean
    ) {
        val visible: MutableList<Fragment?> = java.util.ArrayList<Fragment?>(
            this.visibleFragments
        ) // Copy so we can modify
        visible.removeAll(removed)
        visible.addAll(added)


        // Keep track of back stack state
        if (addedToBackStack) {
            // Dispatch loss of focus to current fragments
            val currentFocus = this.currentBackStackEntry

            if (currentFocus.visible.isEmpty()) {
                mActivity.onLostFocus()
            }

            for (fragment in currentFocus.added) {
                if (fragment is AbsNeverReaderFragment) {
                    fragment.onLostFocus()
                }
            }


            // Create a new back stack entry
            val entry = BackStackEntryFragments()
            entry.added.addAll(added)
            entry.visible.clear()
            entry.visible.addAll(visible)
            mBackStackEntryFragments.add(entry)
        } else {
            // Add fragments to current back stack entry
            val entry = this.currentBackStackEntry
            entry.added.addAll(added)
            entry.added.removeAll(removed.toSet())

            entry.visible.clear()
            entry.visible.addAll(visible)
        }
    }

    override fun onBackStackChanged() {
        val newCount = mFragmentManager.backStackEntryCount

        if (newCount < mLastKnownBackStackCount) {
            // Need to update our references to remove these entries
            val entriesToRemove = mLastKnownBackStackCount - newCount
            for (i in 0..<entriesToRemove) {
                // Pop off the last entry
                mBackStackEntryFragments.removeAt(mBackStackEntryFragments.size - 1)
            }

            if (mBackStackEntryFragments.isEmpty()) {
                // Ensure at least one entry
                mBackStackEntryFragments.add(BackStackEntryFragments())


                // We are seeing this case in production crash logs. It is probably fine and just
                // an Activity finishing, but just to make sure we understand why and where this is
                // happening, report the error with the name of the activity so we can get more information.
                // If it turns out it is fine to do this way, then we can remove this reporting code.
                try {
                    val activity = if (App.activityContext != null) App.activityContext
                        .toString() else ""
                    throw RuntimeException("empty back stack at $activity")
                } catch (e: RuntimeException) {
                    if (mActivity.app()?.mode() == com.neverreader.app.AppMode.DEV) { // REVIEW is there a problem here?
                        throw e
                    } else {
                        Logs.i("NeverReaderFragmentManager", "empty back stack")
                    }
                }
            }

            val currentFocus = this.currentBackStackEntry

            if (currentFocus.visible.isEmpty()) {
                mActivity.onRegainedFocus()
            }

            for (fragment in currentFocus.added) {
                if (fragment is AbsNeverReaderFragment) {
                    fragment.onRegainedFocus()
                }
            }
        }

        mLastKnownBackStackCount = newCount
    }

    private val currentBackStackEntry: BackStackEntryFragments
        get() = mBackStackEntryFragments[mBackStackEntryFragments.size - 1] // Will never be empty because of the one added during the constructor and the way the last stack is removed.

    /**
     * Is this fragment one of the root fragments?
     */
    private fun isRootFragment(fragment: Fragment?): Boolean {
        return mBackStackEntryFragments.get(0).added.contains(fragment)
    }

    /**
     * Invoked during [Activity.onBackPressed]
     * @return true if handled by a fragment, false otherwise
     */
    fun onBackPressed(): Boolean {
        val fragments = this.currentBackStackEntry.added

        val size = fragments.size
        for (i in size - 1 downTo 0) {
            val fragment = fragments[i]
            if (fragment is AbsNeverReaderFragment) {
                val handled = fragment.onBackPressed()
                if (handled) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Invoked during [Activity.onRestart]
     */
    fun onActivityRestart() {
        val fragments = this.currentBackStackEntry.added
        for (fragment in fragments) {
            if (fragment is AbsNeverReaderFragment) {
                fragment.onRestart()
            }
        }
    }

    /**
     * Invoked during [AbsNeverReaderActivity.onThemeChanged]
     */
    fun onThemeChanged(newTheme: Int) {
        val fragments = this.currentBackStackEntry.added
        for (fragment in fragments) {
            if (fragment is AbsNeverReaderFragment) {
                fragment.onThemeChanged(newTheme)
            }
        }
    }

    /**
     * Dismiss/hide this fragment. If it is a root fragment, it will finish the Activity.
     */
    fun finishFragment(fragment: Fragment?, parentActivity: FragmentActivity) {
        if (fragment is DialogFragment && fragment.showsDialog) {
            fragment.dialog!!.dismiss()
        } else if (isRootFragment(fragment)) {
            parentActivity.finish()
        } else {
            // REVIEW this won't pop the back stack... is that a problem?
            FragmentUtil.removeFragment(fragment!!, parentActivity)
        }
    }

    /**
     * Removes all fragments from the Activity.
     */
    fun removeAllFragments() {
        val frags = mFragmentManager.fragments

        val ft = beginTransaction()
        for (frag in frags) {
            if (frag.isVisible) {
                ft.remove(frag)
            }
        }
        ft.commit()

        executePendingTransactions()
    }

    private inner class BackStackEntryFragments {
        /**
         * The fragments added during this transaction/back stack entry.
         */
        var added: java.util.ArrayList<Fragment?> = java.util.ArrayList<Fragment?>()

        /**
         * A copy of what [NeverReaderFragmentManager.visibleFragments] is while this entry is the current state.
         */
        var visible: java.util.ArrayList<Fragment?> = java.util.ArrayList<Fragment?>()
    }

    companion object {
        private const val STATE = "NeverReaderFragmentManagerState"
        private const val STATE_BACK_STACK_ENTRY_COUNT = "backStackEntryCount"
        private const val STATE_BACK_STACK_ENTRY_ADDED_INDEXES = "backStackEntryAdds"
        private const val STATE_BACK_STACK_ENTRY_VISIBLE_INDEXES = "backStackEntryVisibles"

        /**
         * Maps each object of matchList to its index within sourceList.
         * Assumes all of the fragments of matchList can be found in sourceList.
         *
         *
         * For example, if sourceList is ["cat", "dog", "rabbit"] and matchList is ["rabbit", "cat"], this will return [2,0]
         *
         *
         * You can then rebuild the contents of matchList later by extracting them out of sourceList with [.addAllFromIndex]
         */
        private fun getListIndex(
            sourceList: MutableList<Fragment>,
            matchList: MutableList<Fragment?>
        ): IntArray {
            val indexes = IntArray(matchList.size)
            val len = indexes.size
            for (i in 0..<len) {
                indexes[i] = sourceList.indexOf(matchList[i])
            }
            return indexes
        }

        /**
         * Used in conjunction with [.getListIndex] to restore references to fragments when restoring state.
         */
        private fun addAllFromIndex(
            sourceList: MutableList<Fragment>,
            destList: MutableList<Fragment?>,
            indexes: IntArray
        ) {
            for (i in indexes) {
                if (i < 0) {
                    // In certain cases where an activity is restored, such as editing an avatar with "Don't Keep Activities" on,
                    // this can happen. It isn't very clear why. However, we've been doing the above logging for years,
                    // and failing silently on Production. No user has reported issues here so problem not worth the effort to
                    // figure this out. We'll just skip restoring the reference here.
                    continue
                }
                destList.add(sourceList[i])
            }
        }
    }
}
