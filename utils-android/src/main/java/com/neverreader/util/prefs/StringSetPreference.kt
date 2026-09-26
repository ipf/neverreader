package com.neverreader.util.prefs

interface StringSetPreference : Preference<MutableSet<String?>?> {
    /**
     * The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.
     * @return An immutable value, do not attempt to change this value directly. Make a copy if you need to modify it and use [.set] to update the stored value.
     */
    fun get(): MutableSet<String?>?
    fun set(value: MutableSet<String?>?)
}
