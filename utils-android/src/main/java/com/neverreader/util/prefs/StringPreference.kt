package com.neverreader.util.prefs

interface StringPreference : Preference<String?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): String?
    fun set(value: String?)
}
