package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

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

    fun <E : Enum<E>> forUser(
        key: String?,
        clazz: Class<E>?,
        defaultValue: E?
    ): EnumPreference<E>

    fun <E : Enum<E>> forApp(
        key: String?,
        clazz: Class<E>?,
        defaultValue: E?
    ): EnumPreference<E>

    /**
     * @return Emits the key of each preference that changes.
     */
    fun changes(): Flow<String?>
}
