package com.neverreader.sdk.util

import android.animation.ObjectAnimator
import androidx.activity.OnBackPressedCallback
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager.TaskDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Process
import android.util.Property
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.transition.TransitionManager
import com.neverreader.app.App
import com.neverreader.app.AppThreads.UiThreadResponse
import com.neverreader.app.R
import com.neverreader.app.settings.Brightness
import com.neverreader.app.settings.Theme
import com.neverreader.sdk.util.fragment.NeverReaderFragmentManager
import com.neverreader.sdk.util.view.RainbowBar
import com.neverreader.ui.view.notification.AppSnackbar
import com.neverreader.ui.view.notification.AppSnackbar.Companion.current
import com.neverreader.ui.view.notification.AppSnackbar.Companion.make
import com.neverreader.ui.view.notification.AppSnackbar.Companion.setAnchor
import com.neverreader.ui.view.themed.ThemeColors
import com.neverreader.ui.view.themed.Themed
import com.neverreader.util.android.ApiLevel
import com.neverreader.util.android.ContextUtil
import com.neverreader.util.android.FormFactor
import com.neverreader.util.android.ViewUtil
import com.neverreader.util.android.WindowUtil.NavigationBarColorProperty
import com.neverreader.util.android.WindowUtil.StatusBarColorProperty
import com.neverreader.util.android.fragment.FragmentUtil
import com.neverreader.util.android.fragment.FragmentUtil.FragmentLaunchMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.neverreader.util.android.view.ManuallyUpdateTheme
import com.neverreader.util.java.Logs
import com.neverreader.util.java.Milliseconds
import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import io.reactivex.functions.Consumer
import java.lang.ref.WeakReference

/**
 * The base activity for NeverReader's screens. Automatically handles tracking and [AppLifecycle] events.
 * See [.isUserPresent] to opt out.
 */
abstract class AbsNeverReaderActivity : AppCompatActivity(), Themed {
    protected var mContent: AppActivityContentView? = null

    enum class ActivityAccessRestriction {
        /**
         * The user must be logged in. Activity will auto-finish if not logged in or on log out.
         */
        REQUIRES_LOGIN,

        /**
         * A special case for the Login/Splash Activity. Similar to [.REQUIRES_LOGGED_OUT] as it will auto-finish if logged in or on login.
         * The only difference is that it will auto launch the default activity as a replacement. The other types just close and offer no replacement.
         */
        LOGIN_ACTIVITY,

        /**
         * Behaves as ANY if the user is opted into the Guest Mode experience. Otherwise, functions the same as REQUIRES_LOGIN
         */
        ALLOWS_GUEST,

        /**
         * Can be launched at any time, not dependent on the user state.
         */
        ANY
    }

    private val mOnLifeCycleChangedListeners = ArrayList<OnLifeCycleChangedListener>()
    private val mOnBackPressedListeners = ArrayList<OnBackPressedListener>()
    private val mOnConfigurationChangedListeners = ArrayList<OnConfigurationChangedListener>()

    protected var mIsHelpActivity: Boolean = false

    protected var mHandler: Handler? = null

    private var mAccessReceiver: BroadcastReceiver? = null
    private var mFullShutdownReceiver: BroadcastReceiver? = null
    private val EXTRA_KILL_APP = "killApp"
    protected var mRoot: NeverReaderActivityRootView? = null

    protected var isMenuVisible: Boolean = true

    private var mTheme = 0

    protected var mViewsListeningForThemeChanges: ArrayList<WeakReference<ManuallyUpdateTheme?>> =
        ArrayList<WeakReference<ManuallyUpdateTheme?>>()

    private var mThemeFlag = 0

    private var mThemeSubscription = Disposables.empty()

    /**
     * Whether or not the ask overlay is visible or in the process of becoming visible (animating).
     */
    private var mAskUrlOverlayVisible = false

    private var mToasty: Toast? = null

    val neverReaderFragmentManager: NeverReaderFragmentManager =
        NeverReaderFragmentManager(super.getSupportFragmentManager(), this)
    private var mIsContentSet = false
    private val mWindowInsets = Rect()

    fun app(): App? {
        return application as? App
    }


    open val isUserPresent: Boolean
        /**
         * Whether or not this activity should trigger [App.onActivityChange] and be considered
         * that the user is present in the app and trigger [AppLifecycle.onUserPresent] and related flows like opened_app events.
         * Defaults to true, override to opt out or adjust.
         * **The value here should not change between calls.** If it does, then we need to store a value from the first call in onResume to when it is checked again in onPause
         */
        get() = true


