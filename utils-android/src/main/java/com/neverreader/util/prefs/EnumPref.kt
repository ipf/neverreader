package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Implementation of [EnumPreference] */
class EnumPref<E : Enum<E>>(
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

    private fun from(value: String?): E? {
        return value?.let { java.lang.Enum.valueOf(clazz, it) }
    }

    override fun set(value: E?) {
        store.set(key, value?.toString())
    }

    override fun changes(): Flow<E?> {
        return store.stringChanges(key).map { s -> from(s) }
    }

    override val withChanges: Flow<E?>
        get() = changes().onStart { emit(get()) }
}
