package com.neverreader.util.prefs

interface FloatPreference : Preference<Float?> {
    /** The current value. If [.set] has never been called, it returns the default value. Use [.isSet] if you need to known.  */
    fun get(): Float
    fun set(value: Float)
}
