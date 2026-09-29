package com.neverreader.util.prefs

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import java.util.Collections

/**
 * A [Store] backed by Android [SharedPreferences]
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

    override fun keys(): MutableSet<String?> {
        return prefs.all.keys
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


    override fun getStringSet(key: String?): MutableSet<String?>? {
        val v = prefs.getStringSet(key, null)
        return if (v != null) Collections.unmodifiableSet<String?>(v) else null
    }

    override fun set(key: String?, value: MutableSet<String?>?) {
        var value = value
        value =
            if (value != null) HashSet(value) else null // Make a copy so the set we are writing won't throw concurrent mod exceptions
        prefs.edit().putStringSet(key, value).apply()
    }

    override fun stringSetChanges(key: String?): Flow<MutableSet<String?>?> {
        return changes<MutableSet<String?>?>(
            key
        ) { key: String? -> this.getStringSet(key) }
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


    override fun getFloat(key: String?): Float {
        return prefs.getFloat(key, 0f)
    }

    override fun set(key: String?, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun floatChanges(key: String?): Flow<Float?> {
        return changes<Float?>(key) { key: String? -> this.getFloat(key) }
    }


    override fun getLong(key: String?): Long {
        return prefs.getLong(key, 0)
    }

    override fun set(key: String?, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun longChanges(key: String?): Flow<Long?> {
        return changes<Long?>(key) { key: String? -> this.getLong(key) }
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

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
