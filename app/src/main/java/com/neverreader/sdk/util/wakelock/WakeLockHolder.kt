package com.neverreader.sdk.util.wakelock

/**
 * A unique part of the app that uses a wakelock and logic for
 * how long it expects to use the lock so [WakeLockManager] can
 * catch case where this lock is held accidentally too long.
 *
 *
 * The point of all of this extra logic is to allow us to use wake locks
 *
 *
 * Use one of the static methods to create one.
 * The simplest and most common one is [.withTimeout]
 */
class WakeLockHolder {
    val name: String
    val warnTimeout: Int
    val stopTimeout: Int
    val onTimeout: OnTimeout?
    val checkInterval: Int
    val check: LivelinessCheck?

    private constructor(name: String, warnTimeout: Int, stopTimeout: Int, onTimeout: OnTimeout?) {
        require(name.isNotEmpty()) { "Name must not be empty and must be unique." }
        require(stopTimeout >= 1) { "All wakelocks must have a stopTimeout > 0" }
        this.name = name
        this.warnTimeout = warnTimeout
        this.stopTimeout = stopTimeout
        this.onTimeout = onTimeout
        this.checkInterval = 0
        this.check = null
    }

    private constructor(
        name: String,
        checkInterval: Int,
        check: LivelinessCheck,
        onTimeout: OnTimeout?
    ) {
        require(name.isNotEmpty()) { "Name must not be empty and must be unique." }
        require(checkInterval >= 1) { "must supply a check interval" }
        this.name = name
        this.warnTimeout = 0
        this.stopTimeout = 0
        this.onTimeout = onTimeout
        this.checkInterval = checkInterval
        this.check = check
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as WakeLockHolder
        return name == that.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    fun interface OnTimeout {
        /**
         * Your wakelock was held longer than you expected.
         * Return a string with any additional data that will be useful to debug this.
         * Do not include user or personal data.
         * @param
         */
        fun onTimedOut(): String?
    }

    interface LivelinessCheck {
        /**
         * Fired each interval.
         * @return true if the lock has been held longer than expected and should time out, false to keep it going.
         */
        fun keepAlive(): Boolean
    }

    companion object {
        /**
         * Uses a timeout for safety.
         *
         *
         * Note: These timeouts are not meant for functional usage. You should make sure you always
         * release your locks. This is just safety fallback to avoid accidental holding indefinite or long locks.
         *
         *
         * Note: These timeouts only apply to time spent in the background.
         * Each time the user leaves the app, timeouts restart from 0.
         *
         * @param name A name (should be unique app wide) that indicates who/what component needs the lock.
         * Be aware users may see this name device settings or in reports they generate.
         * This name is used to determine equality, so instances with the same name are considered
         * the same holder.
         *
         * @param warnTimeout Optional. (If you don't need this, pass 0)
         * If the lock is held longer than this number of minutes,
         * it will quietly log but allow the lock to continue to be held.
         * Use this for "I don't think it should  be held this long, but if it is I'd like to know about it, but not release the lock."
         *
         * @param stopTimeout Required.
         * If the lock is held longer than this number of minutes,
         * it will quietly log and automatically release it.
         * Google Play perceives a held wake lock of 1 hour as bad behaviour,
         * so it is encouraged to use a value less than 60.
         * If you need a long or indefinite lock (like for audio playback)
         * use [.withLivinessCheck] instead.
         *
         * @param onTimeout Optional, if you want to provide some additional debug info for logs if it timed out.
         */
        fun withTimeout(
            name: String,
            warnTimeout: Int,
            stopTimeout: Int,
            onTimeout: OnTimeout?
        ): WakeLockHolder {
            return WakeLockHolder(name, warnTimeout, stopTimeout, onTimeout)
        }

        /**
         * Checks some condition on an interval to make sure it isn't being held to long.
         * Useful for indefinite or long locks like audio playback.
         *
         *
         * Note: This check is not meant to be functional. You should make sure you always
         * release your locks. This is just safety fallback to avoid accidental holding indefinite or long locks.
         *
         *
         * In the example of audio playback, the LivelinessCheck can keep track of the state
         * of playback, if it notices that it hasn't changed since the last interval, it can
         * assume the lock has been accidentally held too long and can be released.
         *
         * @param name A name (should be unique app wide) that indicates who/what component needs the lock.
         * Be aware users may see this name device settings or in reports they generate.
         * This name is used to determine equality, so instances with the same name are considered
         * the same holder.
         * @param checkInterval In minutes, how often to run the [LivelinessCheck].
         * @param check The logic to run on each interval to check if the lock has accidentally been held too long.
         * @param onTimeout Optional, if you want to provide some additional debug info for logs if it timed out.
         */
        fun withLivelinessCheck(
            name: String,
            checkInterval: Int,
            check: LivelinessCheck,
            onTimeout: OnTimeout?
        ): WakeLockHolder {
            return WakeLockHolder(name, checkInterval, check, onTimeout)
        }
    }
}
