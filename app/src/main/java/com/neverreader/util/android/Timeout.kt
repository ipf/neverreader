package com.neverreader.util.android

import android.os.Handler
import android.os.Looper

/**
 * Simple helper class for a common pattern of having a runnable that is posted after a delay and can be canceled,
 * or restarted.
 *
 *
 * A delay of 0 will act like [Handler.post] without a delay.
 */
class Timeout constructor(
    runOnTimeout: TimeoutListener,
    timeoutMs: Long = 0,
    private val mHandler: Handler = Handler(Looper.getMainLooper())
) {
    private val mRunnable: Runnable

    private var mDelay: Long = 0

    /**
     * Uses the default App handler.
     *
     * @param runOnTimeout
     * @param timeoutMs
     */
    /**
     * Uses the default App handler with no delay. Be sure to use [.setDelay] or [.start]
     * @param runOnTimeout
     */
    init {
        mRunnable = object : Runnable {
            override fun run() {
                runOnTimeout.onTimeout(this@Timeout)
            }
        }
        setDelay(timeoutMs)
    }

    /**
     * Sets the delay that future calls to [.start] will use as a timeout delay.
     * @param delay In milliseconds
     */
    fun setDelay(delay: Long): Timeout {
        mDelay = delay
        return this
    }

    /**
     * Starts the timeout. The runnable will fire at the end of the specified delay.
     * If the timeout is already running, it will cancel the pending one and will restart.
     * If your constructor didn't set a delay, be sure to set one with either [.setDelay] or [.start].
     */
    fun start(delay: Long): Timeout {
        cancel()
        setDelay(delay)
        if (mDelay > 0) {
            mHandler.postDelayed(mRunnable, mDelay)
        } else {
            mHandler.post(mRunnable)
        }
        return this
    }

    /**
     * Starts the timeout. The runnable will fire at the end of the specified delay.
     * If the timeout is already running, it will cancel the pending one and will restart.
     * If your constructor didn't set a delay, be sure to set one with either [.setDelay] or [.start].
     */
    fun start(): Timeout {
        start(mDelay)
        return this
    }

    /**
     * Stops any pending timeouts. If none are pending nothing will happen. Don't worry about it, it's cool man.
     */
    fun cancel() {
        mHandler.removeCallbacks(mRunnable)
    }

    interface TimeoutListener {
        fun onTimeout(timeout: Timeout?)
    }
}
