package com.neverreader.util.android.thread


class PriorityFutureTask(
    runnable: TaskRunnable?,
    /**
     * Used to keep track of the order it was submitted to the queue,
     * so the Comparator can keep track of priority within this order.
     */
    val addedOrder: Long
) : TaskPoolFuture(runnable) {
    private val mInitialPriority: Int

    init {
        mInitialPriority = this.priority
    }

    val priority: Int
        get() {
            if (mRunnable == null) {
                // This task was cancelled or finished
                return mInitialPriority
            } else {
                val runnable = mRunnable
                return runnable?.priority ?: mInitialPriority
            }
        }
}

