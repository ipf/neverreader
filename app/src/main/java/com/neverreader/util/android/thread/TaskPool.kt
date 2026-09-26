package com.neverreader.util.android.thread

import android.os.SystemClock
import com.neverreader.util.java.Logs
import java.util.concurrent.BlockingQueue
import java.util.concurrent.FutureTask
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * A thread pool executor that provides a bulk cancel function and can be paused, resumed and monitored.
 *
 *  * Submit/queue tasks with [.submit] instead of the default execute and submit methods.
 *  * Cancel all tasks with [.cancelAll].
 *  * Pause and Resume with [.pause] and [.resume].
 *  * Terminate the entire pool with [.terminate].
 *
 *
 *
 * This class is thread safe.
 *
 *
 * Developer Note: When a task is bulk canceled, it remains in the queue and will
 * actually execute, but since it is flagged as cancelled, it will not perform its
 * function and will quickly be discarded.
 *
 *
 * NeverReader Usage: For the NeverReader app this serves as a replacement for AsyncTask and
 * most thread pools. It allows fine control over cancelling and stopping all async tasks
 * at logout or for specific cases like stopping offline downloading.
 */
open class TaskPool protected constructor(
    corePoolSize: Int,
    maximumPoolSize: Int,
    keepAliveTime: Long,
    unit: TimeUnit?,
    workQueue: BlockingQueue<Runnable>?,
    poolName: String?
) : PausableThreadPoolExecutor(
    corePoolSize,
    maximumPoolSize,
    keepAliveTime,
    unit,
    workQueue,
    poolName
), PausableThreadPoolExecutor.ExecutionListener {
    private val mLock = Any()

    private var mStatus = Status.ACTIVE

    private enum class Status {
        ACTIVE,
        TERMINATING,
        TERMINATED
    }

    /**
     * The timestamp (elapsedRealtime) of when the last/latest task was submitted to this pool,
     * or zero if never.
     */
    private var mLastSubmit: Long = 0

    /**
     * Tasks that are active, meaning queued or executing, and haven't been bulk cancelled.
     * Tasks are added to these when submitted and are removed if bulk canceled or completed.
     * This is not used as a queue, only to track status and to reference for bulk canceling.
     */
    private val mActiveTasks: MutableList<TaskPoolFuture> = ArrayList<TaskPoolFuture>()

    private val mExecutionStateChangeListeners = ArrayList<ExecutionStateChangeListener>()
    private var mAfterExecutionListener: AfterExecutionListener? = null

    constructor(fixedSize: Int, poolName: String?) : this(fixedSize, fixedSize, poolName)

    constructor(corePoolSize: Int, maximumPoolSize: Int, poolName: String?) : this(
        corePoolSize,
        maximumPoolSize,
        LinkedBlockingQueue<Runnable>(),
        poolName
    )

    protected constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        workQueue: BlockingQueue<Runnable>?,
        poolName: String?
    ) : this(corePoolSize, maximumPoolSize, 1, TimeUnit.SECONDS, workQueue, poolName)

    init {
        setExecutionListener(this)
    }


    override fun execute(command: Runnable) {
        // This is a catch all for anything that submits via the public ThreadPoolExecutor interface, so all calls run through submit(TaskRunnable) instead.
        submit(TaskRunnable.simple(TaskRunnable.ThrowingRunnable { command.run() }))
    }

    /**
     * Submit a task to the queue to be executed.
     * @param runnable A [TaskRunnable] containing the future work to complete.
     * @return A future or null if this pool is terminated and no longer accepting tasks
     */
    fun submit(runnable: TaskRunnable): FutureTask<Any?>? {
        synchronized(mLock) {
            if (mStatus != Status.ACTIVE) {
                return null
            }
            mLastSubmit = SystemClock.elapsedRealtime()

            val futureTask = newFutureTask(runnable)
            mActiveTasks.add(futureTask!!)

            runnable.onTaskPoolSubmit(this, futureTask)
            super.execute(futureTask)
            invalidateExecutionState()
            return futureTask
        }
    }

    /**
     * Create a wrapper for this runnable.
     * Provided for subclasses to override as needed
     */
    protected open fun newFutureTask(runnable: TaskRunnable?): TaskPoolFuture? {
        return TaskPoolFuture(runnable)
    }

    /**
     * Cancel all tasks currently being executed and in the queue.
     * For executing tasks, it only invokes [TaskRunnable.cancel] and does not try to interrupt the thread.
     * This does not shutdown the queue.
     * Any new submissions after this call will be accepted.
     */
    fun cancelAll() {
        cancelAll(false)
    }

    /**
     * Like [.cancelAll] but also blocks until the queue is empty.
     * If new tasks are added during this wait it will also cancel those and will extend the wait time.
     */
    fun cancelAllUntilEmpty() {
        cancelAll(true)
    }

    private fun cancelAll(await: Boolean) {
        val futures: MutableList<TaskPoolFuture> = ArrayList<TaskPoolFuture>()
        synchronized(mLock) {
            for (r in mActiveTasks) {
                if (await) futures.add(r)
                try {
                    r.bulkCancel()
                } catch (t: Throwable) {
                    // Don't let an outside implementation break our flow here.
                    Logs.printStackTrace(t)
                }
            }
            mActiveTasks.clear()
            invalidateExecutionState()
        }
        if (await && !futures.isEmpty()) {
            for (r in futures) {
                try {
                    r.get()
                } catch (ignore: Throwable) {
                }
            }
            cancelAll(true) // Loop until we didn't have to cancel/await anything. This catches tasks added during our wait time.
        }
    }

    override fun beforeExecution(r: Runnable?) {}

    override fun afterExecution(r: Runnable?) {
        val f = r as TaskPoolFuture
        val wasBulkCanceled: Boolean

        synchronized(mLock) {
            mActiveTasks.remove(f)
            wasBulkCanceled = f.wasBulkCanceled()
            invalidateExecutionState()
        }

        if (mAfterExecutionListener != null) {
            try {
                mAfterExecutionListener!!.afterExecution(wasBulkCanceled)
            } catch (t: Throwable) {
                // Don't let a bad callback break this classes functionality and flow
                Logs.printStackTrace(t)
            }
        }
    }

    /**
     * Stop receiving new tasks and wait (blocking) for currently queue'd tasks to complete (up to the provided timeout)
     * afterwards, it kills the underlying pool. This pool is no longer useable and should be discarded.
     *
     * @param timeout how long to wait to allow the already queue'd work to complete before discarding and terminating.
     * @param unit The timeout's time unit.
     */
    fun terminate(timeout: Int, unit: TimeUnit?) {
        synchronized(mLock) {
            mStatus = Status.TERMINATING
            shutdown()
        }

        try {
            awaitTermination(timeout.toLong(), unit)
        } catch (e: InterruptedException) {
            Logs.printStackTrace(e)
        }

        synchronized(mLock) {
            cancelAll()
            mStatus = Status.TERMINATED
            shutdownNow()
            invalidateExecutionState()
        }
    }

    val isActive: Boolean
        get() {
            synchronized(mLock) {
                return mStatus == Status.ACTIVE
            }
        }

    // TODO pausing should have a timeout to avoid accidently holding work indefinitely.
    override fun pause() {
        synchronized(mLock) {
            super.pause()
        }
    }

    override fun resume() {
        synchronized(mLock) {
            super.resume()
        }
    }

    override fun isPaused(): Boolean {
        synchronized(mLock) {
            return super.isPaused()
        }
    }

    /**
     * true when this pool is actively working on a task and/or has pending  (non-cancelled) work in its queue.
     */
    fun hasWork(): Boolean {
        synchronized(mLock) {
            return mStatus != Status.TERMINATED && !mActiveTasks.isEmpty()
        }
    }

    fun addExecutionStateChangeListener(listener: ExecutionStateChangeListener) {
        synchronized(mLock) {
            listener.isExecuting = hasWork()
            mExecutionStateChangeListeners.add(listener)
        }
    }

    private fun invalidateExecutionState() {
        synchronized(mLock) {
            val isExecuting = hasWork()
            for (listener in mExecutionStateChangeListeners) {
                if (listener.isExecuting != isExecuting) {
                    listener.isExecuting = isExecuting
                    try {
                        listener.onTaskPoolExecutionStateChanged(isExecuting)
                    } catch (t: Throwable) {
                        // Don't let an outside implementation break our flow
                        Logs.printStackTrace(t)
                    }
                }
            }
        }
    }

    interface AfterExecutionListener {
        fun afterExecution(wasBulkCanceled: Boolean)
    }

    abstract class ExecutionStateChangeListener {
        /** The last execution state sent to the listener, or its initial state when registered to listen.  */
        internal var isExecuting = false

        abstract fun onTaskPoolExecutionStateChanged(isExecuting: Boolean)
    }

    fun millisSinceLastSubmit(): Long {
        if (mLastSubmit == 0L) {
            return 0
        } else {
            return SystemClock.elapsedRealtime() - mLastSubmit
        }
    }
}
