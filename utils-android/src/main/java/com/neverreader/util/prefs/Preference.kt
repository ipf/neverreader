package com.neverreader.util.prefs

import io.reactivex.Observable


/**
 * Represents a value, of some type, typically persisted across app lifecycles / processes.
 * Mostly meant as an abstraction on Android's shared preferences that can be used in different
 * non-android use cases such as unit tests.
 */
interface Preference<T> {
    /** @return true if this preference as been explicitly changed/set in the past, false if it has not and is returning its defaultValue.
     */
    val isSet: Boolean

    /** An observable anytime this preference's value changes in the future  */
    fun changes(): Observable<T?>?

    /** An observable that emits the current value on subscribe plus anytime this preference's value changes in the future.  */
    val withChanges: Observable<T?>?
}
