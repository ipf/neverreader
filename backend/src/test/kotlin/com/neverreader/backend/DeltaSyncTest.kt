package com.neverreader.backend

import com.neverreader.backend.model.Account
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.readeck.ReadeckAdapter
import com.neverreader.backend.wallabag.WallabagAdapter
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Delta sync, which is the one part of the two adapters that can disagree about
 * what a caller is meant to do.
 *
 * Both report changed bookmarks through `changedBookmarks`, but only Readeck can
 * report deletions: Wallabag's updatedSince has no notion of a tombstone, so its
 * `deletedBookmarkIds` returns null to mean "I cannot tell you", and the
 * repository falls back to a periodic full refresh. Both paths are pinned here
 * because the distinction is invisible in the interface and easy to break by
 * returning an empty list instead of null.
 */
class DeltaSyncTest {

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

    private val baseUrl get() = server.url("/").toString().trimEnd('/')

    private fun readeck() = ReadeckAdapter(
        serverUrl = baseUrl,
        clientId = "client-id",
        accessToken = "token",
    )

    private fun wallabag() = WallabagAdapter(
        Account(
            backendType = BackendType.WALLABAG,
            serverUrl = baseUrl,
            username = "user",
            accessToken = "token",
            refreshToken = null,
            clientId = "client",
            clientSecret = "secret",
        ),
    )

    // ---- Readeck ----------------------------------------------------------

    /**
     * Readeck's /bookmarks/sync is two calls: a GET listing ids and a change
     * type, then a POST asking for the bookmark bodies. Only the update entries
     * should be fetched -- asking Readeck to resolve an id it has since deleted
     * returns nothing, and booking it as changed would resurrect it locally.
     */
    @Test
    fun `readeck changed bookmarks fetches bodies for updates and reports deletes`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"id":"keep","type":"update"},{"id":"gone","type":"delete"}]""",
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                """[{"id":"keep","url":"https://example.com","title":"Kept",
                    "is_marked":false,"is_archived":false,"reading_time":3,
                    "created":"2024-01-01T10:00:00+00:00",
                    "updated":"2024-01-03T10:00:00+00:00"}]""",
            ),
        )

        val changed = readeck().changedBookmarks(1_700_000_000_000)

        assertEquals(1, changed.size)
        assertEquals("keep", changed[0].id)

        val list = server.takeRequest()
        assertEquals("GET", list.method)
        assertTrue(list.path!!, list.path!!.startsWith("/api/bookmarks/sync"))
        // The cursor is the whole point of the call, so it is pinned rather than
        // left to the default of "everything, ever".
        assertTrue(list.path!!, list.path!!.contains("since="))

        val bodies = server.takeRequest()
        assertEquals("POST", bodies.method)
        val body = bodies.body.readUtf8()
        assertTrue(body, body.contains("\"keep\""))
    }

    /**
     * A sync window containing only deletions must not trigger a second request.
     * The repository asks for changed bodies and for deleted ids separately, so
     * this is the shape of every delete-only sync, and an empty POST would be
     * rejected by the server.
     */
    @Test
    fun `readeck changed bookmarks makes no second call when everything was deleted`() = runTest {
        server.enqueue(
            MockResponse().setBody("""[{"id":"gone","type":"delete"}]"""),
        )

        val changed = readeck().changedBookmarks(1_700_000_000_000)

        assertTrue(changed.isEmpty())
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `readeck reports only the deleted ids`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """[{"id":"a","type":"delete"},{"id":"b","type":"update"},{"id":"c","type":"delete"}]""",
            ),
        )

        val deleted = readeck().deletedBookmarkIds(1_700_000_000_000)

        assertEquals(listOf("a", "c"), deleted)
        assertEquals(1, server.requestCount)
    }

    /**
     * The cursor round-trips. Readeck returns timestamps in its own format, and
     * the repository stores one and hands it back on the next sync, so a value
     * that comes back unparseable would silently reset the window.
     */
    @Test
    fun `readeck sends the cursor as an iso timestamp`() = runTest {
        server.enqueue(MockResponse().setBody("[]"))

        readeck().changedBookmarks(1_700_000_000_000)

        val request = server.takeRequest()
        val since = request.requestUrl!!.queryParameter("since")!!
        assertTrue(since, since.matches(Regex("""\d{4}-\d{2}-\d{2}T[\d:]+([+-]\d{2}:\d{2}|Z)""")))
    }

    // ---- Wallabag ---------------------------------------------------------

    /**
     * The deletion half of the contract: null, not empty. An empty list would
     * read as "nothing was deleted" and the repository would skip the full
     * refresh, so deleted articles would stay in the list forever.
     */
    @Test
    fun `wallabag cannot report deletions and says so`() = runTest {
        assertNull(wallabag().deletedBookmarkIds(0L))
        assertEquals(0, server.requestCount)
    }

    /**
     * updatedSince is a strict lower bound and the paging has to keep going while
     * a page comes back full, or a busy account silently loses the tail of its
     * changes.
     */
    @Test
    fun `wallabag changed bookmarks pages until a short page and sorts ascending`() = runTest {
        val page = { from: Int, to: Int, total: Int ->
            val items = (from..to).joinToString(",") { i ->
                """{"id":$i,"url":"https://example.com/$i","title":"Item $i",
                    "is_archived":0,"is_starred":0,
                    "created_at":"2024-01-01T10:00:00+00:00",
                    "updated_at":"2024-01-02T10:00:00+00:00"}"""
            }
            """{"total":$total,"_embedded":{"items":[$items]}}"""
        }
        server.enqueue(MockResponse().setBody(page(1, 30, 31)))
        server.enqueue(MockResponse().setBody(page(31, 31, 31)))

        val changed = wallabag().changedBookmarks(1_700_000_000_000)

        assertEquals(31, changed.size)
        assertEquals("1", changed.first().id)
        assertEquals("31", changed.last().id)

        val first = server.takeRequest()
        val path = first.path!!
        assertTrue(path, path.contains("updatedSince="))
        // Ascending by update time, so a sync that stops half way resumes
        // correctly instead of skipping whatever it had not reached yet.
        assertTrue(path, path.contains("sort=updated"))
        assertTrue(path, path.contains("order=asc"))
        assertTrue(path, path.contains("page=1"))

        val second = server.takeRequest()
        assertTrue(second.path!!, second.path!!.contains("page=2"))
    }

    /**
     * Archive is expressed as is_archived, so an archived entry must arrive
     * marked read rather than unread, or it reappears in the Unread filter.
     */
    @Test
    fun `wallabag maps archived entries to read and starred to favorite`() = runTest {
        server.enqueue(
            MockResponse().setBody(
                """{"total":1,"_embedded":{"items":[{"id":7,"url":"https://example.com",
                    "title":"Archived","is_archived":1,"is_starred":1,"reading_time":4,
                    "created_at":"2024-01-01T10:00:00+00:00",
                    "updated_at":"2024-01-02T10:00:00+00:00"}]}}""",
            ),
        )

        val changed = wallabag().changedBookmarks(0L)

        val bookmark = changed.single()
        assertTrue(bookmark.id == "7")
        assertTrue("archived should read", !bookmark.unread)
        assertTrue("starred should favorite", bookmark.favorite)
        assertEquals(4, bookmark.readingTimeMinutes)
    }
}