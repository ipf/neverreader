package com.neverreader.util.android.thread

import java.util.concurrent.FutureTask

/**
 * A [FutureTask] wrapper on a [TaskRunnable] to provide some status info for [TaskPool]
 * and to invoke the cancel method if needed.
 */
open class TaskPoolFuture internal constructor(var mRunnable: TaskRunnable?) : FutureTask<Any?>(
    mRunnable, null
) {
    private val mLock = Any()
    private var mWasBulkCancelled = false

    override fun done() {
        synchronized(mLock) {
            mRunnable = null
        }
        super.done()
    }

    /**
     * Same as [.cancel] but also flags that this was part of a bulk cancel
     * so that [.wasBulkCanceled] will return true.
     */
    fun bulkCancel() {
        synchronized(mLock) {
            if (mRunnable == null) {
                return
            }
            mWasBulkCancelled = true
        }

        mRunnable!!.cancel()
    }

    /**
     * Check if the TaskRunnable was cancelled because the pool did a bulk cancel.
     * @return true if it was part of a bulk pool cancel
     */
    fun wasBulkCanceled(): Boolean {
        synchronized(mLock) {
            return mWasBulkCancelled
        }
    }
}

