package com.neverreader.util.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart

class Prefs(private val user: Store, private val app: Store) : Preferences {
    override fun clearUser() {
        user.clear()
    }

    override fun clear() {
        user.clear()
        app.clear()
    }

    override fun remove(key: String?) {
        user.remove(key)
        app.remove(key)
    }

    override fun appKeys(): MutableSet<String?>? {
        return app.keys()
    }

    override fun userKeys(): MutableSet<String?>? {
        return user.keys()
    }


    override fun forUser(key: String?, defaultValue: Boolean): BooleanPreference {
        return BooleanPref(key, defaultValue, user)
    }

    override fun forApp(key: String?, defaultValue: Boolean): BooleanPreference {
        return BooleanPref(key, defaultValue, app)
    }


    override fun forUser(key: String?, defaultValue: Float): FloatPreference {
        return FloatPref(key, defaultValue, user)
    }

    override fun forApp(key: String?, defaultValue: Float): FloatPreference {
        return FloatPref(key, defaultValue, app)
    }


    override fun forUser(key: String?, defaultValue: Int): IntPreference {
        return IntPref(key, defaultValue, user)
    }

    override fun forApp(key: String?, defaultValue: Int): IntPreference {
        return IntPref(key, defaultValue, app)
    }


    override fun forUser(key: String?, defaultValue: Long): LongPreference {
        return LongPref(key, defaultValue, user)
    }

    override fun forApp(key: String?, defaultValue: Long): LongPreference {
        return LongPref(key, defaultValue, app)
    }


    override fun forUser(key: String?, defaultValue: String?): StringPreference {
        return StringPref(key, defaultValue, user, false)
    }

    override fun forApp(key: String?, defaultValue: String?): StringPreference {
        return StringPref(key, defaultValue, app, false)
    }


    override fun <E : Enum<E>> forUser(
        key: String?,
        clazz: Class<E>?,
        defaultValue: E?,
    ): EnumPreference<E> {
        return EnumPref(clazz!!, key, defaultValue, user)
    }

    override fun <E : Enum<E>> forApp(
        key: String?,
        clazz: Class<E>?,
        defaultValue: E?,
    ): EnumPreference<E> {
        return EnumPref(clazz!!, key, defaultValue, app)
    }

    override fun forUser(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference {
        return StringSetPref(key, defaultValue, user, false)
    }

    override fun forApp(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference {
        return StringSetPref(key, defaultValue, app, false)
    }

    override fun group(name: String?): Preferences {
        return PrefixPreferences(this, name)
    }

    override fun changes(): Flow<String?> {
        return merge(user.changes(), app.changes())
    }
}
