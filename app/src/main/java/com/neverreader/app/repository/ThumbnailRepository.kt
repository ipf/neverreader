package com.neverreader.app.repository

import android.util.LruCache
import com.neverreader.app.list.hostOf
import com.neverreader.backend.repo.AccountManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches article thumbnails.
 *
 * This deliberately does not use Coil's own HTTP stack. Coil 3.4 keeps its
 * OkHttp fetcher at `DeprecationLevel.HIDDEN`, so there is no public way to give
 * it the bearer token that Readeck requires for `/api/...` images, and going
 * through Coil would also mean the image library deciding which hosts to
 * contact. Fetching here keeps that decision in the app: a request only ever
 * carries the Authorization header when it is going to the user's own server.
 *
 * Callers are expected to have already decided whether an image may be loaded at
 * all - see `MyListViewModel`.
 */
@Singleton
class ThumbnailRepository @Inject constructor(
    private val accountManager: AccountManager,
) {
    private val client = OkHttpClient()
    private val cache = object : LruCache<String, ByteArray>(CACHE_ENTRIES) {
        override fun sizeOf(key: String, value: ByteArray) = value.size
    }

    /** Returns the image bytes, or null if it could not be fetched. */
    suspend fun load(url: String): ByteArray? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }

        val account = accountManager.activeCached
        val isOurServer = account != null &&
            runCatching { java.net.URI(url).host }
                .getOrNull()
                ?.equals(hostOf(account.serverUrl), ignoreCase = true) == true
        val token = account?.accessToken

        val request = Request.Builder().url(url).apply {
            if (isOurServer && !token.isNullOrBlank()) {
                header("Authorization", "Bearer $token")
            }
        }.build()

        runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                response.body.bytes().also { cache.put(url, it) }
            }
        }.getOrNull()
    }

    private companion object {
        /** Bounded by total bytes rather than entry count, so a few large images cannot evict everything. */
        const val CACHE_ENTRIES = 8 * 1024 * 1024
    }
}
