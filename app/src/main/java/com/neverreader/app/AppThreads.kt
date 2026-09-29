package com.neverreader.app

import android.os.Handler
import android.os.Looper
import com.neverreader.app.AppLifecycle.LogoutPolicy
import com.neverreader.sdk.util.thread.PriorityTaskPool
import com.neverreader.sdk.util.thread.WakefulTaskPool
import com.neverreader.sdk.util.wakelock.WakeLockManager
import com.neverreader.util.android.thread.TaskPool
import com.neverreader.util.android.thread.TaskRunnable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helpers and utilities for working with Threads and Handlers in the app
 *
 *
 * Run async tasks using one of the async() like methods.
 *
 *
 * These tasks are run in a general purpose task pool which will automatically handle stopping on during the [LogoutPolicy.stopModifyingUserData].
 *
 *
 * If you want to manage your own task pool, see some of the helper methods like [.newWakefulPool] and [.newPriorityPool] variants.
 *
 *
 * This also has [Handler] and Ui Thread related helper methods.
 */
@Singleton
class AppThreads @Inject constructor(private val wakelocks: WakeLockManager) {
    val handler: Handler = Handler(Looper.getMainLooper())
    private val main: Thread = Looper.getMainLooper().thread

    private val lock = Any()
    private var pool: TaskPool? = null

    /**
     * Convenience for [Handler.post] with the built in [.getHandler].
     * This always posts. Also see [.runOrPostOnUiThread] which will run it immediately if already on the ui thread.
     */
    fun postOnUiThread(r: Runnable) {
        handler.post(r)
    }

    /** If already on the UI thread, run the runnable immediately. Otherwise post it to the ui thread.  */
    fun runOrPostOnUiThread(r: Runnable) {
        if (this.isOnUIThread) {
            r.run()
        } else {
            handler.post(r)
        }
    }

    /** If already NOT on the UI thread, run the runnable immediately. Otherwise submit it to a background pool to run.  */
    fun runOffUiThread(r: Runnable) {
        if (!this.isOnUIThread) {
            r.run()
        } else {
            async(r)
        }
    }

    val isOnUIThread: Boolean
        get() = Thread.currentThread() === main

    /**
     * @return Creates if needed and returns a single task pool for general usage.
     */
    private fun pool(): TaskPool {
        synchronized(lock) {
            if (pool == null) {
                pool = WakefulTaskPool(wakelocks, 5, 128, "task")
            }
            return pool!!
        }
    }

    /**
     * Queue up a task in the general purpose task pool, see main doc for details.
     */
    fun submit(task: TaskRunnable?) {
        pool().submit(task)
    }

    /**
     * Queues up a task off the ui thread in the general purpose task pool.
     * **Note** It is up to you to handle exceptions this might throw.
     * If any uncaught runtime exceptions are thrown, they will be logged via [Logs.printStackTrace] but otherwise ignored.
     * @param task The background work to complete
     * @return The already submitted task for listening, referencing, controlling, etc.
     */
    fun async(task: Runnable): TaskRunnable {
        val run = TaskRunnable.simple(TaskRunnable.ThrowingRunnable { task.run() })
        pool().submit(run)
        return run
    }

    /**
     * Same as [.async] but catches uncaught exceptions during the task
     * and invokes onError with the the error. This error callback will be invoked from the task thread as well.
     * The error will automatically be logged via [Logs.printStackTrace].
     * Passing a null onError will essentially ignore the error.
     *
     * This can be useful if your task must callback to something regardless of result and you want to make sure runtime exceptions
     * and other errors still end up with a callback.
     */
    fun async(task: SimpleTask, onError: OnError?): TaskRunnable {
        return async(Runnable {
            try {
                task.backgroundOperation()
            } catch (t: Throwable) {
                onError?.onError(t)
            }
        })
    }

    /**
     * A way to run a simple, quick task with an interface that allows for lambda use.
     * Submitted to the general purpose task pool, see main doc for details.
     * @param task The background work to complete. Note, all exceptions here are caught. See the callback's crash parameter for the exception.
     * @param uiOnComplete A callback on the ui thread.
     * @return The already submitted task for listening, referencing, controlling, etc.
     */
    fun asyncThen(task: SimpleTask, uiOnComplete: UiThreadResponse?): TaskRunnable {
        val run: TaskRunnable = object : TaskRunnable() {
            override fun backgroundOperation() {
                task.backgroundOperation()
            }

            override fun requiresUIResponse(): Boolean {
                return true
            }

            override fun uiOnComplete(success: Boolean, operationCrash: Throwable?) {
                uiOnComplete?.uiOnComplete(success, operationCrash)
            }
        }
        pool().submit(run)
        return run
    }

    interface SimpleTask {
        fun backgroundOperation()
    }

    interface OnError {
        fun onError(error: Throwable?)
    }

    interface ResultTask<T> {
        fun backgroundOperation(): T?
    }

    fun interface UiThreadResponse {
        fun uiOnComplete(success: Boolean, operationCrash: Throwable?)
    }

    interface UiThreadResultResponse<T> {
        fun uiOnComplete(success: Boolean, operationCrash: Throwable?, result: T?)
    }


    val logoutPolicy: LogoutPolicy
        /**
         * This does not use [AppLifecycle.onLogoutStarted] because [UserManager]
         * wants to run this policy at the very end, because logout processes might actually need
         * to use this to run async tasks.
         */
        get() = object : LogoutPolicy {
            override fun stopModifyingUserData() {
                synchronized(lock) {
                    if (pool != null) {
                        pool!!.terminate(20, TimeUnit.SECONDS)
                    }
                }
            }

            override fun deleteUserData() {}

            override fun restart() {
                pool = null
            }

            override fun onLoggedOut() {}
        }

    /**
     * Creates and returns a new task pool that can process tasks by priority.
     * It also has the same rules as [.newWakefulPool].
     */
    fun newPriorityPool(name: String?, maxThreads: Int): PriorityTaskPool {
        val pool = PriorityTaskPool(wakelocks, maxThreads, name)
        setup(pool)
        return pool
    }

    /**
     * Creates and returns a new task pool that has some standard settings:
     *
     *  * Holds a wake lock as long as it has pending or active tasks.
     *  * Can spin up to maxThreads when busy.
     *  * Will spin down and release down threads when idle.
     *
     * Note: These pools are **not** automatically managed during logout,
     * please consider whether or not you need a [LogoutPolicy].
     */
    fun newWakefulPool(name: String?, maxThreads: Int): WakefulTaskPool {
        val pool = WakefulTaskPool(wakelocks, maxThreads, name)
        setup(pool)
        return pool
    }

    /** A new pool with custom settings. These pools are not automatically managed during logout, it is up to you to handle that as needed.  */
    fun newWakefulPool(
        name: String?,
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?
    ): WakefulTaskPool {
        return WakefulTaskPool(
            wakelocks,
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            unit,
            LinkedBlockingQueue<Runnable>(),
            name
        )
    }

    /** A new pool with custom settings. These pools are not automatically managed during logout, it is up to you to handle that as needed.  */
    fun newPriorityPool(
        name: String?,
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long,
        unit: TimeUnit?
    ): PriorityTaskPool {
        return PriorityTaskPool(wakelocks, corePoolSize, maximumPoolSize, keepAliveTime, unit, name)
    }

    private fun setup(pool: WakefulTaskPool) {
        pool.setKeepAliveTime(10, TimeUnit.SECONDS)
        pool.allowCoreThreadTimeOut(true)
    }
}
