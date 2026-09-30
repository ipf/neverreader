package com.neverreader.util.prefs

import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/** Implementation of [StringSetPreference]  */
class StringSetPref(
    private val key: String?,
    defaultValue: MutableSet<String?>?,
    private val store: Store
) : StringSetPreference {
    private val defaultValue: MutableSet<String?>? = if (defaultValue != null) Collections.unmodifiableSet<String?>(defaultValue) else null

    override val isSet: Boolean
        get() = store.contains(key)

    override fun get(): MutableSet<String?>? {
        return if (this.isSet) store.getStringSet(key) else defaultValue
    }

    override fun set(value: MutableSet<String?>?) {
        store.set(key, value)
    }

    override fun changes(): Flow<MutableSet<String?>?> {
        return store.stringSetChanges(key)
    }

    override val withChanges: Flow<MutableSet<String?>?>
        get() = changes().onStart { emit(get()) }
}
