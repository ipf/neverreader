package com.neverreader.backend.wallabag

import com.neverreader.backend.Backend
import com.neverreader.backend.model.Iso
import com.neverreader.backend.model.Account
import com.neverreader.backend.model.Annotation
import com.neverreader.backend.model.BackendCapabilities
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.ListFilter
import com.neverreader.backend.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

object WallabagAuth {
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String,
        clientId: String,
        clientSecret: String,
        http: OkHttpClient = OkHttpClient(),
    ): AuthResult = token(
        serverUrl,
        mapOf(
            "grant_type" to "password",
            "username" to username,
            "password" to password,
            "client_id" to clientId,
            "client_secret" to clientSecret,
        ),
        http,
    )

    suspend fun refresh(
        serverUrl: String,
        refreshToken: String,
        clientId: String,
        clientSecret: String,
        http: OkHttpClient = OkHttpClient(),
    ): AuthResult = token(
        serverUrl,
        mapOf(
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "client_id" to clientId,
            "client_secret" to clientSecret,
        ),
        http,
    )

    private suspend fun token(serverUrl: String, params: Map<String, String>, http: OkHttpClient): AuthResult {
        val body = FormBody.Builder().apply { params.forEach { (k, v) -> add(k, v) } }.build()
        val request = Request.Builder()
            .url(serverUrl.trimEnd('/') + "/oauth/v2/token")
            .post(body)
            .build()
        val response = withContext(Dispatchers.IO) { http.newCall(request).execute() }
        response.use {
            val text = it.body?.string().orEmpty()
            if (it.isSuccessful) {
                val parsed = json.decodeFromString<TokenResponse>(text)
                return AuthResult(parsed.accessToken, parsed.refreshToken)
            }
            val error = runCatching { json.decodeFromString<ErrorResponse>(text) }.getOrNull()
            throw IOException(
                "wallabag login failed: ${error?.errorDescription ?: error?.error ?: "HTTP ${it.code}"}",
            )
        }
    }
}

data class AuthResult(val accessToken: String, val refreshToken: String?)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
)

