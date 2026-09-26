package com.neverreader.util.prefs

import io.reactivex.Observable

/** Implementation of [EnumPreference] */
class EnumPref<E>(
    private val clazz: Class<E>,
    private val key: String?,
    private val defaultValue: E?,
    private val store: Store
) : EnumPreference<E> {

    override val isSet: Boolean
        get() = store.contains(key)

    override fun get(): E? {
        return if (isSet) from(store.getString(key)) else defaultValue
    }

    @Suppress("UNCHECKED_CAST")
    private fun from(value: String?): E? {
        return value?.let { java.lang.Enum.valueOf(clazz as Class<out Enum<*>>, it) as E }
    }

    override fun set(value: E?) {
        store.set(key, value?.toString())
    }

    override fun changes(): Observable<E?>? {
        return store.stringChanges(key)?.map { s -> from(s) }
    }

    override val withChanges: Observable<E?>?
        get() = changes()?.startWith(get())
}
