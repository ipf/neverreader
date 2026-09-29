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
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.enableEdgeToEdge
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.commit
import com.neverreader.app.settings.isDarkAppTheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.transition.TransitionManager
import com.neverreader.app.App
import com.neverreader.app.R
import com.neverreader.app.settings.Brightness
import com.neverreader.app.settings.Theme
import com.neverreader.ui.compose.AppSnackbarHost
import com.neverreader.ui.theme.AppTheme
import com.neverreader.util.android.ApiLevel
import com.neverreader.util.android.ContextUtil
import com.neverreader.util.android.FormFactor
import com.neverreader.util.android.ViewUtil
import com.neverreader.util.android.WindowUtil.NavigationBarColorProperty
import com.neverreader.util.android.WindowUtil.StatusBarColorProperty
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.neverreader.util.java.Logs

/**
 * The base activity for NeverReader's screens. Automatically handles tracking and [AppLifecycle] events.
 * See [.isUserPresent] to opt out.
 */
abstract class AbsNeverReaderActivity : AppCompatActivity() {

    enum class ActivityAccessRestriction {
        /**
         * The user must be logged in. Activity will auto-finish if not logged in or on log out.
         */
        REQUIRES_LOGIN,

        /**
         * A special case for the Login/Splash Activity. Similar to [.REQUIRES_LOGGED_OUT] as it will auto-finish if logged in or on login.
         * The only difference is that it will auto-launch the default activity as a replacement. The other types just close and offer no replacement.
         */
        LOGIN_ACTIVITY,

        /**
         * Behaves as ANY if the user is opted into the Guest Mode experience. Otherwise, functions are the same as REQUIRES_LOGIN
         */
        ALLOWS_GUEST,

        /**
         * Can be launched at any time, not dependent on the user state.
         */
        ANY
    }

    private val mOnLifeCycleChangedListeners = ArrayList<OnLifeCycleChangedListener>()
    private val mOnConfigurationChangedListeners = ArrayList<OnConfigurationChangedListener>()

    protected var mIsHelpActivity: Boolean = false

    protected var mHandler: Handler? = null

    private var mAccessReceiver: BroadcastReceiver? = null
    private var mFullShutdownReceiver: BroadcastReceiver? = null
    private val EXTRA_KILL_APP = "killApp"

    protected var isMenuVisible: Boolean = true

    private var mTheme = 0

    /**
     * The resolved app theme, so Compose screens can honour the in-app preference
     * instead of falling back to the system setting.
     */
    fun currentTheme(): Int = mTheme



    private var themeJob: Job? = null

    /**
     * Whether or not the ask overlay is visible or in the process of becoming visible (animating).
     */
    private var mAskUrlOverlayVisible = false

    /**
     * Every snackbar in the app renders here. Replaces the old AppSnackbar view,
     * which needed the themed-view colour machinery just to pick a background.
     */
    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val appContent = mutableStateOf<(@Composable () -> Unit)?>(null)
    private val fragmentContainerId = View.generateViewId()

