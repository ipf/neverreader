package com.neverreader.util.android

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.widget.Toast
import com.neverreader.app.App.Companion.getContext as appContextFn
import com.neverreader.app.R
import com.neverreader.util.android.IntentUtils2.isIntentUsable
import com.neverreader.util.java.StringUtils2

object IntentUtils {
    /**
     * Convenience method for checking if an Intent that starts an Activity will actually
     * find an Activity to open or not.
     *
     * @param context
     * @param intent
     * @return
     */
    fun isActivityIntentAvailable(context: Context, intent: Intent): Boolean {
        val packageManager = context.packageManager
        val list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return list.isNotEmpty()
    }

    /**
     * Returns all apps that can accept this intent. If there is a preferred one it will be the first in the list.
     *
     * @param context
     * @return Always returns a list, never null, though it may be empty.
     */
    fun getMatchingApps(intent: Intent, context: Context): MutableList<ResolveInfo> {
        val pm = context.packageManager
        val apps = pm.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_ALL
        )

        if (apps.size > 1) {
            var preferred: ResolveInfo? = null
            val filters: MutableList<IntentFilter?> = ArrayList<IntentFilter?>()
            val activities: MutableList<ComponentName?> = ArrayList<ComponentName?>()
            for (app in apps) {
                filters.clear()
                activities.clear()
                pm.getPreferredActivities(filters, activities, app.activityInfo.packageName)
                if (activities.isNotEmpty()) {
                    preferred = app
                    break
                }
            }

            if (preferred != null) {
                apps.remove(preferred)
                apps.add(0, preferred)
            }
        }

        return apps
    }

    /**
     * Returns all apps that are likely a browser. If there is a preferred one, it will be the first in the list.
     *
     * @param context
     * @return A list with any browsers found. Never null.
     */
    fun getAllAvailableBrowsers(context: Context): MutableList<ResolveInfo> {
        return getMatchingApps(
            Intent(Intent.ACTION_VIEW, Uri.parse("http://ideashower.com")),
            context
        )
    }

    fun isAppInstalled(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        var info: PackageInfo?
        try {
            info = pm.getPackageInfo(packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            info = null
        }

        return info != null
    }

    /**
     * Start an intent safely without risking crashing if there are no matching apps available.
     * A message will be toasted in that case.
     *
     *
     * Also automatically adds the NEW_TASK flag if the context is a service.
     *
     * @param context The context to start the activity with.
     * @param intent The intent to start.
     * @param clearWhenTaskReset Whether or not to add [Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET]. Should be true for most cases when opening other apps. This will ensure that NeverReader is still in the recents and is reopened rather than this new app.
     * @return true if started, false if the error was shown.
     */
    fun safeStartActivity(context: Context, intent: Intent, clearWhenTaskReset: Boolean): Boolean {
        if (clearWhenTaskReset) {
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET)
        }

        if (context is Service) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (isActivityIntentAvailable(context, intent)) {
            context.startActivity(intent)
            return true
        } else {
            Toast.makeText(context, R.string.ts_no_apps_for_intent, Toast.LENGTH_LONG)
                .show()
            return false
        }
    }

    val defaultBrowser: ComponentName?
        get() {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://")
            )
            val app =
                appContextFn()!!.packageManager.resolveActivity(intent, 0)
            return if (app != null) ComponentName(
                app.activityInfo.packageName,
                app.activityInfo.name
            ) else null
        }

    /**
     * Attempts to open the intent with the default browser or at least a common browser, avoiding re-opening
     * with our own app.
     *
     * @param context
     * @param viewIntent This must be a VIEW intent with an http/https uri as the data.
     * @return true if it found a browser to open with and it was started, false if it couldn't find an app to view it with.
     */
    fun openWithDefaultBrowser(
        context: Context,
        viewIntent: Intent,
        allowResolverActivity: Boolean
    ): Boolean {
        var viewIntent = viewIntent
        if (viewIntent.action != Intent.ACTION_VIEW || viewIntent.dataString == null) {
            return false
        }

        var componentName: ComponentName? = defaultBrowser
        if (componentName == null || componentName.packageName == context.packageName) {
            return false
        }

        if (!allowResolverActivity && componentName.className == "com.android.internal.app.ResolverActivity") {
            // Try to pick a default browser for them
            val browsers = getAllAvailableBrowsers(context)
            // First, try known ones like Chrome
            var found = false
            for (info in browsers) {
                if (StringUtils2.equalsIgnoreCaseOneOf(
                        info.activityInfo.packageName,
                        "com.android.chrome",
                        "com.chrome.beta"
                    )
                ) {
                    componentName =
                        ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    found = true
                    break
                }
            }
            if (!found && !browsers.isEmpty()) {
                // Just use the first one... I know.. i know it is janky. but this is the best we can do
                var info: ResolveInfo? = null
                for (browser in browsers) {
                    if (browser.activityInfo.packageName == appContextFn()!!.packageName) {
                        continue
                    } else {
                        info = browser
                        break
                    }
                }
                if (info != null) {
                    componentName =
                        ComponentName(info.activityInfo.packageName, info.activityInfo.name)
                    found = true
                }
            }
            if (!found) {
                // We couldn't do it.
                return false
            }
        }

        viewIntent = Intent(viewIntent).setComponent(componentName)

        if (isActivityIntentAvailable(context, viewIntent)) {
            context.startActivity(viewIntent)
            return true
        } else {
            return false
        }
    }

    val googleTranslateIntent: Intent
        get() {
            val intent = Intent()
            intent.action = Intent.ACTION_PROCESS_TEXT
            intent.setTypeAndNormalize("text/plain")
            intent.component = ComponentName(
                "com.google.android.apps.translate",
                "com.google.android.apps.translate.copydrop.gm3.TapToTranslateActivity"
            )
            return intent
        }

    fun hasGoogleTranslate(context: Context): Boolean {
        return isIntentUsable(
            context,
            googleTranslateIntent
        )
    }

}
