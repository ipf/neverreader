package com.neverreader.util.prefs

import io.reactivex.Observable

/**
 * An app's persisted [Preference]s.
 *
 *
 * Divides preferences into two different [Store]s, one that represents preferences that are for the user
 * and should be cleared with [.clearUser] when the user logs out, and another set of preferences that
 * are for the app and likely are never cleared (but can be with [.clear].
 *
 *
 * To obtain an instance of a user preference, use one of the forUser methods.
 * To obtain an instance of a user preference, use one of the forApp methods.
 */
interface Preferences {
    /** Reset all user-based preferences, returning them back to unset.  */
    fun clearUser()

    /** Reset all preferences, returning them back to unset.  */
    fun clear()

    /** Reset the preference with this key, returning it back to unset.  */
    fun remove(key: String?)

    /** A set of all known keys for user preferences  */
    fun userKeys(): MutableSet<String?>?

    /** A set of all known keys for app preferences  */
    fun appKeys(): MutableSet<String?>?

    fun forUser(key: String?, defaultValue: Boolean): BooleanPreference
    fun forApp(key: String?, defaultValue: Boolean): BooleanPreference

    fun forUser(key: String?, defaultValue: Int): IntPreference
    fun forApp(key: String?, defaultValue: Int): IntPreference

    fun forUser(key: String?, defaultValue: Float): FloatPreference
    fun forApp(key: String?, defaultValue: Float): FloatPreference

    fun forUser(key: String?, defaultValue: Long): LongPreference
    fun forApp(key: String?, defaultValue: Long): LongPreference

    fun forUser(key: String?, defaultValue: String?): StringPreference
    fun forApp(key: String?, defaultValue: String?): StringPreference

    fun forUser(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference
    fun forApp(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference

    fun <E> forUser(
        key: String?,
        clazz: Class<E?>?,
        defaultValue: E?
    ): EnumPreference<E?>

    fun <E> forApp(
        key: String?,
        clazz: Class<E?>?,
        defaultValue: E?
    ): EnumPreference<E?>

    /**
     * Create a nested set of preferences.
     * TODO flush out docs and expectations a bit more.
     * @param name
     * @return
     */
    fun group(name: String?): Preferences?

    /**
     * @return An observable of when preferences change, the emitted value is the key of the preference.
     */
    fun changes(): Observable<String?>?
}
