package com.neverreader.backend.wallabag

import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.model.ListFilter
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WallabagAdapterTest {

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

    private fun account(token: String = "token") = Account(
        backendType = BackendType.WALLABAG,
        serverUrl = server.url("/").toString().trimEnd('/'),
        username = "user",
        accessToken = token,
        refreshToken = "refresh",
        clientId = "client",
        clientSecret = "secret",
    )

    @Test
    fun `list bookmarks maps entry fields`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"total":1,"page":1,"pages":1,"limit":30,
                    "_embedded":{"items":[{"id":42,"url":"https://example.com",
                    "title":"Hello","content":"<p>body</p>",
                    "created_at":"2024-01-01T10:00:00+0000",
                    "updated_at":"2024-01-02T10:00:00+0000",
                    "is_archived":0,"is_starred":1,"reading_time":4,
                    "preview_picture":"https://img",
                    "tags":[{"id":1,"label":"news"}]}]}}""",
            ),
        )
        val bookmarks = WallabagAdapter(account()).listBookmarks(ListFilter(unread = true, favorite = true), 30, 0)

        assertEquals(1, bookmarks.size)
        val bookmark = bookmarks[0]
        assertEquals("42", bookmark.id)
        assertTrue(bookmark.unread)
        assertTrue(bookmark.favorite)
        assertEquals("news", bookmark.tags[0])
        assertEquals("https://img", bookmark.imageUrl)
        assertTrue(bookmark.excerpt.contains("body"))

        val request = server.takeRequest()
        assertEquals("Bearer token", request.getHeader("Authorization"))
        val path = request.requestUrl!!.encodedQuery!!
        assertTrue(path.contains("archive=0"))
        assertTrue(path.contains("star=1"))
    }

    @Test
    fun `login exchanges password for token`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"access_token":"tok","refresh_token":"ref","token_type":"bearer","expires_in":3600}""",
            ),
        )
        val result = WallabagAuth.login(
            server.url("/").toString().trimEnd('/'),
            "user",
            "pass",
            "client",
            "secret",
        )
        assertEquals("tok", result.accessToken)
        assertEquals("ref", result.refreshToken)

        val request = server.takeRequest()
        assertEquals("/oauth/v2/token", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("grant_type=password"))
        assertTrue(body.contains("client_secret=secret"))
    }

    @Test
    fun `refreshes token on 401 and retries`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"invalid_token"}"""))
        server.enqueue(
            MockResponse().setBody("""{"access_token":"tok2","refresh_token":"ref2","expires_in":3600}"""),
        )
        server.enqueue(MockResponse().setBody("""{"total":0,"_embedded":{"items":[]}}"""))

        var refreshed: Account? = null
        val bookmarks = WallabagAdapter(account(), onTokensRefreshed = { refreshed = it })
            .listBookmarks(ListFilter(), 30, 0)

        assertEquals(0, bookmarks.size)
        assertEquals("tok2", refreshed?.accessToken)
        assertEquals("ref2", refreshed?.refreshToken)
    }

    @Test
    fun `archive patches entry`() = runTest {
        server.enqueue(MockResponse().setBody("{}"))
        WallabagAdapter(account()).setArchived("42", true)

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/entries/42.json", request.path)
        assertTrue(request.body.readUtf8().contains("archive=true"))
    }

    @Test
    fun `add bookmark posts url`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"id":7,"url":"https://example.com","title":"Added",
                    "is_archived":0,"is_starred":0,
                    "created_at":"2024-01-01T10:00:00+0000",
                    "updated_at":"2024-01-01T10:00:00+0000"}""",
            ),
        )
        val bookmark = WallabagAdapter(account()).addBookmark("https://example.com", "Title")

        assertEquals("7", bookmark.id)
        assertEquals("Added", bookmark.title)
        val request = server.takeRequest()
        assertEquals("/api/entries.json", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("url=https%3A%2F%2Fexample.com"))
    }
}
