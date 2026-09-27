package com.neverreader.backend.readeck

import com.neverreader.backend.model.ListFilter
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadeckAdapterTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun adapter() = ReadeckAdapter(
        serverUrl = server.url("/").toString().trimEnd('/'),
        clientId = "client-id",
        accessToken = "token",
    )

    /**
     * The single-article view is what the reader renders. It returns text/html
     * rather than JSON, and the id is an opaque server string, so both the
     * Accept header and the path escaping are pinned here.
     */
    @Test
    fun `fetch article html asks for html and escapes the id`() = runTest {
        server.enqueue(MockResponse().setBody("<p>Article body</p>"))

        val html = adapter().fetchArticleHtml("a/b c")

        assertEquals("<p>Article body</p>", html)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/bookmarks/a%2Fb%20c/article", request.path)
        assertEquals("text/html", request.getHeader("Accept"))
        assertEquals("Bearer token", request.getHeader("Authorization"))
    }

    @Test
    fun `fetch article html reports the status when the server refuses`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("not found"))

        val error = runCatching { adapter().fetchArticleHtml("abc") }.exceptionOrNull()

        assertTrue("$error", error is IllegalStateException)
        assertTrue("$error", error!!.message!!.contains("404"))
    }

    @Test
    fun `list bookmarks maps fields and sends filters`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"id":"abc","url":"https://example.com","title":"Hello",
                    "description":"desc","is_marked":true,"is_archived":false,
                    "reading_time":7,"created":"2024-01-01T10:00:00+00:00",
                    "updated":"2024-01-02T10:00:00+00:00","labels":["news"],
                    "resources":{"thumbnail":{"src":"https://img"}}}]""",
            ),
        )
        val bookmarks = adapter().listBookmarks(ListFilter(unread = true), 30, 60)

        assertEquals(1, bookmarks.size)
        val bookmark = bookmarks[0]
        assertEquals("abc", bookmark.id)
        assertTrue(bookmark.unread)
        assertTrue(bookmark.favorite)
        assertEquals(listOf("news"), bookmark.tags)
        assertEquals(7, bookmark.readingTimeMinutes)
        assertEquals("https://img", bookmark.imageUrl)

        val request = server.takeRequest()
        assertEquals("Bearer token", request.getHeader("Authorization"))
        val path = request.requestUrl!!.encodedQuery!!
        assertTrue(path.contains("is_archived=false"))
        assertTrue(path.contains("limit=30"))
        assertTrue(path.contains("offset=60"))
    }

    @Test
    fun `add bookmark posts url`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"id":"new1","url":"https://example.com","title":"Added",
                    "is_marked":false,"is_archived":false,"created":"2024-01-01T10:00:00+00:00",
                    "updated":"2024-01-01T10:00:00+00:00"}""",
            ),
        )
        val bookmark = adapter().addBookmark("https://example.com", "Title")

        assertEquals("new1", bookmark.id)
        assertEquals("Added", bookmark.title)
        val request = server.takeRequest()
        assertEquals("/api/bookmarks", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"url\":\"https://example.com\""))
    }

    @Test
    fun `set archived patches bookmark`() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        adapter().setArchived("abc", true)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/bookmarks/abc", request.path)
        assertTrue(request.body.readUtf8().contains("\"is_archived\":true"))
    }

    @Test
    fun `device flow registers client and polls token`() = runTest {
        server.enqueue(
            MockResponse().setBody("""{"client_id":"cid123","client_name":"NeverReader"}"""),
        )
        val clientId = ReadeckAuth.registerClient(server.url("/").toString().trimEnd('/'), "1.2.3")
        assertEquals("cid123", clientId)
        assertEquals("/api/oauth/client", server.takeRequest().path)

        server.enqueue(
            MockResponse().setBody(
                """{"device_code":"dc","user_code":"1234-5678",
                    "verification_uri":"https://host/oauth/authorize",
                    "expires_in":1200,"interval":0}""",
            ),
        )
        // first poll pending, second poll success
        server.enqueue(
            MockResponse().setResponseCode(400).setBody("""{"error":"authorization_pending"}"""),
        )
        server.enqueue(
            MockResponse().setBody("""{"access_token":"tok","token_type":"Bearer","scope":"bookmarks:read"}"""),
        )

        val session = ReadeckAuth.startDeviceFlow(server.url("/").toString().trimEnd('/'), clientId)
        assertEquals("dc", session.deviceCode)
        server.takeRequest()

        val token = ReadeckAuth.awaitToken(
            server.url("/").toString().trimEnd('/'),
            clientId,
            session.copy(interval = 0),
            maxAttempts = 3,
        )
        assertEquals("tok", token)
        val poll = server.takeRequest()
        // Readeck registers only application/json on /oauth/token.
        assertTrue(poll.getHeader("Content-Type")!!.startsWith("application/json"))
        val body = poll.body.readUtf8()
        assertTrue(body, body.contains("urn:ietf:params:oauth:grant-type:device_code"))
        assertTrue(body, body.contains("\"device_code\":\"dc\""))
    }

    @Test
    fun `changed bookmarks uses sync endpoint`() = runTest {
        server.enqueue(
            MockResponse().setBody("""[{"id":"abc","type":"update"},{"id":"def","type":"delete"}]"""),
        )
        server.enqueue(
            MockResponse().setBody(
                """[{"id":"abc","url":"https://example.com","title":"Changed",
                    "is_marked":false,"is_archived":false,
                    "created":"2024-01-01T10:00:00+00:00","updated":"2024-01-03T10:00:00+00:00"}]""",
            ),
        )
        val changed = adapter().changedBookmarks(1_700_000_000_000)

        assertEquals(1, changed.size)
        assertEquals("abc", changed[0].id)
        val list = server.takeRequest()
        assertTrue(list.path!!.startsWith("/api/bookmarks/sync"))
        server.takeRequest()
    }
}
