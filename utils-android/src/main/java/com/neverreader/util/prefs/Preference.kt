package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart


/**
 * Represents a value, of some type, typically persisted across app lifecycles / processes.
 * Mostly meant as an abstraction on Android's shared preferences that can be used in different
 * non-android use cases such as unit tests.
 */
interface Preference<T> {
    /** @return true if this preference has been explicitly set in the past, false if it has not and is returning its defaultValue.
     */
    val isSet: Boolean

    /** Emits each time this preference's value changes, from the moment of
     *  collection onwards. Nothing is emitted for changes made before then, so
     *  use [withChanges] to also get the current value. */
    fun changes(): Flow<T?>

    /** Emits the current value on collection, then on every change. */
    val withChanges: Flow<T?>
}
