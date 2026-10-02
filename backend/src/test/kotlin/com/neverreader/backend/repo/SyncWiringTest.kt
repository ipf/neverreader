package com.neverreader.backend.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.neverreader.backend.Backend
import com.neverreader.backend.db.AnnotationEntity
import com.neverreader.backend.db.BookmarkEntity
import com.neverreader.backend.db.BookmarkTagEntity
import com.neverreader.backend.db.NeverReaderDatabase
import com.neverreader.backend.model.Account
import com.neverreader.backend.model.Annotation
import com.neverreader.backend.model.BackendType
import com.neverreader.backend.model.BackendCapabilities
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.ListFilter
import com.neverreader.backend.model.Tag
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * sync() end to end, against a real database and a fake server.
 *
 * The purge and remove helpers have their own tests, but nothing covered the
 * part that actually decides which one runs: a full refresh happens only when
 * there has never been one or the last is older than a week, and a delta sync
 * otherwise. Both branches advance the cursor, and a backend that cannot report
 * deletions has to be sent down the full-refresh path rather than silently
 * keeping rows the server dropped.
 *
 * Reaching this needed Accounts and a backend factory as parameters, since
 * AccountManager decrypts tokens through Tink and the Android keystore.
 */
@RunWith(RobolectricTestRunner::class)
class SyncWiringTest {

    private lateinit var db: NeverReaderDatabase
    private lateinit var context: Context
    private val accounts = FakeAccounts()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NeverReaderDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- fakes -------------------------------------------------------------

    private class FakeAccounts(
        var state: SyncState = SyncState(0L, 0L),
    ) : Accounts {
        var updates = 0
        override suspend fun active(): Account = Account(
            backendType = BackendType.WALLABAG,
            serverUrl = "https://example.com",
            username = "u",
            accessToken = "t",
            refreshToken = null,
            clientId = null,
            clientSecret = null,
        )

        override suspend fun syncState(): SyncState = state
        override suspend fun update(account: Account) = Unit
        override suspend fun updateSyncState(lastSyncAt: Long?, lastFullSyncAt: Long?) {
            updates++
            state = SyncState(
                lastSyncAt = lastSyncAt ?: state.lastSyncAt,
                lastFullSyncAt = lastFullSyncAt ?: state.lastFullSyncAt,
            )
        }
    }

    private class FakeBackend(
        val pages: List<List<Bookmark>> = emptyList(),
        val changed: List<Bookmark> = emptyList(),
        val deleted: List<String>? = emptyList(),
    ) : Backend {
        override val type = BackendType.WALLABAG
        override val capabilities = BackendCapabilities(
            highlights = true, serverSearch = true, deltaSync = true,
        )
        var fullRefreshCalls = 0
        var deltaCalls = 0
        val archivedCalls = mutableListOf<Pair<String, Boolean>>()

        override suspend fun listBookmarks(filter: ListFilter, limit: Int, offset: Int): List<Bookmark> {
            fullRefreshCalls++
            return pages.getOrElse(offset / limit) { emptyList() }
        }

        override suspend fun changedBookmarks(since: Long): List<Bookmark> {
            deltaCalls++
            return changed
        }

        override suspend fun deletedBookmarkIds(since: Long): List<String>? = deleted

        override suspend fun fetchArticleHtml(id: String) = ""
        override suspend fun addBookmark(url: String, title: String?) = error("unused")
        override suspend fun setArchived(id: String, archived: Boolean) {
            archivedCalls += id to archived
        }
        override suspend fun setFavorite(id: String, favorite: Boolean) = Unit
        override suspend fun deleteBookmark(id: String) = Unit
        override suspend fun listTags(): List<Tag> = emptyList()
        override suspend fun addTags(bookmarkId: String, names: List<String>) = Unit
        override suspend fun removeTag(bookmarkId: String, name: String) = Unit
        override suspend fun listAnnotations(bookmarkId: String): List<Annotation> = emptyList()
        override suspend fun addAnnotation(bookmarkId: String, annotation: Annotation) = error("unused")
        override suspend fun deleteAnnotation(bookmarkId: String, annotationId: String) = Unit
    }

    // ---- fixtures ----------------------------------------------------------

    private fun bookmark(id: String, title: String = id, tags: List<String> = emptyList()) = Bookmark(
        id = id, url = "https://example.com/$id", title = title, excerpt = "",
        imageUrl = "", unread = true, favorite = false, readingTimeMinutes = 0,
        createdAt = 0L, updatedAt = 0L, tags = tags,
    )

    private suspend fun store(bookmark: Bookmark) {
        db.bookmarkDao().upsertAll(
            listOf(
                BookmarkEntity(
                    id = bookmark.id, url = bookmark.url, title = bookmark.title,
                    excerpt = bookmark.excerpt, imageUrl = bookmark.imageUrl,
                    unread = bookmark.unread, favorite = bookmark.favorite,
                    readingTimeMinutes = bookmark.readingTimeMinutes,
                    createdAt = bookmark.createdAt, updatedAt = bookmark.updatedAt,
                    tagsJson = "[]",
                ),
            ),
        )
        if (bookmark.tags.isNotEmpty()) {
            db.bookmarkDao().insertTagLinks(bookmark.tags.map { BookmarkTagEntity(bookmark.id, it) })
        }
    }

    private suspend fun annotate(bookmarkId: String, id: String) {
        db.annotationDao().upsertAll(
            listOf(
                AnnotationEntity(
                    id = id, bookmarkId = bookmarkId, text = "t", comment = null,
                    startSelector = "p#1", startOffset = 0, endSelector = "p#1", endOffset = 1,
                    createdAt = 0L,
                ),
            ),
        )
    }

