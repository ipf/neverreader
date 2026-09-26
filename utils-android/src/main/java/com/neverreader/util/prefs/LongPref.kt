package com.neverreader.util.prefs

import io.reactivex.Observable

/** Implementation of [LongPreference]  */
class LongPref(private val key: String?, private val defaultValue: Long, private val store: Store) :
    LongPreference {
    override fun get(): Long {
        return if (this.isSet) store.getLong(key) else defaultValue
    }

    override fun set(value: Long) {
        store.set(key, value)
    }

    override val isSet: Boolean
        get() = store.contains(key)

    override fun changes(): Observable<Long?>? {
        return store.longChanges(key)
    }

    override val withChanges: Observable<Long?>?
        get() = changes()!!.startWith(get())
}
