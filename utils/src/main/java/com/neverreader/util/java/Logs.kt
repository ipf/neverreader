package com.neverreader.util.java

import com.neverreader.app.AppMode
import org.apache.commons.lang3.StringUtils

/**
 * Utility for logging and controlling logging in various builds.
 * Setup [.logger] and [.mode] at the startup of your app,
 */
object Logs {

    val OFF: Logger = object : Logger {
        override fun v(tag: String?, log: String?) {}
        override fun w(tag: String?, log: String?) {}
        override fun e(tag: String?, log: String?) {}
        override fun i(tag: String?, log: String?) {}
        override fun d(tag: String?, log: String?) {}
        override fun printStackTrace(t: Throwable?) {}
    }

    private var logger: Logger = OFF
    private var mode: AppMode? = null

    /** What mode to use for methods like [.throwIfNotProduction]  */
    fun mode(value: AppMode?) {
        mode = value
    }

    fun v(tag: String?, log: String?) {
        logger.v(tag, log)
    }

    fun d(tag: String?, log: String?) {
        logger.d(tag, log)
    }

    fun i(tag: String?, log: String?) {
        logger.i(tag, log)
    }

    fun w(tag: String?, log: String?) {
        logger.w(tag, log)
    }

    fun e(tag: String?, log: String?) {
        logger.e(tag, log)
    }

    fun l(string: String?) {
        v("ReadItLater", string)
    }

    fun printStackTrace(t: Throwable) {
        t.printStackTrace()
    }

    /**
     * Convenience method for throwing a RuntimeException ONLY if the app is in beta or development mode.
     *
     *
     * If this unexpected state is safe to ignore in production, this can be used to inform developers of unexpected states without
     * causing a crash for production users if for some reason it occurs.
     *
     *
     * If no mode has been set via [.mode], this will act as if the mode is production.
     */
    fun throwIfNotProduction(string: String?) {
        if (mode != null && mode!!.isForInternalCompanyOnly) {
            throw RuntimeException(StringUtils.defaultString(string))
        }
    }


    interface Logger {
        fun v(tag: String?, log: String?)
        fun w(tag: String?, log: String?)
        fun e(tag: String?, log: String?)
        fun i(tag: String?, log: String?)
        fun d(tag: String?, log: String?)
        fun printStackTrace(t: Throwable?)
    }
}