@Serializable
private data class ErrorResponse(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
private data class WallabagEntry(
    val id: Long,
    val url: String = "",
    val title: String? = null,
    val content: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("is_archived") val isArchived: Int = 0,
    @SerialName("is_starred") val isStarred: Int = 0,
    @SerialName("reading_time") val readingTime: Int = 0,
    @SerialName("preview_picture") val previewPicture: String? = null,
    val tags: List<WallabagTag> = emptyList(),
)

@Serializable
private data class WallabagTag(val id: Long, val label: String)

@Serializable
private data class WallabagEntries(
    val total: Int = 0,
    @SerialName("_embedded") val embedded: Embedded = Embedded(),
) {
    @Serializable
    internal data class Embedded(val items: List<WallabagEntry> = emptyList())
}

@Serializable
private data class WallabagAnnotation(
    val id: Long,
    val quote: String = "",
    val ranges: List<Range> = emptyList(),
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
) {
    @Serializable
    internal data class Range(
        @SerialName("startOffset") val startOffset: Int = 0,
        @SerialName("endOffset") val endOffset: Int = 0,
        val start: String = "",
        val end: String = "",
    )
}

private val htmlTags = Regex("<[^>]*>")

private fun WallabagEntry.toBookmark() = Bookmark(
    id = id.toString(),
    url = url,
    title = title ?: url,
    excerpt = content?.let { htmlTags.replace(it, " ").replace(Regex("\\s+"), " ").trim().take(200) }.orEmpty(),
    imageUrl = previewPicture,
    unread = isArchived == 0,
    favorite = isStarred == 1,
    readingTimeMinutes = readingTime,
    createdAt = createdAt?.let(Iso::toMillis) ?: 0L,
    updatedAt = updatedAt?.let(Iso::toMillis) ?: 0L,
    tags = tags.map { it.label },
)

class WallabagAdapter(
    private var account: Account,
    private val http: OkHttpClient = OkHttpClient(),
    private val onTokensRefreshed: suspend (Account) -> Unit = {},
) : Backend {

    override val type = BackendType.WALLABAG
    override val capabilities = BackendCapabilities(
        highlights = true,
        serverSearch = true,
        // updatedSince reports changes but not deletions; the repository does a periodic full refresh instead.
        deltaSync = true,
    )

    private fun url(path: String, query: Map<String, String?> = emptyMap()): String {
        val builder = (account.serverUrl.trimEnd('/') + "/api" + path).toHttpUrl().newBuilder()
        query.forEach { (key, value) -> if (value != null) builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private suspend fun execute(request: Request, retried: Boolean = false): String {
        val response = withContext(Dispatchers.IO) { http.newCall(request).execute() }
        if (response.code == 401 && !retried && account.refreshToken != null) {
            response.close()
            refreshTokens()
            return execute(
                request.newBuilder().header("Authorization", "Bearer ${account.accessToken}").build(),
                retried = true,
            )
        }
        response.use {
            val text = it.body?.string().orEmpty()
            check(it.isSuccessful) { "wallabag ${request.method} failed: HTTP ${it.code} ${text.take(200)}" }
            return text
        }
    }

    private suspend fun refreshTokens() {
        val refreshToken = account.refreshToken ?: return
        val result = WallabagAuth.refresh(
            account.serverUrl,
            refreshToken,
            account.clientId.orEmpty(),
            account.clientSecret.orEmpty(),
            http,
        )
        account = account.copy(accessToken = result.accessToken, refreshToken = result.refreshToken ?: refreshToken)
        onTokensRefreshed(account)
    }

    private fun authorized(url: String, method: String = "GET", body: okhttp3.RequestBody? = null) =
        Request.Builder()
            .url(url)
            .method(method, body)
            .header("Authorization", "Bearer ${account.accessToken}")
            .build()

    private fun form(vararg pairs: Pair<String, String>): okhttp3.RequestBody =
        FormBody.Builder().apply { pairs.forEach { (k, v) -> add(k, v) } }.build()

    override suspend fun listBookmarks(filter: ListFilter, limit: Int, offset: Int): List<Bookmark> {
        val query = buildMap {
            put("page", (offset / limit + 1).toString())
            put("perPage", limit.toString())
            put("sort", "created")
            put("order", "desc")
            filter.unread?.let { put("archive", if (it) "0" else "1") }
            filter.favorite?.let { put("star", "1") }
            filter.tag?.let { put("tags", it) }
            filter.search?.let { put("search", it) }
        }
        val text = execute(authorized(url("/entries.json", query)))
        return json.decodeFromString<WallabagEntries>(text).embedded.items.map { it.toBookmark() }
    }

    override suspend fun fetchArticleHtml(id: String): String {
        val text = execute(authorized(url("/entries/$id.json")))
        return json.decodeFromString<WallabagEntry>(text).content.orEmpty()
    }

    override suspend fun addBookmark(url: String, title: String?): Bookmark {
        val pairs = mutableListOf("url" to url)
        title?.let { pairs.add("title" to it) }
        val text = execute(authorized(url("/entries.json"), "POST", form(*pairs.toTypedArray())))
        return json.decodeFromString<WallabagEntry>(text).toBookmark()
    }

    override suspend fun setArchived(id: String, archived: Boolean) =
        patch(id, form("archive" to archived.toString()))

    override suspend fun setFavorite(id: String, favorite: Boolean) =
        patch(id, form("star" to favorite.toString()))

    private suspend fun patch(id: String, body: okhttp3.RequestBody) {
        execute(authorized(url("/entries/$id.json"), "PATCH", body))
    }

    override suspend fun deleteBookmark(id: String) {
        execute(authorized(url("/entries/$id.json"), "DELETE"))
    }

    override suspend fun listTags(): List<Tag> {
        val text = execute(authorized(url("/tags.json")))
        // wallabag does not report tag counts; the UI computes them locally.
        return json.decodeFromString<List<WallabagTag>>(text).map { Tag(it.label, 0) }
    }

    override suspend fun addTags(bookmarkId: String, names: List<String>) {
        execute(
            authorized(
                url("/entries/$bookmarkId/tags.json"),
                "POST",
                form("tags" to names.joinToString(",")),
            ),
        )
    }

    override suspend fun removeTag(bookmarkId: String, name: String) {
        val text = execute(authorized(url("/entries/$bookmarkId/tags.json")))
        val tagId = json.decodeFromString<List<WallabagTag>>(text)
            .firstOrNull { it.label.equals(name, ignoreCase = true) }?.id
            ?: return
        execute(authorized(url("/entries/$bookmarkId/tags/$tagId.json"), "DELETE"))
    }

    override suspend fun listAnnotations(bookmarkId: String): List<Annotation> {
        val text = execute(authorized(url("/annotations/$bookmarkId.json")))
        return json.decodeFromString<List<WallabagAnnotation>>(text).map { a ->
            val range = a.ranges.firstOrNull()
            Annotation(
                id = a.id.toString(),
                bookmarkId = bookmarkId,
                text = a.quote,
                comment = a.comment,
                startSelector = range?.start.orEmpty(),
                startOffset = range?.startOffset ?: 0,
                endSelector = range?.end.orEmpty(),
                endOffset = range?.endOffset ?: 0,
                createdAt = a.createdAt?.let(Iso::toMillis) ?: 0L,
            )
        }
    }

    override suspend fun addAnnotation(bookmarkId: String, annotation: Annotation): Annotation {
        val payload = buildJsonObject {
            put("quote", annotation.text)
            put("comment", annotation.comment ?: "")
            put(
                "ranges",
                kotlinx.serialization.json.buildJsonArray {
                    add(
                        buildJsonObject {
                            put("start", annotation.startSelector)
                            put("startOffset", annotation.startOffset)
                            put("end", annotation.endSelector)
                            put("endOffset", annotation.endOffset)
                        },
                    )
                },
            )
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val text = execute(authorized(url("/annotations/$bookmarkId.json"), "POST", body))
        val created = json.decodeFromString<WallabagAnnotation>(text)
        return annotation.copy(id = created.id.toString())
    }

    override suspend fun deleteAnnotation(bookmarkId: String, annotationId: String) {
        execute(authorized(url("/annotations/$annotationId.json"), "DELETE"))
    }

    override suspend fun changedBookmarks(since: Long): List<Bookmark> {
        val query = mapOf("updatedSince" to Iso.fromMillis(since), "perPage" to "30", "sort" to "updated", "order" to "asc")
        val result = mutableListOf<WallabagEntry>()
        var page = 1
        while (true) {
            val text = execute(authorized(url("/entries.json", query + mapOf("page" to page.toString()))))
            val entries = json.decodeFromString<WallabagEntries>(text)
            result.addAll(entries.embedded.items)
            if (entries.embedded.items.size < 30 || result.size >= entries.total) break
            page++
        }
        return result.map { it.toBookmark() }
    }

    // ponytail: wallabag cannot report deletions via updatedSince; deletions propagate at the
    // periodic full refresh (see BookmarkRepository), upgrade path: compare-by-id full scan.
    override suspend fun deletedBookmarkIds(since: Long): List<String>? = null
}
