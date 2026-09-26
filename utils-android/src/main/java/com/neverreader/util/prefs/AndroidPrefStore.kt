package com.neverreader.util.prefs

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.ObservableOnSubscribe
import io.reactivex.functions.Cancellable
import io.reactivex.functions.Function
import io.reactivex.functions.Predicate
import java.util.Collections

/**
 * A [Store] backed by Android [SharedPreferences]
 */
class AndroidPrefStore(private val prefs: SharedPreferences) : Store {
    override fun changes(): Observable<String?>? {
        return Observable.create<String?>(ObservableOnSubscribe { emitter: ObservableEmitter<String?>? ->
            val listener =
                OnSharedPreferenceChangeListener { sharedPreferences: SharedPreferences?, key: String? ->
                    if (key == null) return@OnSharedPreferenceChangeListener  // OnSharedPreferenceChangeListener.onSharedPreferenceChanged makes a callback with a null key whenever clear() is called, which we don't need to emit.
                    emitter!!.onNext(key)
                }
            emitter!!.setCancellable(Cancellable {
                prefs.unregisterOnSharedPreferenceChangeListener(
                    listener
                )
            })
            prefs.registerOnSharedPreferenceChangeListener(listener)
        })
    }

    private fun <T> changes(key: String?, getter: Get<T?>): Observable<T?>? {
        return changes()!!
            .filter(Predicate { changed: String? -> changed == key })
            .map<T?>(Function { k: String? -> getter.get(key) })
    }

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

    override fun stringChanges(key: String?): Observable<String?>? {
        return changes<String?>(key) { key: String? -> this.getString(key) }
    }


    override fun getStringSet(key: String?): MutableSet<String?>? {
        val v = prefs.getStringSet(key, null)
        return if (v != null) Collections.unmodifiableSet<String?>(v) else null
    }

    override fun set(key: String?, value: MutableSet<String?>?) {
        var value = value
        value =
            if (value != null) HashSet<String?>(value) else null // Make a copy so the set we are writing won't throw concurrent mod exceptions
        prefs.edit().putStringSet(key, value).apply()
    }

    override fun stringSetChanges(key: String?): Observable<MutableSet<String?>?>? {
        return changes<MutableSet<String?>?>(
            key,
            AndroidPrefStore.Get { key: String? -> this.getStringSet(key) })
    }


    override fun getInt(key: String?): Int {
        return prefs.getInt(key, 0)
    }

    override fun set(key: String?, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    override fun intChanges(key: String?): Observable<Int?>? {
        return changes<Int?>(key, AndroidPrefStore.Get { key: String? -> this.getInt(key) })
    }


    override fun getFloat(key: String?): Float {
        return prefs.getFloat(key, 0f)
    }

    override fun set(key: String?, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    override fun floatChanges(key: String?): Observable<Float?>? {
        return changes<Float?>(key, AndroidPrefStore.Get { key: String? -> this.getFloat(key) })
    }


    override fun getLong(key: String?): Long {
        return prefs.getLong(key, 0)
    }

    override fun set(key: String?, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    override fun longChanges(key: String?): Observable<Long?>? {
        return changes<Long?>(key, AndroidPrefStore.Get { key: String? -> this.getLong(key) })
    }


    override fun getBoolean(key: String?): Boolean {
        return prefs.getBoolean(key, false)
    }

    override fun set(key: String?, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    override fun booleanChanges(key: String?): Observable<Boolean?>? {
        return changes<Boolean?>(key, AndroidPrefStore.Get { key: String? -> this.getBoolean(key) })
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}
