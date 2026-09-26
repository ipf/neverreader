package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/** Implementation of [IntPreference]  */
class IntPref(private val key: String?, private val defaultValue: Int, private val store: Store) :
    IntPreference {
    override fun get(): Int {
        return if (this.isSet) store.getInt(key) else defaultValue
    }

    override fun set(value: Int) {
        store.set(key, value)
    }

    override val isSet: Boolean
        get() = store.contains(key)

    override fun changes(): Flow<Int?> {
        return store.intChanges(key)
    }

    override val withChanges: Flow<Int?>
        get() = changes().onStart { emit(get()) }
}
