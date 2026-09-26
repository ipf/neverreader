package com.neverreader.util.prefs

import io.reactivex.Observable

/** Implementation of [FloatPreference]  */
class FloatPref(
    private val key: String?,
    private val defaultValue: Float,
    private val store: Store
) : FloatPreference {
    override fun get(): Float {
        return if (this.isSet) store.getFloat(key) else defaultValue
    }

    override fun set(value: Float) {
        store.set(key, value)
    }

    override val isSet: Boolean
        get() = store.contains(key)

    override fun changes(): Observable<Float?>? {
        return store.floatChanges(key)
    }

    override val withChanges: Observable<Float?>?
        get() = changes()!!.startWith(get())
}
