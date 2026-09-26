package com.neverreader.sdk.util.fragment

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

class NeverReaderFragmentTransaction(
    private val mTransaction: FragmentTransaction,
    private val mNeverReaderFragmentManager: NeverReaderFragmentManager
) : FragmentTransaction() {
    private val mAdded = ArrayList<Fragment?>()
    private val mRemoved = ArrayList<Fragment?>()

    private var mIsAddedToBackStack = false

    override fun add(fragment: Fragment, tag: String?): FragmentTransaction {
        mTransaction.add(fragment, tag)
        mAdded.add(fragment)

        return this
    }

    override fun add(containerViewId: Int, fragment: Fragment): FragmentTransaction {
        return add(containerViewId, fragment, null)
    }

    override fun add(containerViewId: Int, fragment: Fragment, tag: String?): FragmentTransaction {
        mTransaction.add(containerViewId, fragment, tag)
        mAdded.add(fragment)

        return this
    }

    override fun replace(containerViewId: Int, fragment: Fragment): FragmentTransaction {
        return replace(containerViewId, fragment, null)
    }

    override fun replace(
        containerViewId: Int,
        fragment: Fragment,
        tag: String?
    ): FragmentTransaction {
        val currentFragment = mNeverReaderFragmentManager.findFragmentById(containerViewId)

        mTransaction.replace(containerViewId, fragment, tag)

        if (currentFragment != null) {
            mRemoved.add(currentFragment)
        }
        mAdded.add(fragment)
        return this
    }

    override fun show(fragment: Fragment): FragmentTransaction {
        mTransaction.show(fragment)

        mAdded.add(fragment)
        return this
    }

    override fun addToBackStack(name: String?): FragmentTransaction {
        mTransaction.addToBackStack(name)

        mIsAddedToBackStack = true
        return this
    }

    override fun attach(fragment: Fragment): FragmentTransaction {
        mTransaction.attach(fragment)

        mAdded.add(fragment)
        return this
    }

    override fun setPrimaryNavigationFragment(fragment: Fragment?): FragmentTransaction {
        return mTransaction.setPrimaryNavigationFragment(fragment)
    }

    override fun detach(fragment: Fragment): FragmentTransaction {
        mTransaction.detach(fragment)

        mRemoved.add(fragment)
        return this
    }

    override fun remove(fragment: Fragment): FragmentTransaction {
        mTransaction.remove(fragment)

        mRemoved.add(fragment)
        return this
    }

    override fun commit(): Int {
        mNeverReaderFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack)
        return mTransaction.commit()
    }

    override fun commitAllowingStateLoss(): Int {
        mNeverReaderFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack)
        return mTransaction.commitAllowingStateLoss()
    }

    override fun commitNowAllowingStateLoss() {
        mNeverReaderFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack)
        mTransaction.commitNowAllowingStateLoss()
    }

    override fun commitNow() {
        mNeverReaderFragmentManager.onCommit(mAdded, mRemoved, mIsAddedToBackStack)
        mTransaction.commitNow()
    }

    override fun disallowAddToBackStack(): FragmentTransaction {
        mTransaction.disallowAddToBackStack()

        return this
    }

    override fun hide(fragment: Fragment): FragmentTransaction {
        mTransaction.hide(fragment)

        mRemoved.add(fragment)
        return this
    }

    override fun isAddToBackStackAllowed(): Boolean {
        return mTransaction.isAddToBackStackAllowed
    }

    override fun isEmpty(): Boolean {
        return mTransaction.isEmpty
    }

    @Deprecated("Deprecated in Java")
    override fun setBreadCrumbShortTitle(res: Int): FragmentTransaction {
        mTransaction.setBreadCrumbShortTitle(res)

        return this
    }

    @Deprecated("Deprecated in Java")
    override fun setBreadCrumbShortTitle(text: CharSequence?): FragmentTransaction {
        mTransaction.setBreadCrumbShortTitle(text)

        return this
    }

    override fun setReorderingAllowed(b: Boolean): FragmentTransaction {
        return mTransaction.setReorderingAllowed(b)
    }

    @Deprecated("Deprecated in Java")
    override fun setAllowOptimization(allowOptimization: Boolean): FragmentTransaction {
        mTransaction.setAllowOptimization(allowOptimization)

        return this
    }

    @Deprecated("Deprecated in Java")
    override fun setBreadCrumbTitle(res: Int): FragmentTransaction {
        mTransaction.setBreadCrumbTitle(res)

        return this
    }

    @Deprecated("Deprecated in Java")
    override fun setBreadCrumbTitle(text: CharSequence?): FragmentTransaction {
        mTransaction.setBreadCrumbTitle(text)

        return this
    }

    override fun setCustomAnimations(enter: Int, exit: Int): FragmentTransaction {
        mTransaction.setCustomAnimations(enter, exit)

        return this
    }

    override fun setCustomAnimations(
        enter: Int,
        exit: Int,
        popEnter: Int,
        popExit: Int
    ): FragmentTransaction {
        mTransaction.setCustomAnimations(enter, exit, popEnter, popExit)

        return this
    }

    override fun addSharedElement(view: View, s: String): FragmentTransaction {
        mTransaction.addSharedElement(view, s)
        return this
    }

    override fun setTransition(transit: Int): FragmentTransaction {
        return mTransaction.setTransition(transit)
    }

    @Deprecated("Deprecated in Java")
    override fun setTransitionStyle(styleRes: Int): FragmentTransaction {
        mTransaction.setTransitionStyle(styleRes)

        return this
    }

    override fun runOnCommit(runnable: Runnable): FragmentTransaction {
        mTransaction.runOnCommit(runnable)
        return this
    }
}