    /**
     * A sync state that is inside the refresh window, so the delta path runs.
     * Any small constant here would read as 1970 and be seven years overdue.
     */
    private fun recentState() = SyncState(
        lastSyncAt = System.currentTimeMillis() - 1_000L,
        lastFullSyncAt = System.currentTimeMillis() - 1_000L,
    )

    private fun repository(backend: Backend) = BookmarkRepository(
        context = context,
        db = db,
        accounts = accounts,
        backendFactory = { _, _ -> backend },
    )

    // ---- the branches ------------------------------------------------------

    /**
     * The bug this whole class exists to guard: a first sync walks the full
     * refresh, and anything stored that the server did not return is removed.
     */
    @Test
    fun `a first sync full refreshes and drops what the server no longer has`() = runTest {
        store(bookmark("kept"))
        store(bookmark("gone"))
        annotate("gone", "ann-1")
        val backend = FakeBackend(pages = listOf(listOf(bookmark("kept"))))

        assertTrue(repository(backend).sync())

        assertEquals(1, backend.fullRefreshCalls)
        assertEquals(0, backend.deltaCalls)
        assertEquals(listOf("kept"), db.bookmarkDao().allIds())
        assertTrue(db.annotationDao().byBookmark("gone").isEmpty(), "orphaned annotation left behind")
    }

    /**
     * The window between refreshes asks the server what changed instead. This is
     * the common case, and it must not re-list the whole library.
     */
    @Test
    fun `a recent sync uses the delta path`() = runTest {
        accounts.state = recentState()
        store(bookmark("kept"))
        val backend = FakeBackend(changed = listOf(bookmark("kept", title = "Renamed")))

        repository(backend).sync()

        assertEquals(0, backend.fullRefreshCalls)
        assertEquals(1, backend.deltaCalls)
        assertEquals("Renamed", db.bookmarkDao().get("kept")?.title)
    }

    /**
     * A backend that cannot report deletions must be sent down the full refresh
     * path, or rows the server dropped would never be noticed. Here that means
     * asking for the list rather than asking for a delta and being told nothing.
     */
    @Test
    fun `a backend that cannot report deletions still loses the deleted rows`() = runTest {
        store(bookmark("kept"))
        store(bookmark("gone"))
        // lastFullSyncAt is stale, so the full refresh runs; deleted is null so
        // the delta path would have had nothing to act on.
        accounts.state = SyncState(lastSyncAt = 1_000L, lastFullSyncAt = 1L)
        val backend = FakeBackend(
            pages = listOf(listOf(bookmark("kept"))),
            deleted = null,
        )

        repository(backend).sync()

        assertEquals(1, backend.fullRefreshCalls)
        assertEquals(listOf("kept"), db.bookmarkDao().allIds())
    }

    /**
     * When the delta path does get a deletion list, it is honoured - and its
     * annotations go with it, which they did not before.
     */
    @Test
    fun `the delta path removes reported deletions and their annotations`() = runTest {
        accounts.state = recentState()
        store(bookmark("kept"))
        store(bookmark("gone"))
        annotate("gone", "ann-1")
        val backend = FakeBackend(deleted = listOf("gone"))

        repository(backend).sync()

        assertEquals(1, backend.deltaCalls)
        assertEquals(listOf("kept"), db.bookmarkDao().allIds())
        assertTrue(db.annotationDao().byBookmark("gone").isEmpty())
    }

    /**
     * The cursor has to move, or every sync redoes the same window forever.
     */
    @Test
    fun `both branches advance the cursor`() = runTest {
        accounts.state = recentState()
        repository(FakeBackend()).sync()
        val afterDelta = accounts.state

        accounts.state = SyncState(lastSyncAt = 1L, lastFullSyncAt = 0L)
        repository(FakeBackend(pages = listOf(emptyList()))).sync()
        val afterFull = accounts.state

        val before = System.currentTimeMillis() - 60_000L
        assertTrue(afterDelta.lastSyncAt > before, "delta sync did not advance lastSyncAt")
        assertTrue(afterFull.lastSyncAt > before, "full refresh did not advance lastSyncAt")
        assertTrue(afterFull.lastFullSyncAt > 0L, "full refresh did not record itself")
    }

    /**
     * A refresh more than a week old triggers the full path even though a delta
     * has run recently - that is the only thing that ever finds a Wallabag
     * deletion, so the interval has to actually be honoured.
     */
    @Test
    fun `a full refresh older than a week forces the full path`() = runTest {
        val eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        accounts.state = SyncState(lastSyncAt = 1_000L, lastFullSyncAt = eightDaysAgo)
        val backend = FakeBackend(pages = listOf(emptyList()))

        repository(backend).sync()

        assertEquals(1, backend.fullRefreshCalls, "expected a full refresh after eight days")
        assertEquals(0, backend.deltaCalls)
    }

    @Test
    fun `paging walks every page before purging`() = runTest {
        accounts.state = SyncState(lastSyncAt = 0L, lastFullSyncAt = 0L)
        // First page is full (30), so the repository has to ask for the next.
        val full = List(30) { bookmark("id-$it") }
        store(bookmark("stale"))
        val backend = FakeBackend(pages = listOf(full, listOf(bookmark("last"))))

        repository(backend).sync()

        assertEquals(2, backend.fullRefreshCalls, "expected a second page")
        assertTrue(db.bookmarkDao().allIds().contains("last"), "the short page was not stored")
        assertFalse(db.bookmarkDao().allIds().contains("stale"), "stale row survived the refresh")
    }
}