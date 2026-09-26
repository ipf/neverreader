package com.neverreader.sdk.util.dialog

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.neverreader.app.App
import com.neverreader.app.App.Companion.getStringResource
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.util.android.fragment.FragmentUtil.addFragmentAsDialog

/**
 * More helper methods on top of RilDialogFragment.
 *
 * Extend this class for easier DialogFragment creation. To extend this class:
 *
 * Create a static method that creates a new instance of your class. Such as getNew(). In this method init your class such as:
 *
 * MyDialogFragment frag = new MyDialogFragment();
 *
 * With no params, just an empty constructor.
 *
 * Then in getNew() call a variant of frag.createArgs() with your title and message.
 *
 * If you want your dialog to only be able to have one visible instance at once, look at extending  onlyAllowOneInstanceAtATime(), isShowingInstance() and setShowingInstance().
 *
 *
 * If your dialog needs more than just a title and message, create a setter for the value and override onCreateArgs. During onCreateArgs,
 * add the set value to the supplied arguments.
 *
 * A completed example:
 *
 * public static MyDialogFragment getNew() {
 * MyDialogFragment frag = new MyDialogFragment();
 * frag.setSomeValue("A value");
 * frag.createArgs("title", "message");
 * }
 *
 * private String mSomeValue;
 *
 * public void setSomeValue(String value) {
 * mSomeValue = value;
 * }
 *
 * @ Override
 * protected Bundle onCreateArgs(Bundle args) {
 * args.putString(SOME_VALUES_KEY, mSomeValue);
 * }
 *
 * Then, to build your dialog, override onCreateDialog(Bundle savedInstanceState).
 *
 * For more built-in functionality look at AlertMessaging
 *
 * @author max
 */
abstract class ExtendedDialogFragment : RilDialogFragment() {
    /**
     * Show the dialog on whatever RilAppActivity is currently on screen. If there is no activity on screen, it will not show.
     * OPT allows it showing on the next activity launch if it occurs within the next second.
     * OPT this shouldn't be used very often. We should know what activity it should appear in, in most cases.
     */
    fun showOnCurrentActivity() {
        show(null)
    }

    /**
     * Will show the dialog fragment on the activity, unless the activity is null or finishing, then it will try to show it on the current active RilAppActivity if any.
     * @param activity
     */
    fun show(activity: FragmentActivity?) {
        if (onlyAllowOneInstanceAtATime() && this.isShowingInstance) return

        if (activity != null && !activity.isFinishing) {
            this.isShowingInstance = true
            addFragmentAsDialog(this, activity, tag,
                addToBackStack = false,
                executeImmediately = false
            )
        } else {
            val currentActivity: AbsNeverReaderActivity = App.activityContext ?: return

            currentActivity.runOnUiThread {
                this.isShowingInstance = true
                addFragmentAsDialog(this, currentActivity, tag,
                    addToBackStack = false,
                    executeImmediately = false
                )
            }
        }
    }

    /**
     * Whether this class of dialog should be able to have multiples shown to the user at the same time.
     * Use this when the dialog might be opened from multiple places at once, such as database or file errors.
     * This will prevent a flood of dialogs from being opened.
     *
     * @return false to allow multiples, true to ignore show attempts while one is already open.
     */
    protected fun onlyAllowOneInstanceAtATime(): Boolean {
        return false
    }

    /**
     * Whether or not an instance of this dialog is currently showing. Subclasses should maintain a static flag that is toggled by setShowingInstance().
     * @return the dialogs static flag, set by setShowingInstance()
     */
    /**
     * Subclasses should maintain a private static boolean that is set by this method and returned in isShowingInstance
     */
    protected abstract var isShowingInstance: Boolean

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isShowingInstance = true
        mWasRestored = savedInstanceState != null
        if (mWasRestored) {
            mShouldPersist =
                savedInstanceState!!.getBoolean(STATE_SHOULD_PERSIST)
            if (!mShouldPersist) {
                showsDialog = false
                dismiss()
            }
        }
    }

    fun createArgs(title: String?, message: Int) {
        createArgs(title, getStringResource(message))
    }

    fun createArgs(title: String?, message: String?) {
        var args: Bundle? = Bundle()

        if (title != null) args!!.putString(KEY_TITLE, title)

        if (message != null) args!!.putString(KEY_MESSAGE, message)

        args = onCreateArgs(args)
        setArguments(args)
    }

    // Hook for adding additional args
    protected open fun onCreateArgs(args: Bundle?): Bundle? {
        return args
    }

    override fun onClose(isCancel: Boolean) {
        super.onClose(isCancel)
        this.isShowingInstance = false
    }

    companion object {
        protected const val KEY_TITLE: String = "title"
        protected const val KEY_MESSAGE: String = "message"
    }
}
