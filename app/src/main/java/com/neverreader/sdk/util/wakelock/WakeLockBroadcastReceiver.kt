package com.neverreader.sdk.util.wakelock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.neverreader.app.App.Companion.from

/**
 * A receiver that holds a wake lock during its onReceive method.
 */
abstract class WakeLockBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val holder: WakeLockHolder =
            WakeLockHolder.Companion.withTimeout(javaClass.simpleName, 0, 1, null)
        from(context).wakelocks().acquire(holder)
        doOnReceive(context, intent)
        from(context).wakelocks().release(holder)
    }

    abstract fun doOnReceive(context: Context?, intent: Intent?)
}
