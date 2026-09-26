package com.neverreader.util.java

import android.os.SystemClock

/**
 * An injectable wrapper around System.currentTimeMillis().
 */
fun interface Clock {
    fun now(): Long

    companion object {
        val SYSTEM: Clock = Clock { System.currentTimeMillis() }
        val ELAPSED_REALTIME: Clock = Clock { SystemClock.elapsedRealtime() }
    }
}
