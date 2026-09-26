package com.neverreader.util.java

/**
 * Utilities for accessing fields that might be null or whose parent fields might not exist.
 *
 *
 * For example, when accessing a value like feed[0].item.posts[0].profile.avatar_url
 * any of those could be null, or out of bounds in the array. If you just want null
 * or a default value in that case, these utilities can save a lot of verbosity by avoiding
 * handling all of the null checks along the way and just returning a default value if
 * any parent or target value is null or inaccessible.
 *
 *
 * Useful for Thing generated classes that have immutable, nullable, fields.
 */
object Safe {
    /** Returns the value or 0 if the value is null.  */
    fun value(value: Int?): Int {
        return if (value != null) value else 0
    }

    /** Returns the value or 0 if the value is null.  */
    fun value(value: Long?): Long {
        return if (value != null) value else 0
    }

    /** Returns the value or 0 if the value is null.  */
    fun value(value: Float?): Float {
        return if (value != null) value else 0f
    }

    /** Returns the value or 0 if the value is null.  */
    fun value(value: Double?): Double {
        return if (value != null) value else 0.0
    }

    /** Returns the value or false if the value is null.  */
    fun value(value: Boolean?): Boolean {
        return if (value != null) value else false
    }

    /** Obtains this value or null if any exceptions were throw such as null pointers or index out of bounds or others.  */
    fun <V> get(getter: Get<V?>): V? {
        try {
            return getter.get()
        } catch (npe: Throwable) {
            return null
        }
    }

    /** A variant of [.get] that will return false if the value could not be obtained.  */
    fun getBoolean(getter: Get<Boolean?>): Boolean {
        return value(get<Boolean?>(getter))
    }

    /** A variant of [.get] that will return 0 if the value could not be obtained.  */
    fun getInt(getter: Get<Int?>): Int {
        return value(get<Int?>(getter))
    }

    /** A variant of [.get] that will return 0 if the value could not be obtained.  */
    fun getLong(getter: Get<Long?>): Long {
        return value(get<Long?>(getter))
    }

    fun <V> nonNullCopy(list: MutableList<V?>?): MutableList<V?> {
        if (list != null) return ArrayList<V?>(list)
        return ArrayList<V?>()
    }

    fun <K, V> nonNullCopy(map: MutableMap<K?, V?>?): MutableMap<K?, V?> {
        if (map != null) return HashMap<K?, V?>(map)
        return HashMap<K?, V?>()
    }

    fun interface Get<V> {
        /** Do whatever is needed to obtain the value or throw an exception if it cannot be obtained.  */
        @Throws(Exception::class)
        fun get(): V?
    }
}
