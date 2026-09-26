package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/** Implementation of [StringPreference]  */
class StringPref(
    private val key: String?,
    private val defaultValue: String?,
    private val store: Store,
    override val isSet: Boolean
) : StringPreference {
    override fun get(): String? {
        return if (this.isSet) store.getString(key) else defaultValue
    }

    override fun set(value: String?) {
        store.set(key, value)
    }

    override fun changes(): Flow<String?> {
        return store.stringChanges(key)
    }

    override val withChanges: Flow<String?>
        get() = changes().onStart { emit(get()) }
}
