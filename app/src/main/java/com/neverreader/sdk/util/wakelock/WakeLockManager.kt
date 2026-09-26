package com.neverreader.sdk.util.wakelock

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.neverreader.app.AppLifecycle
import com.neverreader.app.AppLifecycleEventDispatcher
import com.neverreader.util.java.Clock
import com.neverreader.util.java.Logs.v
import com.neverreader.util.java.Milliseconds.minutesToMillis
import com.neverreader.util.java.Milliseconds.seconds
import com.neverreader.util.java.Milliseconds.toSeconds
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages acquiring and releasing wakelocks in a way that encourages good behaviour,
 * but can catch when a wake lock is accidentally held too long and release it before
 * it causes problems for the user, earns us bad reviews or "bad behaviour" on Google Play's vitals.
 *
 *
 * It also catches cases where locks are held too long and reports them to us proactively,
 * with a bunch of helpful debugging info included, rather than us having to wait for users to
 * report it or us having to figure out some way to debug it.
 *
 *
 * All wake locks in the NeverReader app should use this class to benefit from this!
 *
 *
 * If this class is successful, our users will have happy batteries,
 * we'll catch wake lock bugs earlier and fix them easily and we'll have
 * low or no sessions with long held locks in Google Play vitals.
 *
 *
 * Use [.acquire] to obtain a wakelock and [.release] when you are done.
 *
 *
 * Implementation Notes:
 *
 *  *
 * All locks here are [PowerManager.PARTIAL_WAKE_LOCK].
 *
 *  *
 * Under the hood, we don't really create a real actual wakelock until the app goes into the background.
 * While the app is open, it is likely going to rapidly go through tasks that call acquire/release,
 * So only hitting the power manager when it matters (in the background) helps reduce unneeded work.
 *
 *  *
 * Internally, after you request a wakelock to be released, we'll actually hold it for a few seconds,
 * this gives tasks a very small window to launch the next one without losing power.
 *
 *
 */
