package com.neverreader.util.java

object ListUtils {
    /**
     * Safe checking of empty when list might be null.
     *
     * @param list
     * @return
     */
    fun <E> isEmpty(list: MutableList<E?>?): Boolean {
        return list == null || list.isEmpty()
    }

    /**
     * Safe checking of size when list might be null.
     *
     * @param list
     * @return 0 if null or empty, otherwise the size.
     */
    fun size(list: MutableList<*>?): Int {
        return if (list == null) 0 else list.size
    }

    /**
     * Null safe version of contains
     * @param collection if null, always returns false
     * @param obj if null, always returns false (so not helpful if you are searching for a null entry)
     * @return
     */
    fun contains(collection: MutableCollection<*>?, obj: Any?): Boolean {
        if (collection == null || obj == null) {
            return false
        } else {
            return collection.contains(obj)
        }
    }
}
