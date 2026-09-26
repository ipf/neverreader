package com.neverreader.util.prefs

@Suppress("UNCHECKED_CAST")
interface EnumPreference<E> : Preference<E?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): E?
    fun set(value: E?)
}
