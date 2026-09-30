package com.neverreader.util.prefs

/**
 * The app's persisted preferences.
 *
 * This is a factory: [forUser] hands back a typed handle for one key, and the
 * handle does the reading and writing. What is left here is the key list, which
 * is three entries — `appTheme`, `lastClipUrlHash` and `loadThirdPartyImages`.
 *
 * There is no app-scoped half. There was one, for settings that outlive an
 * account, but nothing in the app is stored that way, so it was a second
 * SharedPreferences file and a second set of methods carrying no values.
 *
 * [remove] is the one raw key operation exposed. The rotation lock that needed
 * it is gone, but the cleanup has to run once or a stored orientation lock
 * outlives the feature.
 *
 * The [Store] underneath is what makes this testable without Android.
 */
class Prefs(private val user: Store) {
    /** Reset the preference with this key, returning it back to unset. */
    fun remove(key: String?) {
        user.remove(key)
    }

    fun forUser(key: String?, defaultValue: Boolean): BooleanPreference {
        return BooleanPref(key, defaultValue, user)
    }

    fun forUser(key: String?, defaultValue: Int): IntPreference {
        return IntPref(key, defaultValue, user)
    }

    fun forUser(key: String?, defaultValue: String?): StringPreference {
        return StringPref(key, defaultValue, user)
    }
}
