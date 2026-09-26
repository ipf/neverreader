package com.neverreader.util.android.thread

import java.lang.Long
import kotlin.Comparator
import kotlin.Int

class PriorityFutureTaskComparator : Comparator<Runnable?> {
    override fun compare(a: Runnable?, b: Runnable?): Int {
        val task1 = (a as PriorityFutureTask)
        val task2 = (b as PriorityFutureTask)

        val priorityComparison = (task2.priority - task1.priority).toLong()

        if (priorityComparison == 0L) { // Same priority
            // Order it based on FIFO
            return Long.signum(task1.addedOrder - task2.addedOrder)
        } else {
            // Order it based on Priority
            return Long.signum(priorityComparison)
        }
    }
}

