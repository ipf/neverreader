package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

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

    override fun changes(): Flow<Float?> {
        return store.floatChanges(key)
    }

    override val withChanges: Flow<Float?>
        get() = changes().onStart { emit(get()) }
}
