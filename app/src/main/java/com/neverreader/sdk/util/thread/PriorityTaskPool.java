package com.neverreader.sdk.util.thread;

import com.neverreader.sdk.util.wakelock.WakeLockManager;
import com.neverreader.util.android.thread.PriorityFutureTask;
import com.neverreader.util.android.thread.PriorityFutureTaskComparator;
import com.neverreader.util.android.thread.TaskPoolFuture;
import com.neverreader.util.android.thread.TaskRunnable;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A task pool that supports {@link com.neverreader.util.android.thread.PriorityFutureTask}'s priority as a basis for which task to
 * run next out of the queue.
 */
public class PriorityTaskPool extends WakefulTaskPool {
	
	private int mCreatedCount;
	
	public PriorityTaskPool(WakeLockManager wakelocks, int fixedPoolSize, String poolName) {
		super(wakelocks, fixedPoolSize, fixedPoolSize, new PriorityBlockingQueue<>(11, new PriorityFutureTaskComparator()), poolName);
	}
	
	public PriorityTaskPool(WakeLockManager wakelocks, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, String poolName) {
		super(wakelocks, corePoolSize, maximumPoolSize, keepAliveTime, unit, new PriorityBlockingQueue<>(11, new PriorityFutureTaskComparator()), poolName);
	}
	
	@Override
	protected TaskPoolFuture newFutureTask(TaskRunnable runnable) {
		return new PriorityFutureTask(runnable, ++mCreatedCount);
	}
	
}
