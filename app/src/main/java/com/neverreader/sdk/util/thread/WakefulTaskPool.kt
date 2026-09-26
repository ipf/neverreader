package com.neverreader.sdk.util.thread

import com.neverreader.sdk.util.wakelock.WakeLockHolder
import com.neverreader.sdk.util.wakelock.WakeLockManager
import com.neverreader.util.android.thread.TaskPool
import com.neverreader.util.java.Milliseconds.millisToMinutes
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A thread pool that maintains a wake lock while it is running or has pending tasks.
 *
 * @see WakeLockManager
 */
open class WakefulTaskPool : TaskPool {
    private var mWakeLockHolder: WakeLockHolder? = null

    constructor(wakelocks: WakeLockManager, fixedSize: Int, poolName: String?) : super(
        fixedSize,
        poolName
    ) {
        init(wakelocks)
    }

    constructor(
        wakelocks: WakeLockManager,
        corePoolSize: Int,
        maximumPoolSize: Int,
        poolName: String?
    ) : super(corePoolSize, maximumPoolSize, poolName) {
        init(wakelocks)
    }

    constructor(
        wakelocks: WakeLockManager?,
        corePoolSize: Int,
        maximumPoolSize: Int,
        workQueue: BlockingQueue<Runnable>?,
        poolName: String?
    ) : super(corePoolSize, maximumPoolSize, workQueue, poolName) {
        init(wakelocks)
    }

    constructor(
        wakelocks: WakeLockManager?,
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?,
        poolName: String?
    ) : super(corePoolSize, maximumPoolSize, keepAliveTime, unit,
        workQueue, poolName) {
        init(wakelocks)
    }

    private fun init(wakelocks: WakeLockManager?) {
        mWakeLockHolder = WakeLockHolder.withTimeout(name!!, 30, 50, WakeLockHolder.OnTimeout {
            StringBuilder()
                .append(" pk:").append(queue.peek())
                .append(" qs:").append(queue.size)
                .append(" ac:").append(activeCount)
                .append(" cc:").append(completedTaskCount)
                .append(" tc:").append(taskCount)
                .append(" ls:")
                .append(millisToMinutes(this@WakefulTaskPool.millisSinceLastSubmit()))
                .append(" hw:").append(this@WakefulTaskPool.hasWork())
                .append(" ia:").append(this@WakefulTaskPool.isActive)
                .append(" ip:").append(this@WakefulTaskPool.isPaused()).toString()
        })

        addExecutionStateChangeListener(object : ExecutionStateChangeListener() {
            override fun onTaskPoolExecutionStateChanged(isExecuting: Boolean) {
                if (isExecuting) {
                    wakelocks?.acquire(mWakeLockHolder!!)
                } else {
                    wakelocks?.release(mWakeLockHolder!!)
                }
            }
        })
    }
}

