package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow

/**
 * A store of preference values, backed by [AndroidPrefStore] in the app and by
 * whatever a test provides.
 *
 * This exists to make the preference layer testable without Android. It was
 * written for an app that stored hundreds of values; it now carries three, so
 * it holds the primitive types those need and nothing more.
 */
interface Store {
    /** Emits the key of each preference that changes. Cold: the listener is
     *  attached to a collection and detached when it stops. */
    fun changes(): Flow<String>
    fun contains(key: String?): Boolean
    fun remove(key: String?)
    fun clear()

    fun getString(key: String?): String?
    fun set(key: String?, value: String?)
    fun stringChanges(key: String?): Flow<String?>

    fun getInt(key: String?): Int
    fun set(key: String?, value: Int)
    fun intChanges(key: String?): Flow<Int?>

    fun getBoolean(key: String?): Boolean
    fun set(key: String?, value: Boolean)
    fun booleanChanges(key: String?): Flow<Boolean?>
}
