package com.neverreader.backend.readeck

import com.neverreader.backend.Backend
import com.neverreader.backend.model.Iso
import com.neverreader.backend.model.Annotation
import com.neverreader.backend.model.BackendCapabilities
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.ListFilter
import com.neverreader.backend.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

private val json = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

data class DeviceSession(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val interval: Long,
)

/**
 * A pending authorization-code exchange: the PKCE verifier and CSRF state the
 * flow was started with, which the redirect has to be matched against before
 * the code can be redeemed.
 */
data class PendingAuthorization(
    val clientId: String,
    val serverUrl: String,
    val codeVerifier: String,
    val state: String,
    val authorizeUrl: String,
)

object ReadeckAuth {
    // Readeck serves the API under /api.
    private fun base(serverUrl: String) = serverUrl.trimEnd('/') + "/api"

    /**
     * Where the server sends the browser back to. Readeck's own documentation
     * for the field allows "any other app link scheme", so a custom scheme is
     * sanctioned; verified https App Links are not, because we do not control
     * the domain a self-hosted instance runs on.
     *
     * PKCE is what makes this acceptable: an app that intercepts the redirect
     * gets the code but not the verifier, so it cannot redeem it.
     */
    const val REDIRECT_URI: String = "com.neverreader.app://oauth-callback"

    private const val SCOPE = "bookmarks:read bookmarks:write profile:read"

