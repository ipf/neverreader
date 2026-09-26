package com.neverreader.util.prefs

import io.reactivex.Observable

interface LongPreference : Preference<Long?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): Long
    fun set(value: Long)

    companion object {
        val NO_OP: LongPreference = object : LongPreference {
            override val isSet: Boolean
                get() = false

            override fun changes(): Observable<Long?>? {
                return Observable.never<Long?>()
            }

            override val withChanges: Observable<Long?>?
                get() = changes()!!.startWith(get())

            override fun get(): Long {
                return 0
            }

            override fun set(value: Long) {}
        }
    }
}
