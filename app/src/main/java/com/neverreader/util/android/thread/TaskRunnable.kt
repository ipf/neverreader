package com.neverreader.util.android.thread

import android.os.Handler
import android.os.Looper

import com.neverreader.util.java.Cancelable
import com.neverreader.util.java.Logs
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * A single background task to run in a [TaskPool].
 *
 *
 * Subclass and implement the task's work in [.backgroundOperation].
 * Submit the task to a pool with [TaskPool.submit].
 *
 *
 * Provides some additional hooks that can be overridden:
 *
 *  * [.backgroundOnSkipped] Runs if the task's work skipped because it was cancelled
 *  * [.backgroundOnComplete] Runs after the work, while still on a background thread
 *  * [.requiresUIResponse] Allows you to enable a response to be invoked on the ui thread
 *  * [.uiOnComplete] If requiresUIResponse is true, this will be invoked on the ui thread after your work is complete
 *
 * Outside processes can also observe/listen to the result of this operation with [.setOperationListener]
 */
abstract class TaskRunnable constructor(var priority: Int = PRIORITY_NORMAL) :
    Runnable, Cancelable {
    protected val mStatus: AtomicInteger = AtomicInteger(STATUS_UNEXECUTED)
    protected val mCanceled: AtomicBoolean = AtomicBoolean(false)
    protected var mFuture: TaskPoolFuture? = null
    protected val mUIResponse: Boolean
    protected var mPool: TaskPool? = null
    private var mOperationListener: OperationListener? = null
    private var mOperationListenerReturnOnUIThread = false

    init {
        mUIResponse = requiresUIResponse()
    }

    /**
     * Flag this task as cancelled. This does not interrupt threads.
     * If the task has not yet run, when it is run, it will see this flag,
     * and skip all of the work. See [.backgroundOnSkipped].
     *
     *
     * Very long-running tasks can also check [.isCancelled] periodically to see if it should stop its current work.
     */
    override fun cancel() {
        mCanceled.set(true)
    }

    override val isCancelled: Boolean
        get() = mCanceled.get()
                || mStatus.get() == STATUS_CANCELED || (mFuture != null && mFuture!!.isCancelled())

    /*
     * A TaskPool will call this right before it executes.
     *
     */
    fun onTaskPoolSubmit(pool: TaskPool?, futureTask: TaskPoolFuture?) {
        mStatus.compareAndSet(STATUS_UNEXECUTED, STATUS_PENDING)
        mPool = pool
        mFuture = futureTask
    }

    /**
     * Blocks on and awaits this task's future and then returns the status flag (One of the [.STATUS_COMPLETE] like constants.
     * Before invoking this, the task must have either been submitted to a task pool or [.runNow] or it will throw an exception.
     */
    fun get(): Int {
        if (mFuture == null) throw NullPointerException("task not yet submitted or run")

        try {
            mFuture!!.get()
        } catch (t: Throwable) {
            return STATUS_CRASHED
        }

        return mStatus.get()
    }

    /*
     * Run now on the current Thread, blocking until complete, instead of submitting to a pool.
     */
    fun runNow() {
        onTaskPoolSubmit(null, null)
        run()
    }

    protected fun setAsFailed() {
        mStatus.set(STATUS_CRASHED)
    }

    fun hasFailed(): Boolean {
        return mStatus.get() == STATUS_CRASHED
    }

    /**
     * Should only be called from a TaskPool's processes', if you want to run it directly, use [.runNow]
     */
    override fun run() {
        var success = false
        var operationCrash: Throwable? = null
        if (!isCancelled && mStatus.compareAndSet(STATUS_PENDING, STATUS_EXECUTING)) {
            // Run the operation

            try {
                backgroundOperation()
                success = true


                // If it crashes, call the on error method
            } catch (operationThrowable: Throwable) {
                mStatus.set(STATUS_CRASHED)
                operationCrash = operationThrowable
                Logs.printStackTrace(operationThrowable)
            }
        } else {
            // OPT should check if it is cancelled versus something else?
            mStatus.set(STATUS_CANCELED)
            backgroundOnSkipped()
            return
        }


        // Whether it crashes or completes, always guarantee one of these callbacks
        // Even if cancelled
        val fSuccess = success && mStatus.get() == STATUS_EXECUTING
        val fOperationCrash = operationCrash
        try {
            if (mUIResponse) {
                // Callback on ui thread
                // QUESTION should the operation wait for this callback to finish?
                Handler(android.os.Looper.getMainLooper()).post(object : Runnable {
                    override fun run() {
                        uiOnComplete(fSuccess, fOperationCrash)
                        if (mOperationListener != null) {
                            mOperationListener!!.onComplete(this@TaskRunnable, fSuccess)
                        }
                    }
                })
            } else {
                // Callback on background thread
                backgroundOnComplete(fSuccess, fOperationCrash)
                if (mOperationListener != null) {
                    if (mOperationListenerReturnOnUIThread) {
                        Handler(android.os.Looper.getMainLooper()).post(Runnable {
                            mOperationListener!!.onComplete(
                                this@TaskRunnable,
                                fSuccess
                            )
                        })
                    } else {
                        mOperationListener!!.onComplete(this@TaskRunnable, fSuccess)
                    }
                }
            }
        } catch (unexpected: Throwable) {
            mStatus.set(STATUS_CRASHED)
            Logs.i("TaskRunnable", "task crashed")
        }

        if (mOperationListener != null) {
            mOperationListener!!.onFinal()
        }

        mStatus.compareAndSet(STATUS_EXECUTING, STATUS_COMPLETE)
    }

    /**
     * Called on the operation thread. Do your background work here.
     * @throws Exception
     */
    protected abstract fun backgroundOperation()

    /**
     * If the operation should call onComplete on the ui thread instead of the background thread, override this and return true.
     *
     * @return
     */
    protected open fun requiresUIResponse(): Boolean {
        return false
    } // OPT combine this with the uiOnComplete method somehow

    /**
     * If the operation is canceled, this will always be called to allow subclasses to clean up.
     * Runs on the operation thread.
     */
    protected fun backgroundOnSkipped() {}

    /**
     * Even if the backgroundOperation crashes, this will always be called. Use this as a chance to perform clean up.
     * Runs on the operation thread.
     *
     * @param success if backgroundOperation finished successfully.
     * @param operationCrash if success is false, this will contain the Throwable that crashed it. It may be null if success is false because of the operation being canceled.
     */
    protected fun backgroundOnComplete(success: Boolean, operationCrash: Throwable?) {
    }


    /**
     * Even if the backgroundOperation crashes, this will always be called (if requiresUIResponse() returns true).
     * Runs on the UI thread.
     *
     * NOTE: By the time this runs, the operation may be complete and nothing in this should access the operation's variables.
     * Also, at this time, the operation does not wait until this has run. This should be considered in the future.
     *
     * @param success if backgroundOperation finished successfully.
     * @param operationCrash if success is false, this will contain the Throwable that crashed it. It may be null if success is false because of the operation being canceled.
     */
    protected open fun uiOnComplete(success: Boolean, operationCrash: Throwable?) {}


    fun setOperationListener(listener: OperationListener?, returnOnUIThread: Boolean) {
        mOperationListener = listener
        mOperationListenerReturnOnUIThread = returnOnUIThread
    }

    interface OperationListener {
        /**
         * Called after backgroundOperation() or backgroundOnSkipped(). If you want this to be called on the UI Thread, set returnOnUIThread of setOperationLister() to true.
         *
         * @param success whether the backgroundOperation() completed
         */
        fun onComplete(operation: TaskRunnable?, success: Boolean)

        /**
         * Called on background thread as the last chance to perform tasks on the background thread, the last thing that occurs in the operation.
         */
        fun onFinal()
    }

    fun interface ThrowingRunnable {
        fun run()
    }


    companion object {
        const val STATUS_UNEXECUTED: Int = 1
        const val STATUS_PENDING: Int = 2
        const val STATUS_EXECUTING: Int = 3
        const val STATUS_COMPLETE: Int = 4
        const val STATUS_CANCELED: Int = -1
        const val STATUS_CRASHED: Int = -2


        const val PRIORITY_LOW: Int = 1
        const val PRIORITY_NORMAL: Int = 2
        const val PRIORITY_HIGH: Int = 3
        const val PRIORITY_VERY_HIGH: Int = 4

        fun simple(work: ThrowingRunnable, priority: Int = PRIORITY_NORMAL): TaskRunnable {
            return object : TaskRunnable(priority) {
                override fun backgroundOperation() {
                    work.run()
                }
            }
        }
    }
}
