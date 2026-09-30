package com.neverreader.sdk.util

import androidx.activity.OnBackPressedCallback
import android.app.Activity
import android.app.ActivityManager.TaskDescription
import androidx.core.view.WindowInsetsControllerCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import com.neverreader.app.settings.isDarkAppTheme
import androidx.core.content.ContextCompat
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.neverreader.app.App
import com.neverreader.app.R
import com.neverreader.app.settings.Theme
import com.neverreader.ui.compose.AppSnackbarHost
import com.neverreader.ui.theme.AppTheme
import com.neverreader.util.android.ContextUtil
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.neverreader.util.java.Logs

/**
 * The base activity for NeverReader's screens. Owns the window theme and the
 * Compose root that every screen is drawn into.
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

    protected var mIsHelpActivity: Boolean = false


    private var mFullShutdownReceiver: BroadcastReceiver? = null
    private val EXTRA_KILL_APP = "killApp"

    /**
     * The resolved app theme, held as Compose state so that following the system
     * setting actually redraws the app.
     *
     * It was a plain Int, read once from inside the composition's content lambda.
     * Every activity declares `uiMode` in its `configChanges`, so a system
     * dark-mode switch calls [onConfigurationChanged] instead of recreating the
     * activity: nothing recomposed, and the app kept its old palette until the
     * process was killed. [onConfigurationChanged] now recomputes this.
     */
    private var mTheme by mutableStateOf(0)

    /** The resolved app theme, so Compose screens can honour the in-app preference
     *  instead of falling back to the system setting. */
    fun currentTheme(): Int = mTheme



    private var themeJob: Job? = null

    /**
     * Every snackbar in the app renders here. Replaces the old AppSnackbar view,
     * which needed the themed-view colour machinery just to pick a background.
     */
    val snackbarHostState: SnackbarHostState = SnackbarHostState()

    private val appContent = mutableStateOf<(@Composable () -> Unit)?>(null)

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

    /** Supplies the activity's content. Calling it again replaces the whole composition. */
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


    fun app(): App? {
        return application as? App
    }


    public override fun onCreate(savedInstanceState: Bundle?) {
        if (DEBUG_LIFECYCLE) Logs.i(
            "Lifecycle",
            "onCreate " + (if (savedInstanceState != null) "restore " else "new ") + this.toString()
        )

        // targetSdk 35 enforces edge-to-edge, so the window no longer fits the
        // system bars and the content has to inset itself. One place: the
        // Compose root, so every screen and the reader's WebView inherit it.
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        mTheme = app()!!.theme().get(this)

        setActivityTheme(mTheme)

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

        val label = getString(this.applicationInfo.labelRes)
        val colorPrimary = ContextCompat.getColor(this, com.neverreader.ui.R.color.nr_coral_2)

        setTaskDescription(
            TaskDescription.Builder()
                .setLabel(label)
                .setIcon(0)
                .setPrimaryColor(colorPrimary)
                .build()
        )
    }

    /**
     * The theme changed, so the window and everything drawn in it have to follow.
     *
     * Public because it used to be called from the base class's own theme
     * subscription; it is not called from anywhere now.
     */
    private fun onThemeChanged(newTheme: Int) {
        if (mTheme == newTheme) return
        mTheme = newTheme
        setActivityTheme(newTheme)
        setBackgroundDrawable()
        applySystemBarAppearance()
    }

    /**
     * Match the system bar icons to the theme.
     *
     * Only the icon appearance is ours to set. enableEdgeToEdge leaves the bars
     * transparent on API 35, and from 35 the platform ignores statusBarColor,
     * navigationBarColor and systemUiVisibility outright, so the colours this
     * used to animate were no-ops and the divider no longer exists. What is
     * left is deciding whether the icons should be dark, which
     * WindowInsetsControllerCompat still honours.
     */
    fun applySystemBarAppearance() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        val lightBars = mTheme != Theme.DARK
        controller.isAppearanceLightStatusBars = lightBars
        controller.isAppearanceLightNavigationBars = lightBars
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

        onCreateOrRestart()
    }

    protected fun onCreateOrRestart() {
        onThemeChanged(app()!!.theme().get(this))
        applySystemBarAppearance()
    }

    /**
     * Makes sure an activity is not left on screen when the login state does
     * not permit it to be there, and registers for a shutdown so that a logout
     * can finish everything at once.
     *
     * The login-state half is a plain check on the way in: if the activity
     * requires a login and there is no active account, or it is the login
     * activity and one is active, it finishes itself. That replaced an earlier
     * design which registered for an ACTION_LOGOUT or ACTION_LOGIN broadcast;
     * nothing ever sent either, so the receiver could never fire. An activity
     * removed from memory would also have missed the broadcast and been
     * restored later, which is the case the check sidesteps entirely.
     *
     * The shutdown receiver is live: [UserManager.logout] calls
     * [finishAllActivities] to finish every activity and kill the process.
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

            if (accessType == ActivityAccessRestriction.REQUIRES_LOGIN) {
                if (app()!!.accountManager().activeCached == null && !mIsHelpActivity) {
                    finish()
                }
            } else {
                if (app()!!.accountManager().activeCached != null) {
                    if (accessType == ActivityAccessRestriction.LOGIN_ACTIVITY) {
                        // This shouldn't be called unless it needs to launch, since a login should finish any previously open instances.
                        startDefaultActivity()
                    }

                    finish()
                }
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
    }

    public override fun onResume() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onResume $this")

        super.onResume()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            checkClipboardForUrl()
        }
    }

    override fun onStop() {
        super.onStop()

        themeJob?.cancel()
        themeJob = null
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

    /**
     * Offers to save a url found on the clipboard. Only a read of the clipboard
     * is involved: the app has no Copy Link action, so there is no copy of its
     * own to suppress the prompt for. [Clipboard] does still skip a url it has
     * already offered, so the same link is not offered twice in a row.
     */
    private fun showAskUrl(url: String) {
        lifecycleScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = getString(R.string.lb_add_copied_url),
                actionLabel = getString(com.neverreader.ui.R.string.ac_save),
                withDismissAction = true,
                // Held until the user acts or swipes it away, as before.
                duration = SnackbarDuration.Indefinite,
            )
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
            // getString, not toString: that branch produces a string resource id,
            // and toString on it showed the user a raw number.
            snackbarHostState.showSnackbar(
                message = getString(message),
                duration = SnackbarDuration.Short,
            )
        }
    }

    public override fun onPause() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onPause $this")

        super.onPause()
    }

    override fun onDestroy() {
        if (DEBUG_LIFECYCLE) Logs.i("Lifecycle", "onDestroy $this")

        super.onDestroy()

        unregisterReceivers()
    }

    override fun finish() {
        unregisterReceivers()
        super.finish()
    }

    private fun unregisterReceivers() {

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

    /**
     * Launches the default, starting Activity for the NeverReader app.
     */
    fun startDefaultActivity() {
        val activity: Class<out Activity> = app()?.userManager()!!.defaultActivity
        startActivity(Intent(this, activity))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Every activity declares uiMode in its configChanges, so a system
        // dark-mode switch lands here rather than recreating the activity. This
        // is the only chance to re-resolve a theme that follows the system: the
        // preference flow does not emit, because the preference did not change.
        onThemeChanged(app()!!.theme().get(this))
    }

    companion object {
        const val DEBUG_LIFECYCLE: Boolean = false

        const val ACTION_SHUTDOWN: String = "com.ideashower.readitlater.ACTION_SHUTDOWN"

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
    }
}
