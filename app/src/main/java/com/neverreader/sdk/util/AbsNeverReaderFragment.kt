package com.neverreader.sdk.util

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import com.neverreader.app.App
import com.neverreader.app.App.Companion.from
import com.neverreader.app.MainActivity
import com.neverreader.app.settings.Theme
import com.neverreader.app.settings.isDarkAppTheme
import com.neverreader.sdk.util.fragment.NeverReaderFragmentManager
import com.neverreader.util.android.FormFactor
import com.neverreader.util.android.ViewUtil.refreshDrawableStateDeep
import com.neverreader.util.android.view.DialogSizeWrapper
import com.neverreader.util.java.Logs

/**
 * A base Fragment class that handles showPage life cycles. Any fragments that
 * opened from a showPage() call or that want to showPages should subclass this.
 *
 * Extends DialogFragment instead of Fragment so that subclasses can implement
 * themselves as dialogs or not.
 *
 * @author max
 */
abstract class AbsNeverReaderFragment : AppCompatDialogFragment() {
    private var mApp: App? = null

    /**
     * If this fragment is displayed as a dialog, this is the root view.
     * @see AbsNeverReaderFragment.viewRoot
     * @see .mRootView
     */
    private var mDialogRootView: View? = null

    /**
     * The view returned by [.onCreateViewImpl]
     * @see AbsNeverReaderFragment.viewRoot
     * @see .mDialogRootView
     */
    private var mRootView: View? = null

    private var mOnDestroyListeners: ArrayList<OnFragmentDestroyListener>? =
        null // REVIEW convert to use the new OnFragmentLifeCycleChangedListener

    /**
     * @throws IllegalStateException if a fragment hasn't been attached to a context
     */
    protected fun app(): App {
        checkNotNull(mApp) { "Fragment $this hasn't been attached to a context yet." }

        return mApp!!
    }

    /**
     * @throws IllegalStateException if fragment hasn't been attached to a context
     */
    val screenIdentifier: String?
        /** Returns an identifier for the screen represented by this fragment.  */
        get() = null

    val screenIdentifierString: String?
        get() = null

    /**
     * Use instead of [.onCreateView].
     * This method should pretty much be limited to inflating and creating your view.
     * Do bindings, findViewById and other setup in [.onViewCreatedImpl]
     */
    protected abstract fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?

    /**
     * Use instead of [.onViewCreated].
     */
    protected open fun onViewCreatedImpl(view: View, savedInstanceState: Bundle?) {
        if (this.screenIdentifierString != null) {
        } else if (this.screenIdentifier != null) {
        }
    }

