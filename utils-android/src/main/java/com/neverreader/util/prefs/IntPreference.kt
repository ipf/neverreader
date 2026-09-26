package com.neverreader.util.prefs

interface IntPreference : Preference<Int?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): Int
    fun set(value: Int)
}
