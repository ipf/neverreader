package com.neverreader.util.prefs

/** A [Preference] with a boolean value.  */
interface BooleanPreference : Preference<Boolean?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): Boolean
    fun set(value: Boolean)
}