    override fun onAttach(activity: Context) {
        super.onAttach(activity)


        // Check if Activity is the correct type.
        if (activity !is AbsNeverReaderActivity) {
            Logs.throwIfNotProduction("AbsNeverReaderFragment requires the parent Activity to be a AbsNeverReaderActivity in order to use the additional functionality and APIs")
        }

        mApp = from(activity)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (showsDialog) {
            return null // View will be added to the dialog in onCreateDialog instead.
        }

        mRootView = onCreateViewImpl(inflater, container, savedInstanceState)
        return mRootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewCreatedImpl(view, savedInstanceState)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // If the fragment is shown as a DialogFragment, this is the replacement to onCreateView.

        setStyle(STYLE_NO_TITLE, 0)

        var wrapper: DialogSizeWrapper? = null
        if (FormFactor.showSecondaryScreensInDialogs(activity!!)) {
            wrapper = DialogSizeWrapper(activity!!)
        }

        mRootView =
            onCreateViewImpl(LayoutInflater.from(activity), wrapper, savedInstanceState)
        onViewCreatedImpl(mRootView!!, savedInstanceState)

        if (wrapper != null) {
            wrapper.addView(mRootView)
            mDialogRootView = wrapper
        } else {
            mDialogRootView = mRootView
        }

        val builder = AlertDialog.Builder(activity)
        builder.setView(mDialogRootView)
        val dialog = builder.create()
        dialog.setOnKeyListener(object : DialogInterface.OnKeyListener {
            /**
             * BUG it seems like in this case, onKey gets called twice on BACK.
             * Might be a bug in android, needs more investigation, but for now here is a cheap
             * workaround.
             *
             * We will ignore the first back button call and only respond to the second.
             */
            private var backPressed = false

            override fun onKey(dialog: DialogInterface?, keyCode: Int, event: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (backPressed) {
                        // Second one, react to this one.
                        backPressed = false // Reset
                        return onBackPressed()
                    } else {
                        // First one, ignore this one.
                        backPressed = true
                        return false
                    }
                }


                // Workaround for a hard search button (on older devices) closing open dialogs.
                if (keyCode == KeyEvent.KEYCODE_SEARCH) {
                    return true // Prevent search from closing the dialog. This is ok because we don't use Activity.onSearchRequested for anything.
                }

                return false
            }
        })
        dialog.window!!.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT
        )
        dialog.window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        setupDialogWithNoTopSpace(mDialogRootView!!)

        return dialog
    }

    override fun onStart() {
        super.onStart()

        // Set root view as clickable to avoid touch events passing down to views below this fragment
        if (this.viewRoot != null) {
            this.viewRoot!!.isClickable = true
        }

        if (showsDialog) {
            // HACK to make dialog fragments appear correctly
            val parent = mDialogRootView!!.parent as ViewGroup
            parent.setPadding(0, 0, 0, 0)
        }
    }

    /**
     * Invoked during the parent Activity's Activity#onRestart() callback.
     */
    fun onRestart() {}

    /**
     * The opposite of [.onLostFocus]. A callback to inform the fragment that it is likely users direct focus again.
     * Invoked when a [androidx.fragment.app.FragmentTransaction] pops the back stack and this fragment is once again at the top of the stack.
     *
     *
     * A common example is if another [androidx.fragment.app.DialogFragment] is added above this fragment. This fragment is still partially
     * visible, but the DialogFragment is now the "focus". Then that dialog is dismissed. That is when this fragment will regain "focus".
     *
     * @see .onRegainedFocus
     */
    fun onRegainedFocus() {}

    /**
     * A callback to inform the fragment that it is likely no longer the user's direct focus. Invoked when a [androidx.fragment.app.FragmentTransaction] adds something to the back stack.
     *
     *
     * A common example is if another [androidx.fragment.app.DialogFragment] is added above this fragment. This fragment is still partially
     * visible, but the DialogFragment is now the "focus".
     *
     * @see .onLostFocus
     */
    fun onLostFocus() {}

    /**
     * Dismiss/hide this fragment. If it is a root fragment, it will finish the Activity.
     */
    fun finish() {
        val activity = activity as? AbsNeverReaderActivity? ?: return

        (activity.supportFragmentManager as NeverReaderFragmentManager)
            .finishFragment(this, getActivity()!!)
    }

    /**
     * The app's theme (dark/light mode) has changed.
     */
    fun onThemeChanged(newTheme: Int) {
        refreshDrawableStateDeep(this.viewRoot)
    }

    /** See [isDarkAppTheme]. */
    protected fun isDarkTheme(): Boolean = isDarkAppTheme(activity)

    val absNeverReaderActivity: AbsNeverReaderActivity?
        /**
         * A convenience for returning the Fragment's activity cast to
         * [AbsNeverReaderActivity]
         */
        get() = activity as AbsNeverReaderActivity?

    val themeFlag: Int
        /**
         * Similar to [AbsNeverReaderActivity.defaultThemeFlag] in that it declares what themes are supported
         * by this fragment. By default, it will be whatever the activity supports. If your fragment is more limited,
         * override this to declare your allowed themes.
         */
        get() {
            val activity = this.absNeverReaderActivity
            return activity?.themeFlag ?: Theme.FLAG_ALLOW_ALL
        }

    /**
     * Called when the user presses the device's back button.
     * @return true if handled, false if not
     */
    fun onBackPressed(): Boolean {
        return false
    }

    /**
     * Only used for fragments that use [com.neverreader.util.BackPressedUtil], which is,
     * only used in [MainActivity] at the moment
     * @return true if this fragment needs to intercept onBackPressed from a child fragment
     */
    fun onInterceptBackPressed(): Boolean {
        return false
    }

    /**
     * Convenience method for finding a view within this fragment.
     * @param res
     * @return
     */
    fun <T : View?> findViewById(res: Int): T? {
        return this.viewRoot?.findViewById<T?>(res)
    }

    val viewRoot: View?
        /**
         * Returns the actual view returned by [.onCreateViewImpl], or in the case of being displayed as a dialog, possibly the wrapper of that view.
         * @return
         */
        get() {
            return if (mDialogRootView != null) {
                mDialogRootView
            } else {
                mRootView // Seems like the compatibility library wraps the view returned by onCreateView and returns that parent in super.getView() instead of our actual view. So we just maintain a reference ourselves.
            }
        }

    override fun onCancel(dialog: DialogInterface) {
        if (showsDialog) {
            finish()
        }
        super.onCancel(dialog)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mOnDestroyListeners != null) {
            for (listener in mOnDestroyListeners) {
                listener.onFragmentDestroy(this)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mRootView = null
        mDialogRootView = null
    }

    interface OnFragmentDestroyListener {
        fun onFragmentDestroy(fragment: Fragment?)
    }


    companion object {
        /**
         * Show a Dialog with the extra title/top padding collapsed.
         *
         * @param customView The custom view that you added to the dialog
         */
        fun setupDialogWithNoTopSpace(customView: View) {
            // Now we setup a listener to detect as soon as the dialog has shown.
            customView.viewTreeObserver
                .addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        // Check if your view has been laid out yet
                        if (customView.height > 0) {
                            // If it has been, we will search the view hierarchy for the view that is responsible for the extra space.
                            val dialogLayout: LinearLayout? = findDialogLinearLayout(customView)
                            if (dialogLayout == null) {
                                // Could find it. Unexpected.
                                // OPT report
                            } else {
                                // Found it, now remove the height of the title area
                                val child = dialogLayout.getChildAt(0)
                                if (child !== customView) {
                                    // remove height
                                    val lp = child.layoutParams as LinearLayout.LayoutParams
                                    lp.height = 0
                                    child.layoutParams = lp
                                } else {
                                    // Could find it. Unexpected.
                                    // OPT report
                                }
                            }


                            // Done with the listener
                            customView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                        }
                    }
                })
        }

        /**
         * Searches parents for a LinearLayout
         *
         * @param view to search the search from
         * @return the first parent view that is a LinearLayout or null if none was found
         */
        fun findDialogLinearLayout(view: View): LinearLayout? {
            val parent = view.parent
            if (parent != null) {
                if (parent is LinearLayout) {
                    // Found it
                    return parent
                } else if (parent is View) {
                    // Keep looking
                    return findDialogLinearLayout(parent as View)
                }
            }


            // Couldn't find it
            return null
        }
    }
}
