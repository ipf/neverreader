package com.neverreader.util.prefs

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

/**
 * A [Store] backed by Android [SharedPreferences].
 *
 * SharedPreferences rather than DataStore: there are three keys, it is not
 * deprecated, and it reads synchronously. The theme is needed synchronously
 * while the first activity is starting, and DataStore is an async API, so
 * moving would mean caching a value that is currently just read.
 */
class AndroidPrefStore(private val prefs: SharedPreferences) : Store {
    /**
     * Cold: the listener is registered when the flow is collected and unregistered
     * when a collection stops.
     */
    override fun changes(): Flow<String> = callbackFlow {
        val listener = OnSharedPreferenceChangeListener { _, key ->
            // A null key means clear() was called; there is no single key to report.
            if (key != null) trySend(key)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun <T> changes(key: String?, getter: Get<T?>): Flow<T?> =
        changes()
            .filter { changed -> changed == key }
            .map { getter.get(key) }

    internal fun interface Get<T> {
        fun get(key: String?): T?
    }

    override fun contains(key: String?): Boolean {
        return prefs.contains(key)
    }

    override fun remove(key: String?) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    override fun getString(key: String?): String? {
        return prefs.getString(key, null)
    }

    override fun set(key: String?, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    override fun stringChanges(key: String?): Flow<String?> {
        return changes<String?>(key) { key: String? -> this.getString(key) }
    }

    override fun getInt(key: String?): Int {
        return prefs.getInt(key, 0)
    }

    override fun set(key: String?, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun intChanges(key: String?): Flow<Int?> {
        return changes<Int?>(key) { key: String? -> this.getInt(key) }
    }

    override fun getBoolean(key: String?): Boolean {
        return prefs.getBoolean(key, false)
    }

    override fun set(key: String?, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun booleanChanges(key: String?): Flow<Boolean?> {
        return changes<Boolean?>(key) { key: String? -> this.getBoolean(key) }
    }
}
