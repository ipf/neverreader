package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.onStart

interface LongPreference : Preference<Long?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): Long
    fun set(value: Long)

    companion object {
        val NO_OP: LongPreference = object : LongPreference {
            override val isSet: Boolean
                get() = false

            override fun changes(): Flow<Long?> = emptyFlow()

            override val withChanges: Flow<Long?>
                get() = changes().onStart { emit(get()) }

            override fun get(): Long {
                return 0
            }

            override fun set(value: Long) {}
        }
    }
}