    /**
     * The activity root: a Compose surface holding the subclass's content and the
     * snackbar host.
     *
     * Replaces a three-file XML scaffold - activity_root.xml inflating
     * NeverReaderActivityRootView, which inflated ril_root.xml, which held a
     * FrameLayout, a ViewStub and a ComposeView. All it bought was nesting one
     * container inside another.
     */
    private fun installComposeRoot() {
        super.setContentView(ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme(darkTheme = isDarkAppTheme(this@AbsNeverReaderActivity)) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.systemBars),
                    ) {
                        appContent.value?.invoke()
                        AppSnackbarHost(snackbarHostState)
                    }
                }
            }
        })
    }

    /** Supplies the activity's content: Compose, or [hostFragment]. */
    protected fun setAppContent(content: @Composable () -> Unit) {
        appContent.value = content
    }

    /** Shows a message on the shared host. Fire and forget. */
    protected fun snack(message: String, long: Boolean = false) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short,
            )
        }
    }

    private var mToasty: Toast? = null

    private var mIsContentSet = false

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

        // targetSdk 35 enforces edge-to-edge, so the window no longer fits the
        // system bars and the content has to inset itself. One place: the
        // Compose root, so every screen and the reader's WebView inherit it.
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        mTheme = app()!!.theme().get(this)
        mHandler = Handler()

        setActivityTheme(app()!!.theme().get(this))


        if (mIsContentSet) {
            Logs.throwIfNotProduction("You must call the super.onCreate() of AbsNeverReaderActivity before calling setContentView")
        }
        installComposeRoot()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Nothing intercepts ahead of the dispatcher: destinations pop
                // their own back stack, and dialogs handle back themselves.
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        })

        setBackgroundDrawable()

        if (!isFinishing) {
            installLogoutReceiver(this.accessType)
        }

        onCreateOrRestart()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityCreate(savedInstanceState, this)
        }

        val label = getString(this.applicationInfo.labelRes)
        val colorPrimary = ContextCompat.getColor(this, com.neverreader.ui.R.color.nr_coral_2)

        setTaskDescription(TaskDescription(label, null, colorPrimary))


        app()!!.activities().onActivityCreate(this)
    }

    /**
     * Similar to [.setContentView] but allows you to supply a Fragment as your root layout.
     *
     *
     * Sets the fragment tag as null, if you want to set a tag, use [.setContentFragment].
     * @param fragment
     */

    /**
     * Similar to [.setContentView] but allows you to supply a Fragment as your root layout.
     * @param fragment
     * @param tag
     */

    

    /**
     * Something has modified the theme (dark/light mode), the UI should update as needed.
     *
     * @param newTheme
     */
    fun onThemeChanged(newTheme: Int) {
        setActivityTheme(newTheme)


        setBackgroundDrawable()
        mTheme = newTheme

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
        }

        onCreateOrRestart()

        for (listener in mOnLifeCycleChangedListeners) {
            listener.onActivityRestart(this)
        }
    }

    protected fun onCreateOrRestart() {
        Brightness.applyBrightnessIfSet(this)
        invalidateStatusBarColor()
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
        themeJob = lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app()!!.theme().observeFor(this@AbsNeverReaderActivity).collect { newTheme ->
                    onThemeChanged(newTheme)
                }
            }
        }
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

        themeJob?.cancel()
        themeJob = null

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
        lifecycleScope.launch {
            mAskUrlOverlayVisible = true
            val result = snackbarHostState.showSnackbar(
                message = getString(R.string.lb_add_copied_url),
                actionLabel = getString(com.neverreader.ui.R.string.ac_save),
                withDismissAction = true,
                // Held until the user acts or swipes it away, as before.
                duration = SnackbarDuration.Indefinite,
            )
            mAskUrlOverlayVisible = false
            onClipboardUrlPromptViewDismissed()
            if (result != SnackbarResult.ActionPerformed) return@launch

            val message = withContext(Dispatchers.IO) {
                val existing = runCatching {
                    App.from(this@AbsNeverReaderActivity).bookmarks().bookmarkByUrlOnce(url)
                }.getOrNull()
                val saved = runCatching {
                    App.from(this@AbsNeverReaderActivity).bookmarks().add(url, null)
                }
                when {
                    existing != null -> R.string.ts_add_already
                    saved.isSuccess -> R.string.ts_add_added
                    else -> R.string.ts_add_error
                }
            }
            // Auto-dismissing. The old bar was forced down after a flat 3s, which
            // was unreadable at large font sizes; Short scales with the setting.
            snackbarHostState.showSnackbar(
                message = message.toString(),
                duration = SnackbarDuration.Short,
            )
        }
    }

    /**
     * If needed, this can be overridden to add custom dismissal handling.
    *
     */
    /** Called when the clipboard prompt goes away. */
    protected fun onClipboardUrlPromptViewDismissed() {}



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

    }

    /**
     * Launches the default, starting Activity for the NeverReader app.
     */
    fun startDefaultActivity() {
        val activity: Class<out Activity> = app()?.userManager()!!.defaultActivity
        startActivity(Intent(this, activity))
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


    companion object {
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

        /**
         * Since we seem to do this type casting a lot, this is a helper method for cleaner code. Pass a context
         * and if the context is a RilAppActivity it will return one, otherwise it returns null.
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
