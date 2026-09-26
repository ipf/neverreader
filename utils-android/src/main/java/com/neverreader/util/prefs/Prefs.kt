package com.neverreader.util.prefs

import io.reactivex.Observable

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
        return StringPref(key, defaultValue, user, false, null)
    }

    override fun forApp(key: String?, defaultValue: String?): StringPreference {
        return StringPref(key, defaultValue, app, false, null)
    }


    override fun <E> forUser(
        key: String?,
        clazz: Class<E?>?,
        defaultValue: E?,
    ): EnumPreference<E?> {
        return EnumPref(clazz!!, key, defaultValue, user)
    }

    override fun <E> forApp(
        key: String?,
        clazz: Class<E?>?,
        defaultValue: E?,
    ): EnumPreference<E?> {
        return EnumPref(clazz!!, key, defaultValue, app)
    }

    override fun forUser(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference {
        return StringSetPref(
            key, defaultValue, user,
            isSet = false,
            withChanges = null
        )
    }

    override fun forApp(key: String?, defaultValue: MutableSet<String?>?): StringSetPreference {
        return StringSetPref(key, defaultValue, app, false, null)
    }

    override fun group(name: String?): Preferences {
        // TODO also add some protection so someone can't create a preference with this prefix
        return PrefixPreferences(this, name)
    }

    override fun changes(): Observable<String?>? {
        return Observable.merge<String?>(user.changes(), app.changes())
    }
}