@Singleton
class WakeLockManager @Inject constructor(
    @ApplicationContext context: Context,
    dispatcher: AppLifecycleEventDispatcher
) : AppLifecycle {
    /**
     * Currently held locks.
     */
    private val locks: MutableMap<WakeLockHolder?, Lock> = HashMap<WakeLockHolder?, Lock>()

    /**
     * Holds a lock while the app is in the foreground.
     * Mostly a simple way to provide the app at least [.RELEASE_BUFFER] seconds
     * to obtain locks after going into the background. This was functionality
     * provided in older versions of this class. I think the idea is that something
     * that triggers asynchronously during onPause might not acquire their lock during onPause
     * so this gives them a small window to do so.
     * REVIEW whether or not we really need this buffer. How quickly does Android shut us down after onPause and onStop anyways?
     */
    private val foregroundHolder: WakeLockHolder =
        WakeLockHolder.Companion.withTimeout("app", 0, 1, null)

    private val clock = Clock.ELAPSED_REALTIME
    private val handler: Handler
    private val power: PowerManager
    private var isHoldingLocks = false
    private var isInBackground = true
    private var listener: Listener? = null

    init {
        dispatcher.registerAppLifecycleObserver<WakeLockManager>(this)
        this.power = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
        this.handler = Handler(Looper.getMainLooper())
    }

    /**
     * Acquires a wake lock and holds it until [.release]
     */
    @Synchronized
    fun acquire(holder: WakeLockHolder) {
        var lock = locks[holder]
        if (lock != null) {
            // Already exists
            // Can keep existing state unless it is in a release buffer, for that, reactivate it.
            if (lock.state == LockState.RELEASE_BUFFER) {
                if (DEBUG) log("reactivate", holder)
                lock.cancelBuffer()
                lock.activate()
            }
        } else {
            // Create and activate
            if (DEBUG) log("create", holder)
            lock = Lock(holder)
            locks[holder] = lock
            lock.activate()

            invalidateListener()
        }
    }

    /**
     * Release a [WakeLockHolder]'s lock.
     * It is safe to call this even if you already have released it.
     * It will just ignore duplicate or redundant calls.
     */
    @Synchronized
    fun release(holder: WakeLockHolder?) {
        val lock = locks[holder]
        if (lock != null) {
            if (DEBUG) log("release", holder)


            // Release it after the buffer time
            lock.bufferRelease(Runnable {
                synchronized(this@WakeLockManager) {
                    if (DEBUG) log("released after buffer", holder)
                    lock.release()
                    locks.remove(holder)
                    invalidateListener()
                }
            })
        }
    }

    /**
     * A listener for when there are active holds.
     */
    @Synchronized
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    @Synchronized
    fun hasLocks(): Boolean {
        return isHoldingLocks
    }

    fun interface Listener {
        fun onWakeLockStateChanged(isLocked: Boolean)
    }

    override fun onUserPresent() {
        synchronized(this) {
            if (DEBUG) log("foreground")
            this.isInBackground = false
            for (lock in locks.values) {
                lock.foreground()
            }
            acquire(foregroundHolder)
        }
    }

    override fun onUserGone(context: Context?) {
        synchronized(this) {
            if (DEBUG) log("background")
            this.isInBackground = true
            val now = clock.now()
            for (lock in locks.values) {
                lock.background(now)
            }
            release(foregroundHolder)
        }
    }

    @Synchronized
    private fun invalidateListener() {
        val hasLocks = !locks.isEmpty()
        if (hasLocks != isHoldingLocks) {
            isHoldingLocks = hasLocks
            if (DEBUG) log(if (isHoldingLocks) "HAS LOCKS" else "NO LOCKS")
            if (listener != null) {
                // TODO cycles pretty quickly, maybe have a timeout on this  one?
                listener!!.onWakeLockStateChanged(isHoldingLocks)
            }
        }
    }

    private enum class LockState {
        /**
         * While the app is in the foreground, we note that a wakelock is requested,
         * but don't actually hold on.
         */
        FOREGROUND,

        /**
         * While the app is in the background, we hold a wake lock.
         */
        BACKGROUND,

        /**
         * The lock was requested to be released, but we are waiting [.RELEASE_BUFFER] seconds
         * to see if it reactivates before we unlock and release it.
         */
        RELEASE_BUFFER,

        /**
         * The lock is unlocked and fully released.
         */
        RELEASED
    }

    private inner class Lock(val holder: WakeLockHolder) {
        val created: Long

        var state: LockState? = null

        var lock: WakeLock? = null
        var timeBackgrounded: Long = 0

        var warning: Runnable? = null
        var timeout: Runnable? = null
        var releaseBuffer: Runnable? = null
        var liveliness: Runnable? = null

        init {
            this.created = clock.now()
        }

        /**
         * Set it into the correct state based on whether the app is currently foreground or background.
         */
        fun activate() {
            if (isInBackground) {
                background(if (timeBackgrounded > 0) timeBackgrounded else created)
            } else {
                foreground()
            }
        }

        /**
         * Set lock into foreground mode.
         */
        fun foreground() {
            state = LockState.FOREGROUND
            timeBackgrounded = 0
            clearTimeouts()
            unlock()
        }

        /**
         * Set lock into background mode.
         */
        fun background(start: Long) {
            state = LockState.BACKGROUND
            timeBackgrounded = start
            lock()


            // Setup fail safes
            if (holder.stopTimeout > 0) {
                // Timeout Based
                timeout = Runnable { this.timeout() }
                handler.postDelayed(timeout!!, minutesToMillis(holder.stopTimeout))

                if (holder.warnTimeout > 0 && REPORT_WARNINGS) {
                    warning = Runnable {
                        synchronized(this@WakeLockManager) {
                            if (DEBUG) log("warn", holder)
                        }
                    }
                    handler.postDelayed(warning!!, minutesToMillis(holder.warnTimeout))
                }
            } else if (holder.check != null) {
                // Liveliness Based
                liveliness = Runnable {
                    synchronized(this@WakeLockManager) {
                        if (DEBUG) log("liveliness", holder)
                        if (holder.check.keepAlive()) {
                            handler.postDelayed(liveliness!!, minutesToMillis(holder.checkInterval))
                        } else {
                            timeout()
                        }
                    }
                }
                handler.postDelayed(liveliness!!, minutesToMillis(holder.checkInterval))
            }
        }

        /**
         * The lock timed out, quietly log an error and release it.
         */
        fun timeout() {
            synchronized(this@WakeLockManager) {
                if (DEBUG) log("timeout", holder)
                if (REPORT_TIMEOUTS) {
                }
                release()
                locks.remove(holder)
            }
        }

        /**
         * Release after [.RELEASE_BUFFER] seconds.
         */
        fun bufferRelease(afterBuffer: Runnable?) {
            state = LockState.RELEASE_BUFFER
            if (releaseBuffer == null) {
                releaseBuffer = afterBuffer
                clearTimeouts()
                handler.postDelayed(releaseBuffer!!, seconds(RELEASE_BUFFER))
            }
        }

        /**
         * Cancel releasing started by [.bufferRelease].
         */
        fun cancelBuffer() {
            if (releaseBuffer != null) {
                handler.removeCallbacks(releaseBuffer!!)
                releaseBuffer = null
            }
        }

        /**
         * Fully release/unlock the lock.
         * Locks are not intended to be reused after this point.
         */
        fun release() {
            state = LockState.RELEASED
            unlock()
        }

        /**
         * Cancel all future timeouts or liveliness checks.
         */
        fun clearTimeouts() {
            if (warning != null) {
                handler.removeCallbacks(warning!!)
                warning = null
            }
            if (timeout != null) {
                handler.removeCallbacks(timeout!!)
                timeout = null
            }
            if (liveliness != null) {
                handler.removeCallbacks(liveliness!!)
                liveliness = null
            }
        }

        /**
         * Actually obtain the real wake lock.
         */
        fun lock() {
            if (lock == null) {
                if (DEBUG) log("lock", holder)
                lock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, holder.name)
                lock!!.acquire()
            }
        }

        /**
         * Actually release the real wake lock.
         */
        fun unlock() {
            if (lock != null) {
                if (DEBUG) log("unlock", holder)
                lock!!.release()
                lock = null
            }
        }
    }

    internal class WakeLockException(details: String?) : RuntimeException(details) {
        companion object {
            private fun newException(
                type: String?,
                holder: WakeLockHolder,
                lock: Lock,
                now: Long
            ): WakeLockException {
                var details = type
                details += holder.name
                details += " cr:" + toSeconds(now - lock.created) // Seconds since first created
                details += " bg:" + toSeconds(now - lock.timeBackgrounded) // Seconds in background
                details += if (holder.onTimeout != null) StringUtils.defaultIfBlank<String?>(
                    holder.onTimeout.onTimedOut(),
                    ""
                ) else ""
                return WakeLockException(details)
            }
        }
    }

    companion object {
        const val DEBUG: Boolean = false
        const val REPORT_WARNINGS: Boolean = false
        const val REPORT_TIMEOUTS: Boolean = false

        /**
         * To help smooth out locks that might quickly acquire and release rapidly (like task pools),
         * Locks will wait this number of seconds, after being requested to release, before internally releasing
         * the actual framework wake lock.
         * This also helps reduce rapid starting and stopping of the [WakefulAppService], WakeLocks
         * and also gives tasks a short buffer of time to trigger another task that might need a lock.
         */
        private const val RELEASE_BUFFER = 5

        fun log(log: String?) {
            log(log, null)
        }

        private fun log(log: String?, holder: WakeLockHolder?) {
            if (DEBUG) {
                v("WakeLockManager", log + " " + (if (holder != null) "~" + holder.name else ""))
            }
        }
    }
}
