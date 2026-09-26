package com.neverreader.util.java

import java.io.File
import java.util.concurrent.CountDownLatch

/**
 * On Android, [java.nio.channels.FileLock], is only advisory and doesn't actually
 * do anything to prevent thread safety issues. This class serves as a replacement for that
 * functionality. To use, there should be a shared single instance of this class and any
 * reads or writes should invoke one of the [.lock] methods prior to doing
 * anything with the file. When complete, invoke [Lock.release]. Be sure to release
 * in a finally block to ensure it releases even with exceptions.
 *
 * TODO make read and write separate lock types so multiple reads can happen at the same time.
 */
class FileLocks {
    private val mLocks: MutableMap<String?, ArrayList<Lock?>?> =
        HashMap<String?, ArrayList<Lock?>?>()
    private val mRecycled = ArrayList<ArrayList<Lock?>?>()

    /**
     * Obtains a lock on a file. If there is already a lock on this file, it blocks until
     * that lock is released and it is granted a lock. Be sure to release the lock
     * when complete with [Lock.release].
     *
     * @param path
     * @return The lock
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    fun lock(path: String?): Lock {
        val lock: Lock = Lock(path)
        val held: Lock?

        synchronized(mLocks) {
            // Find or create a queue for this file.
            var queue = mLocks.get(path)
            if (queue == null) {
                // Create or reuse a queue
                if (mRecycled.isEmpty()) {
                    queue = ArrayList<Lock?>()
                } else {
                    queue = mRecycled.removeAt(0)
                    queue!!.clear()
                }
                mLocks.put(path, queue)
            }

            // If there is anyone ahead of us in the queue, we will wait until they release.
            held = if (!queue.isEmpty()) queue.get(queue.size - 1) else null

            // Add this request to the end of the queue.
            queue.add(lock)
        }

        // Wait for anyone ahead of us.
        if (held != null) {
            held.await()
        }

        // At this point, we should be the currently held lock for the file.
        return lock
    }

    /**
     * @see .lock
     */
    @Throws(InterruptedException::class)
    fun lock(file: File): Lock {
        return lock(file.getAbsolutePath())
    }

    inner class Lock(val file: String?) {
        private val mLatch = CountDownLatch(1)

        @Throws(InterruptedException::class)
        fun await() {
            mLatch.await()
        }

        /**
         * Release control of the file to let others access it.
         */
        fun release() {
            synchronized(mLocks) {
                if (mLatch.getCount() <= 0) {
                    return  // Already released
                }
                // Remove the lock from the queue, recycle the queue if now empty.
                val queue = mLocks.get(file)
                if (queue != null) {
                    queue.remove(this)
                    if (queue.isEmpty()) {
                        mRecycled.add(mLocks.remove(file))
                    }
                }
                mLatch.countDown()
            }
        }
    }

    companion object {
        /**
         * Invokes release or does nothing if the lock is null.
         */
        fun releaseQuietly(lock: Lock?) {
            if (lock != null) {
                lock.release()
            }
        }
    }
}
