package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart


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

    override fun changes(): Flow<Boolean?> {
        return store.booleanChanges(key)
    }

    override val withChanges: Flow<Boolean?>
        get() = changes().onStart { emit(get()) }
}
