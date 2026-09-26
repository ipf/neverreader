package com.neverreader.sdk.util.thread

import com.neverreader.sdk.util.wakelock.WakeLockManager
import com.neverreader.util.android.thread.PriorityFutureTask
import com.neverreader.util.android.thread.PriorityFutureTaskComparator
import com.neverreader.util.android.thread.TaskPoolFuture
import com.neverreader.util.android.thread.TaskRunnable
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A task pool that supports [PriorityFutureTask]'s priority as a basis for which task to
 * run next out of the queue.
 */
class PriorityTaskPool : WakefulTaskPool {
    private var mCreatedCount = 0

    constructor(wakelocks: WakeLockManager?, fixedPoolSize: Int, poolName: String?) : super(
        wakelocks,
        fixedPoolSize,
        fixedPoolSize,
        PriorityBlockingQueue<Runnable>(11, PriorityFutureTaskComparator()),
        poolName
    )

    constructor(
        wakelocks: WakeLockManager?,
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        poolName: String?
    ) : super(
        wakelocks,
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        PriorityBlockingQueue<Runnable>(11, PriorityFutureTaskComparator()),
        poolName
    )

    override fun newFutureTask(runnable: TaskRunnable?): TaskPoolFuture {
        return PriorityFutureTask(runnable, (++mCreatedCount).toLong())
    }
}
