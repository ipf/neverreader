package com.neverreader.util.java

/**
 * A singleton for recycling [StringBuilder] instances. To obtain one call [.get]
 * and return it to the recycler when you are done with [.recycle].
 *
 *
 * **Be sure to not use it after calling recycle!**
 */
object StringBuilders {
    private val mBuilders = ArrayList<StringBuilder?>()

    private val LOCK = Any()

    fun get(): StringBuilder {
        val builder: StringBuilder?
        synchronized(LOCK) {
            builder = if (mBuilders.isEmpty()) null else mBuilders.removeAt(0)
        }

        if (builder != null) {
            return builder
        }

        return StringBuilder()
    }

    fun recycle(builder: StringBuilder) {
        reset(builder)
        synchronized(LOCK) {
            mBuilders.add(builder)
        }
    }

    fun reset(builder: StringBuilder) {
        builder.setLength(0)
    }
}
