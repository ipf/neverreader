package com.neverreader.util.java

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * A synchronization aid that allows one or more threads to wait until a set of operations being performed in other threads completes.
 * Similar to [CountDownLatch].
 *
 *
 * Operations can create a hold with [.hold] and then when their work completes, release it with [.release].
 *
 *
 * A thread can wait until all holds are released by using one of the await methods such as [.await].
 *
 *
 * The latch will open/release the first time the holds reach zero after await has been called at least once.
 * This means if await is invoked before any holds are added, it will release immediately.
 *
 *
 * Once the latch is released, it will stay open. Make a new instance if you need a new latch.
 */
class KeyLatch
/**
 * @param strict if true, this will throw exceptions if you invoke [.hold] or [.release] after the latch has been opened or if release is invoked for a key that doesn't have a hold.
 * if false, those events will just be ignored.
 */ @JvmOverloads constructor(private val strict: Boolean = false) {
    private val holds: MutableSet<Any?> = HashSet<Any?>()
    private val latch = CountDownLatch(1)
    private var state = State.PENDING
    private var lastChange: Long = 0

    internal enum class State {
        /** No await method has been called yet.  */
        PENDING,

        /** Await has been called but there are holds it is waiting for.  */
        CLOSED,

        /** The latch has been released  */
        OPEN
    }

    /**
     * Add an additional hold to keep the latch waiting.
     * @param key A unique key for this hold, based on its hashcode/equals implementation.
     */
    @Synchronized
    fun hold(key: Any?) {
        if (strict && latch.getCount() == 0L) strictError("Latch has already been released. Attempted hold: " + key)
        val added = holds.add(key)
        lastChange = System.currentTimeMillis()
        if (strict && !added) strictError("Duplicate hold: " + key)
        invalidate()
    }

    /**
     * Release a hold. If there are no more holds, the latch will open.
     * @param key The key/object provided to [.hold]
     */
    @Synchronized
    fun release(key: Any?) {
        if (strict && latch.getCount() == 0L) strictError("Latch has already been released. Attempted release: " + key)
        val removed = holds.remove(key)
        lastChange = System.currentTimeMillis()
        if (strict && !removed) strictError("Hold was not active: " + key)
        invalidate()
    }

    private fun strictError(log: String?) {
        throw RuntimeException(log)
    }

    @Synchronized
    private fun activate() {
        if (state == State.PENDING) state = State.CLOSED
    }

    @Synchronized
    private fun invalidate() {
        if (state == State.CLOSED) {
            if (holds.isEmpty()) {
                state = State.OPEN
                latch.countDown()
            }
        } else {
            // If already open or pending, nothing to update yet
        }
    }

    /**
     * @return A copied set (modifications here have no effect) of the active holds.
     */
    @Synchronized
    fun holds(): MutableSet<Any?> {
        return HashSet<Any?>(holds)
    }

    @get:Synchronized
    val isOpen: Boolean
        /**
         * @return if the latch is open/released.  This has the same effect as calling an await in that it will switch the latch from pending to with closed or open. It won't block though, just return the current state.
         */
        get() {
            activate()
            invalidate()
            return state == State.OPEN
        }

    /**
     * Same as [CountDownLatch.await]
     * If no holds have been created, this will release the latch immediately.
     */
    @Throws(InterruptedException::class)
    fun await(time: Long, unit: TimeUnit): Boolean {
        activate()
        invalidate() // Count down the launch in the case that no holds were ever created.
        return latch.await(time, unit)
    }

    /**
     * Awaits until the latch is released, invoking the 'checkIn' at an interval of 'frequencyMillis' to see if it should continue to await.
     * If no holds have been created, this will release the latch immediately.
     * @return true if the latch was completed and released, false if this returned because [CheckIn.checkin] returned false.
     */
    @Throws(InterruptedException::class)
    fun await(frequencyMillis: Long, checkIn: CheckIn): Boolean {
        activate()
        invalidate() // Count down the launch in the case that no holds were ever created.
        val started = System.currentTimeMillis()
        do {
            val released = latch.await(frequencyMillis, TimeUnit.MILLISECONDS)
            if (released) return true
            val now = System.currentTimeMillis()
            if (!checkIn.checkin(now - started, holds(), now - lastChange)) return false
        } while (true)
    }

    interface CheckIn {
        /**
         * The requested frequency since start or the last check in has elapsed during [.await].
         * @param elapsedSinceStart The total time in millis since await began
         * @param held A copy of the current holds
         * @param elapseSinceChange The total time in millis since the last change to holds (add or release) has occurred
         * @return true to continue waiting, false to stop.
         */
        fun checkin(
            elapsedSinceStart: Long,
            held: MutableSet<Any?>?,
            elapseSinceChange: Long
        ): Boolean
    }
}
