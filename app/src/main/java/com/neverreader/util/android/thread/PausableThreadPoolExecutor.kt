package com.neverreader.util.android.thread

import com.neverreader.util.android.AndroidBgThreadFactory
import com.neverreader.util.java.Logs
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.ReentrantLock

/**
 * A [ThreadPoolExecutor] that lets you pause work in its queue. See [.pause]
 * Also provides a [.setExecutionListener]
 */
open class PausableThreadPoolExecutor : ThreadPoolExecutor {
    private val isPaused = AtomicBoolean(false)
    private val pauseLock = ReentrantLock()
    private val unpaused: Condition = pauseLock.newCondition()
    val name: String?
    private var mExecutionListener: ExecutionListener? = null

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?,
        poolName: String?
    ) : super(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        workQueue,
        AndroidBgThreadFactory(poolName)
    ) {
        this.name = poolName
    }

    constructor(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?,
        workQueue: BlockingQueue<Runnable>?,
        factory: ThreadFactory,
        poolName: String?
    ) : super(
        corePoolSize,
        maximumPoolSize,
        keepAliveTime,
        unit,
        workQueue,
        AndroidBgThreadFactory.wrap(factory, poolName)
    ) {
        this.name = poolName
    }

    override fun beforeExecute(t: Thread, r: Runnable?) {
        if (mExecutionListener != null) mExecutionListener!!.beforeExecution(r)

        super.beforeExecute(t, r)

        pauseLock.lock()
        try {
            while (isPaused.get()) {
                unpaused.await()
            }
        } catch (ie: InterruptedException) {
            t.interrupt()
        } finally {
            pauseLock.unlock()
        }
    }

    override fun afterExecute(r: Runnable?, t: Throwable?) {
        var t = t
        if (mExecutionListener != null) mExecutionListener!!.afterExecution(r)

        super.afterExecute(r, t)

        if (t == null && r is Future<*>) {
            try {
                (r as Future<*>).get()
            } catch (ce: CancellationException) {
                t = ce
            } catch (ee: ExecutionException) {
                t = ee.cause
            } catch (ie: InterruptedException) {
                Thread.currentThread().interrupt() // ignore/reset
            }
        }
        if (t != null) {
            Logs.printStackTrace(t)
        }
    }

    /**
     * Pauses work in this thread pool.
     *
     * Tasks that have already begun will be completed but
     * nothing will be taken from the queue to run until after [.resume] is invoked.
     *
     * While paused you may continue to add tasks to the queue.
     */
    open fun pause() {
        if (!isPaused.get()) {
            pauseLock.lock()
            try {
                isPaused.set(true)
            } finally {
                pauseLock.unlock()
            }
        }
    }

    /**
     * Resume running tasks in the queue.
     */
    open fun resume() {
        if (isPaused.get()) {
            pauseLock.lock()
            try {
                isPaused.set(false)
                unpaused.signalAll()
            } finally {
                pauseLock.unlock()
            }
        }
    }

    open fun isPaused(): Boolean {
        return isPaused.get()
    }

    /**
     * Shutdown will also [.resume] if paused, so the remaining tasks can be completed.
     */
    override fun shutdown() {
        resume()
        super.shutdown()
    }

    protected fun setExecutionListener(listener: ExecutionListener?) {
        mExecutionListener = listener
    }

    interface ExecutionListener {
        fun beforeExecution(r: Runnable?)
        fun afterExecution(r: Runnable?)
    }
}
