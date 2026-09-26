package com.neverreader.sdk.util.wakelock

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.neverreader.app.ActivityMonitor
import com.neverreader.app.AppLifecycle
import com.neverreader.app.AppLifecycleEventDispatcher
import com.neverreader.util.android.ContextUtil.getActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This is a service that runs while [WakeLockManager] is locked. Its intention is to
 * give the Android system a Service as a sign we are still doing things so the App isn't killed if we are doing
 * work in the background when the activity is closed.
 *
 * REVIEW - do we want to have all major delegates in the app to manage their own services and wakelocks rather
 * than having a central one?
 *
 */
class WakefulAppService : Service() {
    @Singleton
    class Component @Inject constructor(
        activities: ActivityMonitor,
        wakelocks: WakeLockManager,
        @ApplicationContext context: Context,
        dispatcher: AppLifecycleEventDispatcher
    ) : AppLifecycle {
        private val context: Context
        private val activities: ActivityMonitor
        private var isWakeLocked = false
        private var isUserPresent = false
        private var isRunning = false

        init {
            dispatcher.registerAppLifecycleObserver<Component>(this)
            this.context = context
            this.activities = activities
            setWakeLocked(
                wakelocks.hasLocks(),
                context
            ) // By the time this is created, it might already have some locks.
            wakelocks.setListener(WakeLockManager.Listener { isLocked: Boolean ->
                setWakeLocked(
                    isLocked,
                    context
                )
            })
        }

        private fun setWakeLocked(value: Boolean, context: Context) {
            isWakeLocked = value
            invalidateService(context)
        }

        override fun onUserGone(context: Context?) {
            isUserPresent = false
            invalidateService(context!!)
        }

        override fun onUserPresent() {
            isUserPresent = true
            invalidateService(context)
        }

        private fun invalidateService(context: Context) {
            var context = context
            val run: Boolean
            if (isRunning) {
                // Continue running if there is a wakelock
                run = isWakeLocked
            } else {
                // Only start running if the user is present, otherwise Android Oreo will throw exceptions for background startService calls.
                // In theory, if we are running in the background, there should already be some basis for it like a service or job, so the service shouldn't be needed.
                // TODO eventually we should remove this class and have all individual components manage their own wakefulness.
                run = isWakeLocked && isUserPresent
            }

            val intent = Intent(context, WakefulAppService::class.java)

            if (run) {
                isRunning = true
                // When starting, try to use an activity context if available to better work with Android's background restrictions
                var activity = getActivity(context)
                activity = activity ?: activities.visible
                context = activity ?: context
                try {
                    context.startService(intent)
                } catch (ignore: Throwable) {
                    /*
						There is a bug in Android P where even though the app is resumed, this can crash with
						"Unable to resume activity ... Not allowed to start service Intent ... app is in background"
						https://rink.hockeyapp.net/manage/apps/207507/app_versions/213/crash_reasons/242641781
						https://issuetracker.google.com/issues/113122354

						In this case, we shouldn't crash. The app is in the foreground so we don't need the wakelock app service running quite yet
						anyways. When the user changes something with a wakelock or leaves the app we'll get another chance to start this service
						from setWakeLocked() or onUserGone().
					 */
                }
            } else {
                isRunning = false
                context.stopService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (WakeLockManager.Companion.DEBUG) WakeLockManager.Companion.log("service start")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (WakeLockManager.Companion.DEBUG) WakeLockManager.Companion.log("service destroy")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
