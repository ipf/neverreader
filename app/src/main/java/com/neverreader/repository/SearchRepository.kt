package com.neverreader.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences("recent_searches", Context.MODE_PRIVATE)

    fun getRecentSearches(): Flow<List<String>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(load())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(load())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun addRecentSearch(text: String) {
        val current = load().toMutableList()
        current.remove(text)
        current.add(0, text)
        while (current.size > MAX) current.removeAt(current.size - 1)
        prefs.edit().putString(KEY, current.joinToString("\n")).apply()
    }

    private fun load(): List<String> =
        prefs.getString(KEY, null)?.split("\n")?.filter { it.isNotBlank() }.orEmpty()

    private companion object {
        const val KEY = "recent"
        const val MAX = 8
    }
}
