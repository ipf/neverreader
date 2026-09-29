package com.neverreader.util.android

import android.os.Process
import java.util.concurrent.ThreadFactory

/**
 * A thread factory that ensures it runs with the recommended background thread priority in Android
 */
class AndroidBgThreadFactory constructor(private val name: String? = null) :
    ThreadFactory {
    private var count = 0

    override fun newThread(r: Runnable): Thread {
        val thread: Thread = object : Thread() {
            override fun run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                r.run()
            }
        }
        if (name != null) thread.name = name + "-" + count++
        return thread
    }

    companion object {
        fun wrap(factory: ThreadFactory, name: String?): ThreadFactory {
            return ThreadFactory { r: Runnable? ->
                val t = factory.newThread(Runnable {
                    Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                    r!!.run()
                })
                if (name != null) t.name = name
                t
            }
        }
    }
}
