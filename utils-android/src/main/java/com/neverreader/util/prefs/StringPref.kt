package com.neverreader.util.prefs

import io.reactivex.Observable

/** Implementation of [StringPreference]  */
class StringPref(
    private val key: String?,
    private val defaultValue: String?,
    private val store: Store,
    override val isSet: Boolean,
    override val withChanges: Observable<String?>?
) : StringPreference {
    override fun get(): String? {
        return if (this.isSet) store.getString(key) else defaultValue
    }

    override fun set(value: String?) {
        store.set(key, value)
    }

    override fun changes(): Observable<String?>? {
        return store.stringChanges(key)
    }

}
