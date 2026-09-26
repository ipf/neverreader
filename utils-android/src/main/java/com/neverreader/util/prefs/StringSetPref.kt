package com.neverreader.util.prefs

import java.util.Collections
import io.reactivex.Observable

/** Implementation of [StringSetPreference]  */
class StringSetPref(
    private val key: String?,
    defaultValue: MutableSet<String?>?,
    private val store: Store,
    override val isSet: Boolean,
    override val withChanges: Observable<MutableSet<String?>?>?
) : StringSetPreference {
    private val defaultValue: MutableSet<String?>? = if (defaultValue != null) Collections.unmodifiableSet<String?>(defaultValue) else null

    override fun get(): MutableSet<String?>? {
        return if (this.isSet) store.getStringSet(key) else defaultValue
    }

    override fun set(value: MutableSet<String?>?) {
        store.set(key, value)
    }

    override fun changes(): Observable<MutableSet<String?>?>? {
        return store.stringSetChanges(key)
    }
}
