package com.neverreader.util.android.thread

import kotlin.Comparator

class PriorityFutureTaskComparator : Comparator<Runnable?> {
    override fun compare(a: Runnable?, b: Runnable?): Int {
        val task1 = (a as PriorityFutureTask)
        val task2 = (b as PriorityFutureTask)

        val priorityComparison = (task2.priority - task1.priority).toLong()

        if (priorityComparison == 0L) { // Same priority
            // Order it based on FIFO
            return (task1.addedOrder - task2.addedOrder).compareTo(0L)
        } else {
            // Order it based on Priority
            return priorityComparison.compareTo(0L)
        }
    }
}