    @SuppressLint("NewApi")
    public override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_LIFECYCLE) Logs.i(
            "Lifecycle",
            "onCreate " + (if (savedInstanceState != null) "restore " else "new ") + this.toString()
        )

        FormFactor.init()

        super.onCreate(savedInstanceState)

        mThemeFlag = this.defaultThemeFlag
        mTheme = app()!!.theme().get(this)
        mHandler = Handler()

        setActivityTheme(app()!!.theme().get(this))


        // Create the root layout structure that will wrap the view supplied by subclasses.
        if (mIsContentSet) {
            Logs.throwIfNotProduction("You must call the super.onCreate() of AbsNeverReaderActivity before calling setContentView")
        }
        super.setContentView(R.layout.activity_root)
        mRoot = findViewById<NeverReaderActivityRootView>(R.id.nr_root)
        mRoot!!.attach(this)
        mContent = mRoot!!.contentView

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (this@AbsNeverReaderActivity.root.onBackPressed()) return  // Handled
                if (neverReaderFragmentManager.onBackPressed()) return  // Handled
                for (listener in ArrayList<OnBackPressedListener>(mOnBackPressedListeners)) { // Iterates on a copy to allow listeners to remove themselves during callback/iteration without a concurrent mod exceptions.
                    if (listener.onBackPressed()) return  // Handled
                }
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        setBackgroundDrawable()

        if (!isFinishing) {
            installLogoutReceiver(this.accessType)
        }

        if (savedInstanceState != null) {
            this.neverReaderFragmentManager.onRestoreInstanceState(savedInstanceState)
        }

        onCreateOrRestart()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityCreate(savedInstanceState, this)
        }

        val label = getString(this.applicationInfo.labelRes)
        val colorPrimary = ContextCompat.getColor(this, com.neverreader.ui.R.color.nr_coral_2)

        setTaskDescription(TaskDescription(label, null, colorPrimary))

        ViewCompat.setOnApplyWindowInsetsListener(
            mRoot!!,
            OnApplyWindowInsetsListener { v: View?, insets: WindowInsetsCompat? ->
                mWindowInsets.set(
                    insets!!.systemWindowInsetLeft,
                    insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight,
                    insets.systemWindowInsetBottom
                )
                updateAskUrlOverlayPadding(current)
                insets
            })

        app()!!.activities().onActivityCreate(this)
    }

    override fun setContentView(view: View?) {
        // Overridden to insert into our root layout instead.
        mIsContentSet = true
        mContent!!.addView(view)
    }

    override fun setContentView(layoutResID: Int) {
        // Overridden to insert into our root layout instead.
        mIsContentSet = true
        layoutInflater.inflate(layoutResID, mContent)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        // Overridden to insert into our root layout instead.
        mIsContentSet = true
        mContent!!.addView(view, params)
    }

    /**
     * Similar to [.setContentView] but allows you to supply a Fragment as your root layout.
     *
     *
     * Sets the fragment tag as null, if you want to set a tag, use [.setContentFragment].
     * @param fragment
     */
    fun setContentFragment(fragment: Fragment?) {
        setContentFragment(fragment, null)
    }

    /**
     * Similar to [.setContentView] but allows you to supply a Fragment as your root layout.
     * @param fragment
     * @param tag
     */
    fun setContentFragment(fragment: Fragment?, tag: String?) {
        mIsContentSet = true
        FragmentUtil.addFragment(fragment!!, this, R.id.content, tag, false)
    }

    /**
     * A convenience method to show this fragment based on its launch mode.
     *
     *
     * [FragmentLaunchMode.ACTIVITY] will add the fragment as the main content, same as [.setContentFragment].
     *
     *
     * [FragmentLaunchMode.ACTIVITY_DIALOG] depends on the result of [FormFactor.showSecondaryScreensInDialogs].
     * If false, it is handled the same as the [FragmentLaunchMode.ACTIVITY] mode. If true, the content view will be set
     * to a full-screen rainbow bar and the fragment show as a dialog over it. Note in this case, the fragment must be an instance of
     * [DialogFragment] or an exception will be thrown. Another note is that if this fragment is dismissed, the activity will also
     * automatically finish itself because there is no other content to view.
     *
     *
     * **Warning** No other [FragmentLaunchMode]'s are supported by this method.
     *
     * @param fragment
     * @param tag
     * @param mode
     */
    fun setContentFragment(fragment: Fragment?, tag: String?, mode: FragmentLaunchMode?) {
        if (mode == FragmentLaunchMode.ACTIVITY) {
            setContentFragment(fragment)
        } else if (mode == FragmentLaunchMode.ACTIVITY_DIALOG
            || mode == FragmentLaunchMode.DIALOG
        ) { // If dialog, just launch as activity dialog.
            if (FormFactor.showSecondaryScreensInDialogs(this)) {
                // This is a special case where we launch it as dialog with a rainbow covering the background.
                // There is no additional view layout to the activity.
                val rainbow = RainbowBar(this)
                rainbow.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                setContentView(rainbow)

                FragmentUtil.addFragmentAsDialog(fragment as DialogFragment?, this, tag)


                // If this fragment is dismissed (from a back button for example), finish this activity since there is no other content to show.
                this.neverReaderFragmentManager.addOnBackStackChangedListener {
                    val frags: NeverReaderFragmentManager = neverReaderFragmentManager
                    if (frags.backStackEntryCount == 0 || frags.getFragments().isEmpty()) {
                        finish()
                    }
                }
            } else {
                setContentFragment(fragment, tag)
            }
        } else {
            throw RuntimeException("unexpected mode")
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        this.neverReaderFragmentManager.onSaveInstanceState(outState)
    }

    val defaultThemeFlag: Int
        /**
         * The default Theme Flag to use when creating this Activity.
         * Subclasses can override this to change the flag for their activity, or change it at runtime with [.setThemeFlag].
         * See [.getThemeFlag] for the current value.
         */
        get() = Theme.FLAG_ALLOW_ALL

    override fun getThemeState(view: View): IntArray? {
        return app()!!.theme().getState(view)
    }

    override fun getThemeColors(context: Context): ThemeColors {
        return getThemeColors(app()!!.theme().get(context))
    }

    override fun getThemeColorsChanges(context: Context): Observable<ThemeColors?> {
        return app()!!.theme()
            .observeFor(context)!!
            .map { theme: Int? -> this.getThemeColors(theme!!) }
    }

    private fun getThemeColors(theme: Int): ThemeColors {
        return when (theme) {
            Theme.DARK -> ThemeColors.DARK
            Theme.LIGHT -> ThemeColors.LIGHT
            else -> ThemeColors.LIGHT
        }
    }

    /**
     * Something has modified the theme (dark/light mode), the UI should update as needed.
     *
     * @param newTheme
     */
    fun onThemeChanged(newTheme: Int) {
        TransitionManager.beginDelayedTransition(mRoot!!, THEME_CHANGE)
        setActivityTheme(newTheme)
        ViewUtil.refreshDrawableStateDeep(mRoot!!.getRootView())


        // Manually update any web views
        for (reference in mViewsListeningForThemeChanges) {
            val view = reference.get()
            view?.updateThemeManually()
        }

        setBackgroundDrawable()
        mTheme = newTheme


        // Dispatch to any visible fragments
        neverReaderFragmentManager.onThemeChanged(newTheme)

        invalidateStatusBarColor()
    }

    /**
     * Update the system bar colors to the current theme.
     */
    fun invalidateStatusBarColor() {
        val statusBarColor = Theme.getStatusBarColor(mTheme, this)
        animateThemeColorChange(STATUS_BAR_COLOR, statusBarColor)

        var systemUiFlags = 0
        if (mTheme != Theme.DARK) {
            systemUiFlags = systemUiFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
        if (ApiLevel.isLightNavigationBarAvailable()) {
            if (mTheme != Theme.DARK) {
                systemUiFlags = systemUiFlags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
            animateThemeColorChange(NAVIGATION_BAR_COLOR, statusBarColor)
            if (showNavigationBarDivider()) {
                window.setNavigationBarDividerColor(
                    Theme.getNavigationBarDividerColor(mTheme, this)
                )
            }
        }
        window.decorView.systemUiVisibility = systemUiFlags
    }

    private fun animateThemeColorChange(property: Property<Window, Int>, value: Int) {
        val animator = ObjectAnimator.ofInt<Window>(window, property, value)
        animator.duration = ThemeChange.DURATION.toLong()
        animator.setEvaluator(ThemeChange.ARGB_EVALUATOR)
        animator.start()
    }

    /** Override to hide the navigation bar divider.  */
    protected fun showNavigationBarDivider(): Boolean {
        return true
    }

    /**
     * Sets the background drawable (window) for the activity to getActivityBackground.  If getActivityBackground is null, it will not change anything.
     */
    private fun setBackgroundDrawable() {
        val bg = this.activityBackground
        setBackgroundDrawable(bg)
    }

    /**
     * Sets the activities background (window background)
     * @param drawable
     */
    protected fun setBackgroundDrawable(drawable: Drawable?) {
        window.setBackgroundDrawable(drawable)
    }

    private fun setActivityTheme(newTheme: Int) {
        setTheme(if (themeOverride() != 0) themeOverride() else if (Theme.isDark(newTheme)) R.style.Theme_NeverReaderDefault_Dark else R.style.Theme_NeverReaderDefault_Light)
    }

    /**
     * @return a style resource to use as the activity theme.
     */
    @StyleRes
    protected fun themeOverride(): Int {
        return 0
    }

    protected override fun onRestart() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onRestart $this")

        installLogoutReceiver(this.accessType)

        super.onRestart()

        val theme = app()!!.theme().get(this)
        if (mTheme != theme) {
            mTheme = theme
            neverReaderFragmentManager.onThemeChanged(theme)
        }

        onCreateOrRestart()


        // Dispatch to any visible fragments
        neverReaderFragmentManager.onActivityRestart()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityRestart(this)
        }
    }

    protected fun onCreateOrRestart() {
        Brightness.applyBrightnessIfSet(this)
        invalidateStatusBarColor()
    }

    override fun getSupportFragmentManager(): FragmentManager {
        return this.neverReaderFragmentManager
    }

    /**
     * TODO REVIEW
     * This mechanism is to make sure all activities are finished when the user logs out,
     * so that no old activities with data from the logged-out user persist or are restored.
     * The idea here is that it registers for a ACTION_LOGOUT broadcast and finishes when it
     * receives it. This has been in here since the beginning of the app and for many many years.
     *
     * However, recently we discovered there is a case this does not properly handle.
     * If the activity is removed from memory, it will not receive the broadcast and
     * has the potential to be restored later.  An easy way to experiment with this is
     * "Don't keep activities" in Developer Options.
     *
     * For now, the easiest fix seemed to simply be to use [Activity.finishAffinity]
     * in [com.neverreader.app.UserManager.logout] and that is the fix that was implemented for
     * starters. But really, it means we likely don't need this complexity any more.
     *
     * At some point we should review how this works and see if we can remove these broadcasts.
     */
    private fun installLogoutReceiver(accessType: ActivityAccessRestriction?) {
        // if the access restriction is ALLOWS_GUEST and this user is opted into
        // allowed guest mode, allow them to view this screen without logging in.
        // Otherwise, switch it to REQUIRES_LOGIN.

        var accessType = accessType
        if (accessType == ActivityAccessRestriction.ALLOWS_GUEST) {
            accessType = ActivityAccessRestriction.REQUIRES_LOGIN
        }

        if (accessType != ActivityAccessRestriction.ANY) {
            // Register this activity to be finished if the login state changes

            var action: String? = null

            if (accessType == ActivityAccessRestriction.REQUIRES_LOGIN) {
                if (app()!!.accountManager().activeCached == null && !mIsHelpActivity) {
                    finish()
                } else {
                    action = ACTION_LOGOUT
                }
            } else {
                if (app()!!.accountManager().activeCached != null) {
                    if (accessType == ActivityAccessRestriction.LOGIN_ACTIVITY) {
                        // This shouldn't be called unless it needs to launch, since a login should finish any previously open instances.
                        startDefaultActivity()
                    }

                    finish()
                } else {
                    action = ACTION_LOGIN
                }
            }

            if (action != null && mAccessReceiver == null) {
                val intentFilter = IntentFilter()
                intentFilter.addAction(action)

                mAccessReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        finish()
                    }
                }

                LocalBroadcastManager.getInstance(this@AbsNeverReaderActivity)
                    .registerReceiver(mAccessReceiver!!, intentFilter)
            }
        }


        // Register a full shutdown receiver
        if (mFullShutdownReceiver == null) {
            val shutdownIntentFilter = IntentFilter()
            shutdownIntentFilter.addAction(ACTION_SHUTDOWN)
            mFullShutdownReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent) {
                    finish()
                    if (intent.getBooleanExtra(
                            EXTRA_KILL_APP,
                            false
                        )
                    ) Process.killProcess(Process.myPid())
                }
            }

            LocalBroadcastManager.getInstance(this@AbsNeverReaderActivity)
                .registerReceiver(mFullShutdownReceiver!!, shutdownIntentFilter)
        }
    }

    /**
     * This will send a RilAppActivity.ACTION_SHUTDOWN action to all RilAppActivitys in the Task. The action
     * will cause all the open RilAppActivitys to finish().
     *
     * @param killApp if it should also force close/kill the process. This will completely close the app abruptly.
     */
    fun finishAllActivities(killApp: Boolean) {
        // Used to completely kill the app, such as when a database error occurs
        val broadcastIntent = Intent()
        broadcastIntent.putExtra(EXTRA_KILL_APP, killApp)
        broadcastIntent.action = ACTION_SHUTDOWN
        LocalBroadcastManager.getInstance(this@AbsNeverReaderActivity)
            .sendBroadcast(broadcastIntent)
    }

    /**
     * Declare what state the user must be in in order to use this Activity.
     *
     * @return One of the [ActivityAccessRestriction] values.
     */
    protected abstract val accessType: ActivityAccessRestriction?

    override fun onStart() {
        super.onStart()
        mThemeSubscription = app()!!.theme().observeFor(this)
            ?.subscribe(Consumer { newTheme: Int? -> this.onThemeChanged(newTheme!!) })
            ?: Disposables.empty()
        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityStart(this)
        }
    }

    public override fun onResume() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onResume $this")

        if (this.isUserPresent) App.onActivityChange(this)

        super.onResume()


        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityResume(this)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkClipboardForUrl()
        }
    }

    override fun onStop() {
        super.onStop()
        if (App.activityContext == null) {
            // User went to a screen that is not in our app.
            App.setUserPresent(false, this)
        }

        mThemeSubscription.dispose()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityStop(this)
        }
    }

    /**
     * Checks the clipboard for a url.
     * If there is a url available, it asks the user if they want to save it to their list.
     */
    protected open fun checkClipboardForUrl() {
        if (app()!!.accountManager().activeCached == null) {
            // Don't check if not logged in, because they have to log in to save.
            return
        }

        val url = app()!!.clipboard().getUrl()

        if (url != null) {
            showAskUrl(url)
        }
    }

    private fun showAskUrl(url: String) {
        // TODO if you share Copy Link, don't show this for that link

        val ask = make(
            this,
            AppSnackbar.Type.DEFAULT_DISMISSABLE,
            null,
            url.replace("https?://(www.)?".toRegex(), ""),
            null
        )
        ask.bind().onAction(com.neverreader.ui.R.string.ac_save, object : View.OnClickListener {
            @StringRes
            var message: Int = 0

            override fun onClick(v: View?) {
                ask.bind().dismiss()
                mAskUrlOverlayVisible = false

                CoroutineScope(Dispatchers.IO).launch {
                    var message: Int
                    val existing = runCatching {
                        App.from(this@AbsNeverReaderActivity).bookmarks().bookmarkByUrlOnce(url)
                    }.getOrNull()
                    val saved = runCatching {
                        App.from(this@AbsNeverReaderActivity).bookmarks().add(url, null)
                    }
                    message = if (existing != null) {
                        R.string.ts_add_already
                    } else if (saved.isSuccess) {
                        R.string.ts_add_added
                    } else {
                        R.string.ts_add_error
                    }
                    UiThreadResponse { _, _ ->
                        val confirm = make(
                            this@AbsNeverReaderActivity,
                            if (message != R.string.ts_add_added) AppSnackbar.Type.ERROR_DISMISSABLE else AppSnackbar.Type.DEFAULT_DISMISSABLE,
                            null,
                            getText(message),
                            null
                        )
                        updateAskUrlOverlayPadding(confirm)
                        onClipboardUrlPromptViewLayout(confirm)
                        confirm.bind()
                            .onDismiss(AppSnackbar.OnDismissListener { _ ->
                                onClipboardUrlPromptViewDismissed(confirm)
                            })
                        confirm.show()

                        // Hide in 3 seconds
                        mHandler!!.postDelayed(
                            Runnable { confirm.bind().dismiss() },
                            Milliseconds.SECOND * 3
                        )
                    }.uiOnComplete(true, null)
                }
            }
        }).singleLineMessage(true).title(getText(R.string.lb_add_copied_url))

        updateAskUrlOverlayPadding(ask)
        onClipboardUrlPromptViewLayout(ask)
        ask.bind().onDismiss(AppSnackbar.OnDismissListener { `__`: AppSnackbar.DismissReason? ->
            onClipboardUrlPromptViewDismissed(ask)
        })


        // Show
        mAskUrlOverlayVisible = true
        ask.show()


        // Hide in 10 seconds
        mHandler!!.postDelayed(Runnable {
            ask.bind().dismiss()
            mAskUrlOverlayVisible = false
        }, Milliseconds.SECOND * 10)
    }

    /**
     * If needed, this can be overridden to move the clipboard prompt to
     * a better spot for a specific layout.
     *
     * @param view The view to move to a different position in the layout.
     */
    protected fun onClipboardUrlPromptViewLayout(view: AppSnackbar?) {}

    /**
     * If needed, this can be overridden to add custom dismissal handling.
     *
     * @param view the view that was just dismissed
     */
    protected fun onClipboardUrlPromptViewDismissed(view: AppSnackbar?) {}

    protected fun updateAskUrlOverlayPadding(askView: View?) {
        if (askView != null) {
            val paddingDefault =
                getResources().getDimension(com.neverreader.ui.R.dimen.nr_space_sm).toInt()
            askView.setPadding(
                paddingDefault + mWindowInsets.left,
                paddingDefault,
                paddingDefault + mWindowInsets.right,
                paddingDefault + mWindowInsets.bottom
            )
        }
    }


    public override fun onPause() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onPause $this")

        if (this.isUserPresent) App.onActivityChange(null)

        super.onPause()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityPause(this)
        }
    }

    override fun onDestroy() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onDestroy $this")

        super.onDestroy()

        unregisterReceivers()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityDestroy(this)
        }
        mHandler!!.removeCallbacksAndMessages(null) // Otherwise we can accidentally keep the activity around for a few seconds after destroy.
    }

    override fun onLowMemory() {
        super.onLowMemory()
        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityLowMemory(this)
        }
    }

    override fun finish() {
        unregisterReceivers()
        super.finish()
    }

    private fun unregisterReceivers() {
        if (mAccessReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mAccessReceiver!!)
            mAccessReceiver = null
        }

        if (mFullShutdownReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mFullShutdownReceiver!!)
            mFullShutdownReceiver = null
        }
    }

    protected val activityBackground: Drawable
        /**
         * Return the window background for this activity, or null to just use the current window background.
         * @return
         */
        get() = ColorDrawable(app()!!.theme().getThemeBGColor(this))

    override fun onSearchRequested(): Boolean {
        if (!this.isMenuVisible) return false

        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        populateMenu(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val visible = this.isMenuVisible
        menu.setGroupVisible(MENU_GROUP_APP, visible)
        menu.setGroupVisible(MENU_GROUP_ACTIVITY, visible)
        return super.onPrepareOptionsMenu(menu)
    }

    protected fun populateMenu(menu: Menu) {
        menu.add(MENU_GROUP_APP, MENU_ITEM_SETTINGS, 1, getString(R.string.mu_settings))
            .setIcon(R.drawable.ic_menu_settings)

        if (!mIsHelpActivity) {
            menu.add(MENU_GROUP_APP, MENU_ITEM_HELP, 2, getString(R.string.mu_help))
                .setIcon(R.drawable.ic_menu_help)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setBackgroundDrawable()

        for (listener in mOnConfigurationChangedListeners) {
            listener.onConfigurationChanged(newConfig)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
    }

    /** NOTE currently disabled. if required again, uncomment out code below
     * Set the software dimming level.
     *
     * @param alpha 0 - 255. 0 for no dimming, 255 for complete black out.
     */
    fun setBrightnessOverlay(alpha: Int) {
        /*
		if (mSoftwareBrightnessOverlay == null && alpha > 0) {
			mSoftwareBrightnessOverlay = (ImageView) ((ViewStub) findViewById(R.id.stub_brightness)).inflate();
			mSoftwareBrightnessOverlay.setVisibility(View.VISIBLE);
		}
		mSoftwareBrightnessOverlay.setAlpha(alpha);
		*/
    }

    /**
     * Launches the default, starting Activity for the NeverReader app.
     */
    fun startDefaultActivity() {
        val activity: Class<out Activity> = app()?.userManager()!!.defaultActivity
        startActivity(Intent(this, activity))
    }

    /**
     * Set the content view to be invisible.
     */
    fun hideContent() {
        mContent!!.visibility = View.INVISIBLE
    }

    fun registerViewForThemeChanges(view: ManuallyUpdateTheme?) {
        mViewsListeningForThemeChanges.add(WeakReference<ManuallyUpdateTheme?>(view))
    }


    var themeFlag: Int
        /**
         * Gets a flag for which themes are currently allowed.  Should be one of the FLAG_ values in Theme
         * To change the flag for your activity override [.getDefaultThemeFlag]
         * @return
         */
        get() = mThemeFlag
        /**
         * Set what themes are currently allowed to display in the app. Will refresh views if it causes a theme change.
         *
         * One of [Theme.FLAG_ALLOW_ALL], [Theme.FLAG_ONLY_DARK], [Theme.FLAG_ONLY_LIGHT]
         * @param flag
         */
        set(flag) {
            val currentTheme = app()!!.theme().get(this)
            mThemeFlag = flag
            val newTheme = app()!!.theme().get(this)

            if (newTheme != currentTheme) {
                onThemeChanged(newTheme)
            }
        }

    open fun supportsRotationLock(): Boolean {
        return true
    }

    val root: NeverReaderActivityRootView
        get() = mRoot!!

    /**
     * Searches these Activities' fragments to find which one holds a certain view. If the parent view is found, it is returned. Otherwise null.
     * @param view
     * @return
     */
    fun getFragmentParentOfView(view: View?): Fragment? {
        val fragments: MutableList<Fragment?> = this.neverReaderFragmentManager.getFragments()
        for (frag in fragments) {
            if (FragmentUtil.isDetachedOrFinishing(frag)) {
                continue
            }

            val root = FragmentUtil.getRootView(frag) ?: continue

            if (ViewUtil.containsView(root, view)) {
                return frag
            }
        }
        return null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityResult(this, requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (listener in mOnLifeCycleChangedListeners) {
            listener.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    val themeInt: Int
        get() = app()!!.theme().get(this)


    /**
     * Add a listener for life cycle events like onCreate and onPause.
     * **Warning** if the activity is taken out of memory and recreated,
     * this will not automatically be readded for you. This can break
     * callbacks such as onActivityResult or onRequestPermissionsResult
     * if you don't readd the listener during onCreate
     *
     * @param listener
     * @see SimpleOnLifeCycleChangedListener
     */
    fun addOnLifeCycleChangedListener(listener: OnLifeCycleChangedListener?) {
        mOnLifeCycleChangedListeners.add(listener!!)
    }

    fun removeOnLifeCycleChangeListener(listener: OnLifeCycleChangedListener?) {
        mOnLifeCycleChangedListeners.remove(listener)
    }

    fun addOnBackPressedListener(listener: OnBackPressedListener?) {
        mOnBackPressedListeners.add(listener!!)
    }

    fun removeOnBackPressedListener(listener: OnBackPressedListener?) {
        mOnBackPressedListeners.remove(listener)
    }

    interface OnLifeCycleChangedListener {
        fun onActivityCreate(savedInstanceState: Bundle?, activity: AbsNeverReaderActivity?)
        fun onActivityRestart(activity: AbsNeverReaderActivity?)
        fun onActivityStart(activity: AbsNeverReaderActivity?)
        fun onActivityResume(activity: AbsNeverReaderActivity?)
        fun onActivityPause(activity: AbsNeverReaderActivity?)
        fun onActivityStop(activity: AbsNeverReaderActivity?)
        fun onActivityDestroy(activity: AbsNeverReaderActivity?)
        fun onActivityLowMemory(activity: AbsNeverReaderActivity?)
        fun onActivityResult(
            activity: AbsNeverReaderActivity?,
            requestCode: Int,
            resultCode: Int,
            data: Intent?
        )

        fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) //		void onFocusedFragmentChange(AbsNeverReaderActivity activity, Fragment focus);
    }

    interface OnBackPressedListener {
        /**
         * @return true if handled.
         */
        fun onBackPressed(): Boolean
    }

    fun addOnConfigurationChangedListener(listener: OnConfigurationChangedListener?) {
        mOnConfigurationChangedListeners.add(listener!!)
    }

    fun removeOnConfigurationChangedListener(listener: OnConfigurationChangedListener?) {
        mOnConfigurationChangedListeners.remove(listener)
    }

    interface OnConfigurationChangedListener {
        fun onConfigurationChanged(newConfig: Configuration?)
    }

    /**
     * A version of [OnLifeCycleChangedListener] that has no-op implementations of all the methods
     * so you can just override the ones you actually need.
     */
    abstract class SimpleOnLifeCycleChangedListener : OnLifeCycleChangedListener {
        override fun onActivityCreate(savedInstanceState: Bundle?, activity: AbsNeverReaderActivity?) {}

        override fun onActivityRestart(activity: AbsNeverReaderActivity?) {}

        override fun onActivityStart(activity: AbsNeverReaderActivity?) {}

        override fun onActivityResume(activity: AbsNeverReaderActivity?) {}

        override fun onActivityPause(activity: AbsNeverReaderActivity?) {}

        override fun onActivityStop(activity: AbsNeverReaderActivity?) {}

        override fun onActivityDestroy(activity: AbsNeverReaderActivity?) {}

        override fun onActivityLowMemory(activity: AbsNeverReaderActivity?) {}

        override fun onActivityResult(
            activity: AbsNeverReaderActivity?,
            requestCode: Int,
            resultCode: Int,
            data: Intent?
        ) {
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
        }
    }

    /**
     * A [AbsNeverReaderFragment] has been shown overlaying the activity so that the fragment is now the
     * main focus of the user.
     * @see .onRegainedFocus
     */
    fun onLostFocus() {}

    /**
     * A [AbsNeverReaderFragment] that was covering this activity is gone. This activity is once again
     * the user's main focus.
     * @see .onLostFocus
     */
    fun onRegainedFocus() {}


    open val isListenUiEnabled: Boolean
        /**
         * Does this activity want to show the Listen UI.
         * By default, it is a minimized player at the bottom which can be expanded to a fullscreen view.
         * Return false not to show Listen UI in this activity.
         */
        get() = true

    /**
     * Expand the Listen UI if it is already visible or mark it to automatically expand when it becomes visible.
     * If your intention is for the Listen UI to expand as soon as possible make sure Listen is started
     * and start it if it isn't.
     *
     *
     * This won't start Listen.
     */
    fun expandListenUi() {
        this.root.expandListen()
    }

    val listenViewStates: Observable<Any?>?
        get() = Observable.empty<Any?>()

    /**
     * Default implementation to display a snackbar on this Activity.  Simply calls show.
     *
     * @param bar the snackbar
     */
    fun showSnackbar(bar: AppSnackbar) {
        setAnchor(this, bar, null)
        bar.show()
    }

    companion object {
        const val DIALOG_SUBCLASS: Int = 20 // Should be higher than any generic dialog ids
        const val DEBUG_LIFECYCLE: Boolean = false

        const val ACTION_SHUTDOWN: String = "com.ideashower.readitlater.ACTION_SHUTDOWN"
        const val ACTION_LOGOUT: String = "com.ideashower.readitlater.ACTION_LOGOUT"
        const val ACTION_LOGIN: String = "com.ideashower.readitlater.ACTION_LOGIN"

        const val MENU_GROUP_APP: Int = -1
        const val MENU_GROUP_ACTIVITY: Int = -2
        const val MENU_ITEM_SETTINGS: Int = 1
        const val MENU_ITEM_HELP: Int = 2

        private val THEME_CHANGE = ThemeChange()
        private val STATUS_BAR_COLOR = StatusBarColorProperty()
        private val NAVIGATION_BAR_COLOR = NavigationBarColorProperty()

        const val EXTRA_UI_CONTEXT: String = "com.neverreader.extra.uiContext"

        /**
         * Since we seem to do this type casting a lot, this is a helper method for cleaner code. Pass a context
         * and if the context is a RilAppActivity it will return one, other wise it returns null.
         *
         * @param context
         * @return the context casted to a RilAppActivity if it is not null and is one, otherwise null.
         */
        fun from(context: Context?): AbsNeverReaderActivity? {
            val activity = ContextUtil.getActivity(context)
            return activity as? AbsNeverReaderActivity
        }

        /**
         * Uses a shared toast message for a  RilAppActivity. This is helpful for when toasts might happen fast enough
         * to overlap. This will ensure that the new toast is visible right away instead of waiting for the previous
         * toast to finish before becoming visible.
         *
         * This method does not call show() on the new Toast.
         *
         * @param context should be a RilAppActivity, but can pass a context for coding convenience.
         * @param text
         * @param res if text is null, it will use a resource id, otherwise it is ignored
         * @param duration
         * @return
         */
        @SuppressLint("ShowToast")
        fun toast(context: Context?, text: String?, res: Int, duration: Int): Toast? {
            val activity = context as AbsNeverReaderActivity
            if (activity.mToasty == null) {
                if (text != null) {
                    activity.mToasty = Toast.makeText(context, text, duration)
                } else {
                    activity.mToasty = Toast.makeText(context, res, duration)
                }
            }
            // REVIEW why isn't this part in a else?
            activity.mToasty!!.duration = duration
            if (text != null) {
                activity.mToasty!!.setText(text)
            } else {
                activity.mToasty!!.setText(res)
            }
            return activity.mToasty
        }
    }
}
