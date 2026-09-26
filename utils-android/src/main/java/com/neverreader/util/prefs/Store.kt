package com.neverreader.util.prefs

import io.reactivex.Observable


/**
 * A store of preference values. Typically, persisted.
 * Mostly intended as an abstraction of Android's SharedPreferences class that can be
 * used in non-android contexts like unit tests.
 */
interface Store {
    fun changes(): Observable<String?>?

    fun contains(key: String?): Boolean
    fun remove(key: String?)
    fun clear()
    fun keys(): MutableSet<String?>?

    fun getString(key: String?): String?
    fun set(key: String?, value: String?)
    fun stringChanges(key: String?): Observable<String?>?

    /** @return null if not present or an immutable set.
     */
    fun getStringSet(key: String?): MutableSet<String?>?

    /** Update the value. Note: Implementations to take care to make sure that changes to the value passed here don't change the internal stored value. Make a copy of the set before holding a reference to it.  */
    fun set(key: String?, value: MutableSet<String?>?)

    /** Note that sets emitted are immutable.  */
    fun stringSetChanges(key: String?): Observable<MutableSet<String?>?>?

    fun getInt(key: String?): Int
    fun set(key: String?, value: Int)
    fun intChanges(key: String?): Observable<Int?>?

    fun getFloat(key: String?): Float
    fun set(key: String?, value: Float)
    fun floatChanges(key: String?): Observable<Float?>?

    fun getLong(key: String?): Long
    fun set(key: String?, value: Long)
    fun longChanges(key: String?): Observable<Long?>?

    fun getBoolean(key: String?): Boolean
    fun set(key: String?, value: Boolean)
    fun booleanChanges(key: String?): Observable<Boolean?>?
}
