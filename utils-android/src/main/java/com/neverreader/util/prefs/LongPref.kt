package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

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

    override fun changes(): Flow<Long?> {
        return store.longChanges(key)
    }

    override val withChanges: Flow<Long?>
        get() = changes().onStart { emit(get()) }
}
