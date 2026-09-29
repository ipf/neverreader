package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onStart

/**
 * A [Preferences] that wraps another and when getting preference instances, it adds a prefix to the key and invokes it on the parent.
 * All methods like [.userKeys], [.clear], etc. will only affect preferences with that prefix.
 * This is an implementation of a nested preference for use in [Preferences.group].
 */
class PrefixPreferences(private val wrapped: Preferences, private val prefix: String?) :
    Preferences {
    private fun prefix(key: String?): String {
        return prefix!! + key
    }

    override fun remove(key: String?) {
        wrapped.remove(prefix(key))
    }

    override fun userKeys(): MutableSet<String?> {
        val keys: MutableSet<String?> = HashSet<String?>()
        val prefixLen = prefix?.length
        for (key in wrapped.userKeys()!!) {
            prefix?.let { if (key?.startsWith(it) == true) keys.add(key.substring(prefixLen!!)) }
        }
        return keys
    }

    override fun appKeys(): MutableSet<String?> {
        val keys: MutableSet<String?> = HashSet<String?>()
        val prefixLen = prefix?.length
        for (key in wrapped.appKeys()!!) {
            if (key!!.startsWith(prefix!!)) keys.add(key.substring(prefixLen ?: 0))
        }
        return keys
    }

    override fun clearUser() {
        for (key in wrapped.userKeys()!!) {
            prefix?.let { if (key?.startsWith(it) == true) wrapped.remove(key) }
        }
    }

    override fun clear() {
        clearUser()
        for (key in wrapped.appKeys()!!) {
            prefix?.let { if (key?.startsWith(it) == true) wrapped.remove(key) }
        }
    }

    override fun forUser(key: String?, defaultValue: Boolean): BooleanPreference {
        return wrapped.forUser(prefix(key), defaultValue)
    }

    override fun forApp(key: String?, defaultValue: Boolean): BooleanPreference {
        return wrapped.forApp(prefix(key), defaultValue)
    }

    override fun forUser(key: String?, defaultValue: Float): FloatPreference {
        return wrapped.forUser(prefix(key), defaultValue)
    }

    override fun forApp(key: String?, defaultValue: Float): FloatPreference {
        return wrapped.forApp(prefix(key), defaultValue)
    }

    override fun forUser(key: String?, defaultValue: Int): IntPreference {
        return wrapped.forUser(prefix(key), defaultValue)
    }

    override fun forApp(key: String?, defaultValue: Int): IntPreference {
        return wrapped.forApp(prefix(key), defaultValue)
    }

    override fun forUser(key: String?, defaultValue: Long): LongPreference {
        return wrapped.forUser(prefix(key), defaultValue)
    }

    override fun forApp(key: String?, defaultValue: Long): LongPreference {
        return wrapped.forApp(prefix(key), defaultValue)
    }

    override fun forUser(key: String?, defaultValue: String?): StringPreference {
        return wrapped.forUser(prefix(key), defaultValue)
    }

    override fun forApp(key: String?, defaultValue: String?): StringPreference {
        return wrapped.forApp(prefix(key), defaultValue)
    }

    override fun forUser(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference {
        return wrapped.forUser(prefix(key), defaultValue)
    }

    override fun forApp(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference {
        return wrapped.forApp(prefix(key), defaultValue)
    }

    override fun <E> forUser(
        key: String?,
        clazz: Class<E?>?,
        defaultValue: E?
    ): EnumPreference<E?> {
        return wrapped.forUser(prefix(key), clazz, defaultValue)
    }

    override fun <E> forApp(
        key: String?,
        clazz: Class<E?>?,
        defaultValue: E?
    ): EnumPreference<E?> {
        return wrapped.forApp(prefix(key), clazz, defaultValue)
    }

    override fun group(name: String?): Preferences {
        return PrefixPreferences(this, name)
    }

    override fun changes(): Flow<String?> {
        return wrapped.changes().filter { key -> key?.startsWith(prefix!!) == true }
    }
}