    /**
     * @param softwareVersion the installed app version, reported to the server so
     *   Readeck can show which client a token was issued to. Readeck rejects the
     *   whole registration as `invalid_client_metadata` if it is missing.
     * @param redirectUri required, and the only reason [registerClient] is not
     *   simply "register a device-flow client": the field is rejected unless
     *   `grant_types` contains `authorization_code`, and rejected as missing
     *   when it does.
     */
    suspend fun registerClient(
        serverUrl: String,
        softwareVersion: String,
        redirectUri: String? = null,
        http: OkHttpClient = OkHttpClient(),
    ): String {
        val body = buildJsonObject {
            put("client_name", "NeverReader")
            // Readeck requires an https client_uri that resolves to a public
            // address, so this cannot be a local or project URL.
            put("client_uri", "https://readeck.org")
            put("software_id", "com.neverreader")
            put("software_version", softwareVersion)
            // Both grants, so one ephemeral client serves either sign-in path.
            put(
                "grant_types",
                buildJsonArray {
                    add("urn:ietf:params:oauth:grant-type:device_code")
                    add("authorization_code")
                },
            )
            put("token_endpoint_auth_method", "none")
            if (redirectUri != null) {
                put("redirect_uris", buildJsonArray { add(redirectUri) })
            }
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(base(serverUrl) + "/oauth/client").post(body).build()
        val text = execute(http, request)
        return json.decodeFromString<ClientResponse>(text).clientId
            ?: error("client registration response missing client_id")
    }

    /**
     * Starts an authorization-code flow and returns everything needed to finish
     * it, including the url to hand to the browser.
     *
     * The verifier and state are generated here rather than by the caller so
     * they cannot end up mismatched between the authorize url and the exchange.
     */
    suspend fun startAuthorization(
        serverUrl: String,
        softwareVersion: String,
        redirectUri: String = REDIRECT_URI,
        scope: String = SCOPE,
        http: OkHttpClient = OkHttpClient(),
    ): PendingAuthorization {
        val clientId = registerClient(serverUrl, softwareVersion, redirectUri, http)
        val codeVerifier = newCodeVerifier()
        val state = newState()
        val challenge = codeChallenge(codeVerifier)
        val authorizeUrl = buildAuthorizeUrl(
            serverUrl = serverUrl,
            clientId = clientId,
            redirectUri = redirectUri,
            scope = scope,
            codeChallenge = challenge,
            state = state,
        )
        return PendingAuthorization(clientId, serverUrl, codeVerifier, state, authorizeUrl)
    }

    /**
     * The page the user logs in and approves on.
     *
     * `/authorize` is served from the instance root, not under `/api` like every
     * other endpoint, so the api suffix has to come off first.
     */
    fun buildAuthorizeUrl(
        serverUrl: String,
        clientId: String,
        redirectUri: String,
        scope: String,
        codeChallenge: String,
        state: String,
    ): String {
        val root = serverUrl.trimEnd('/').removeSuffix("/api").trimEnd('/')
        // Built through HttpUrl rather than string concatenation: the custom
        // scheme's '://' and the scope's spaces and colons are not legal
        // unescaped in a query value, and an unencoded redirect_uri does not
        // match the registered one, so the server refuses to redirect at all.
        return (root + "/authorize").toHttpUrl().newBuilder()
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("scope", scope)
            .addQueryParameter("code_challenge", codeChallenge)
            .addQueryParameter("code_challenge_method", "S256")
            .addQueryParameter("state", state)
            .build()
            .toString()
    }

    /**
     * Redeems the code the redirect carried. Unlike the device-code branch, this
     * request takes no `client_id`: the verifier is what identifies the exchange.
     */
    suspend fun exchangeCode(
        serverUrl: String,
        code: String,
        codeVerifier: String,
        http: OkHttpClient = OkHttpClient(),
    ): String {
        val body = buildJsonObject {
            put("grant_type", "authorization_code")
            put("code", code)
            put("code_verifier", codeVerifier)
        }.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(base(serverUrl) + "/oauth/token").post(body).build()
        val text = execute(http, request)
        return json.decodeFromString<TokenResponse>(text).accessToken
    }

    /**
     * A PKCE verifier: 43-128 characters from the RFC 7636 unreserved set.
     * 64 alphanumeric characters, matching the example in the server's spec.
     */
    fun newCodeVerifier(random: SecureRandom = SecureRandom()): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return buildString(64) {
            repeat(64) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
    }

    /** base64url(SHA-256(verifier)), unpadded, as RFC 7636 requires. */
    fun codeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    /** CSRF token, echoed back on the redirect and checked before redeeming. */
    fun newState(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(24)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    suspend fun startDeviceFlow(serverUrl: String, clientId: String, http: OkHttpClient = OkHttpClient()): DeviceSession {
        val body = FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", "bookmarks:read bookmarks:write profile:read")
            .build()
        val request = Request.Builder().url(base(serverUrl) + "/oauth/device").post(body).build()
        val text = execute(http, request)
        val parsed = json.decodeFromString<DeviceResponse>(text)
        return DeviceSession(
            deviceCode = parsed.deviceCode,
            userCode = parsed.userCode,
            verificationUri = parsed.verificationUri,
            verificationUriComplete = parsed.verificationUriComplete,
            interval = parsed.interval,
        )
    }

    suspend fun awaitToken(
        serverUrl: String,
        clientId: String,
        session: DeviceSession,
        maxAttempts: Int = 120,
        http: OkHttpClient = OkHttpClient(),
    ): String {
        var interval = session.interval
        repeat(maxAttempts) {
            delay(interval * 1000)
            val body = buildJsonObject {
                put("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                put("device_code", session.deviceCode)
                put("client_id", clientId)
            }.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(base(serverUrl) + "/oauth/token").post(body).build()
            val response = withContext(Dispatchers.IO) { http.newCall(request).execute() }
            response.use {
                val text = it.body.string()
                if (it.isSuccessful) return json.decodeFromString<TokenResponse>(text).accessToken
                val parsed = runCatching { json.decodeFromString<ErrorResponse>(text) }.getOrNull()
                when (val error = parsed?.error) {
                    "authorization_pending" -> Unit
                    "slow_down" -> interval += 5
                    else -> throw IOException(
                        "device flow failed: " +
                            (parsed?.errorDescription ?: error ?: "HTTP ${it.code}")
                    )
                }
            }
        }
        throw IOException("device flow timed out")
    }

    private suspend fun execute(http: OkHttpClient, request: Request): String = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val text = response.body.string()
            check(response.isSuccessful) { "Readeck request failed: HTTP ${response.code} ${text.take(200)}" }
            text
        }
    }
}

@Serializable
private data class ClientResponse(@SerialName("client_id") val clientId: String? = null)

@Serializable
private data class DeviceResponse(
    @SerialName("device_code") val deviceCode: String,
    @SerialName("user_code") val userCode: String,
    @SerialName("verification_uri") val verificationUri: String,
    @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
    val interval: Long = 5,
)

@Serializable
private data class TokenResponse(@SerialName("access_token") val accessToken: String)

@Serializable
private data class ErrorResponse(
    val error: String? = null,
    @SerialName("error_description") val errorDescription: String? = null,
)

@Serializable
private data class ReadeckBookmark(
    val id: String,
    val url: String = "",
    val title: String = "",
    val description: String? = null,
    @SerialName("is_marked") val isMarked: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("reading_time") val readingTime: Int = 0,
    val created: String = "",
    val updated: String = "",
    val labels: List<String> = emptyList(),
    val resources: JsonObject? = null,
)

@Serializable
private data class LabelInfo(val name: String, val count: Int = 0)

@Serializable
private data class SyncItem(val id: String, val type: String = "update")

@Serializable
private data class AnnotationInfo(
    val id: String,
    @SerialName("start_selector") val startSelector: String = "",
    @SerialName("start_offset") val startOffset: Int = 0,
    @SerialName("end_selector") val endSelector: String = "",
    @SerialName("end_offset") val endOffset: Int = 0,
    val text: String = "",
    val created: String = "",
)

private fun ReadeckBookmark.toBookmark() = Bookmark(
    id = id,
    url = url,
    title = title.ifBlank { url },
    excerpt = description.orEmpty(),
    imageUrl = imageUrl(resources),
    unread = !isArchived,
    favorite = isMarked,
    readingTimeMinutes = readingTime,
    createdAt = Iso.toMillis(created),
    updatedAt = Iso.toMillis(updated),
    tags = labels,
)

private fun imageUrl(resources: JsonObject?): String? =
    listOf("thumbnail", "image").firstNotNullOfOrNull { key ->
        (resources?.get(key) as? JsonObject)?.get("src")?.jsonPrimitive?.content
    }

class ReadeckAdapter(
    private val serverUrl: String,
    private val clientId: String,
    private val accessToken: String,
    private val http: OkHttpClient = OkHttpClient(),
) : Backend {

    override val type = BackendType.READECK
    override val capabilities = BackendCapabilities(
        highlights = true,
        serverSearch = true,
        deltaSync = true,
    )

    private fun url(path: String, query: Map<String, String?> = emptyMap()): String {
        val builder = (serverUrl.trimEnd('/') + "/api" + path).toHttpUrl().newBuilder()
        query.forEach { (key, value) -> if (value != null) builder.addQueryParameter(key, value) }
        return builder.build().toString()
    }

    private suspend fun execute(request: Request): String = withContext(Dispatchers.IO) {
        http.newCall(request).execute().use { response ->
            val text = response.body.string()
            check(response.isSuccessful) { "Readeck ${request.method} failed: HTTP ${response.code} ${text.take(200)}" }
            text
        }
    }

    private fun authorized(url: String, method: String = "GET", body: okhttp3.RequestBody? = null) =
        Request.Builder()
            .url(url)
            .method(method, body)
            .header("Authorization", "Bearer $accessToken")
            .build()

    override suspend fun listBookmarks(filter: ListFilter, limit: Int, offset: Int): List<Bookmark> {
        val query = buildMap {
            put("limit", limit.toString())
            put("offset", offset.toString())
            put("sort", "-created")
            filter.unread?.let { put("is_archived", (!it).toString()) }
            filter.favorite?.let { put("is_marked", it.toString()) }
            filter.tag?.let { put("labels", it) }
            filter.search?.let { put("search", it) }
        }
        val text = execute(authorized(url("/bookmarks", query)))
        return json.decodeFromString<List<ReadeckBookmark>>(text).map { it.toBookmark() }
    }

    override suspend fun fetchArticleHtml(id: String): String {
        // Readeck returns text/html here, not JSON. Ask for it, and escape the id:
        // it is an opaque server-generated string, so it is not safe to paste
        // straight into a path.
        val target = (serverUrl.trimEnd('/') + "/api").toHttpUrl().newBuilder()
            .addPathSegment("bookmarks")
            .addPathSegment(id)
            .addPathSegment("article")
            .build()
        return execute(
            authorized(target.toString())
                .newBuilder()
                .header("Accept", "text/html")
                .build(),
        )
    }

    override suspend fun addBookmark(url: String, title: String?): Bookmark {
        val payload = buildJsonObject {
            put("url", url)
            title?.let { put("title", it) }
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val text = execute(authorized(url("/bookmarks"), "POST", body))
        return json.decodeFromString<ReadeckBookmark>(text).toBookmark()
    }

    override suspend fun setArchived(id: String, archived: Boolean) =
        patch(id, buildJsonObject { put("is_archived", archived) })

    override suspend fun setFavorite(id: String, favorite: Boolean) =
        patch(id, buildJsonObject { put("is_marked", favorite) })

    private suspend fun patch(id: String, payload: kotlinx.serialization.json.JsonObject) {
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        execute(authorized(url("/bookmarks/$id"), "PATCH", body))
    }

    override suspend fun deleteBookmark(id: String) {
        execute(authorized(url("/bookmarks/$id"), "DELETE"))
    }

    override suspend fun listTags(): List<Tag> {
        val text = execute(authorized(url("/bookmarks/labels")))
        return json.decodeFromString<List<LabelInfo>>(text).map { Tag(it.name, it.count) }
    }

    override suspend fun addTags(bookmarkId: String, names: List<String>) {
        val current = currentLabels(bookmarkId)
        val merged = (current + names).distinct()
        setLabels(bookmarkId, merged)
    }

    override suspend fun removeTag(bookmarkId: String, name: String) {
        setLabels(bookmarkId, currentLabels(bookmarkId) - name)
    }

    private suspend fun currentLabels(bookmarkId: String): List<String> {
        val text = execute(authorized(url("/bookmarks/$bookmarkId")))
        return json.decodeFromString<ReadeckBookmark>(text).labels
    }

    private suspend fun setLabels(bookmarkId: String, labels: List<String>) {
        patch(
            bookmarkId,
            buildJsonObject {
                put("labels", kotlinx.serialization.json.JsonArray(labels.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            },
        )
    }

    override suspend fun listAnnotations(bookmarkId: String): List<Annotation> {
        val text = execute(authorized(url("/bookmarks/$bookmarkId/annotations")))
        return json.decodeFromString<List<AnnotationInfo>>(text).map {
            Annotation(
                id = it.id,
                bookmarkId = bookmarkId,
                text = it.text,
                comment = null,
                startSelector = it.startSelector,
                startOffset = it.startOffset,
                endSelector = it.endSelector,
                endOffset = it.endOffset,
                createdAt = Iso.toMillis(it.created),
            )
        }
    }

    override suspend fun addAnnotation(bookmarkId: String, annotation: Annotation): Annotation {
        val payload = buildJsonObject {
            put("start_selector", annotation.startSelector)
            put("start_offset", annotation.startOffset)
            put("end_selector", annotation.endSelector)
            put("end_offset", annotation.endOffset)
            put("color", "yellow")
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val text = execute(authorized(url("/bookmarks/$bookmarkId/annotations"), "POST", body))
        val created = json.decodeFromString<AnnotationInfo>(text)
        return annotation.copy(id = created.id, text = created.text.ifBlank { annotation.text })
    }

    override suspend fun deleteAnnotation(bookmarkId: String, annotationId: String) {
        execute(authorized(url("/bookmarks/$bookmarkId/annotations/$annotationId"), "DELETE"))
    }

    override suspend fun changedBookmarks(since: Long): List<Bookmark> {
        val text = execute(authorized(url("/bookmarks/sync", mapOf("since" to Iso.fromMillis(since)))))
        val items = json.decodeFromString<List<SyncItem>>(text)
        return fetchBookmarks(items.map { it.id })
    }

    override suspend fun deletedBookmarkIds(since: Long): List<String> {
        val text = execute(authorized(url("/bookmarks/sync", mapOf("since" to Iso.fromMillis(since)))))
        return json.decodeFromString<List<SyncItem>>(text)
            .filter { it.type == "delete" }
            .map { it.id }
    }

    private suspend fun fetchBookmarks(ids: List<String>): List<Bookmark> {
        if (ids.isEmpty()) return emptyList()
        val payload = buildJsonObject {
            put("id", kotlinx.serialization.json.JsonArray(ids.map { kotlinx.serialization.json.JsonPrimitive(it) }))
            put("sort", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("-updated"))))
        }
        val body = payload.toString().toRequestBody("application/json".toMediaType())
        val text = execute(authorized(url("/bookmarks/sync"), "POST", body))
        return json.decodeFromString<List<ReadeckBookmark>>(text).map { it.toBookmark() }
    }
}
