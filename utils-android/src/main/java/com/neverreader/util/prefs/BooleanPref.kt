package com.neverreader.util.prefs

import io.reactivex.Observable


/** Implementation of [BooleanPreference]  */
class BooleanPref(
    private val key: String?,
    private val defaultValue: Boolean,
    private val store: Store
) : BooleanPreference {
    override fun get(): Boolean {
        return if (this.isSet) store.getBoolean(key) else defaultValue
    }

    override fun set(value: Boolean) {
        store.set(key, value)
    }

    override val isSet: Boolean
        get() = store.contains(key)

    override fun changes(): Observable<Boolean?>? {
        return store.booleanChanges(key)
    }

    override val withChanges: Observable<Boolean?>?
        get() = changes()!!.startWith(get())
}
