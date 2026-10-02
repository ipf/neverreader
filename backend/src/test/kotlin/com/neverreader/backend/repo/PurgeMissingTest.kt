package com.neverreader.backend.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.neverreader.backend.db.AnnotationEntity
import com.neverreader.backend.db.BookmarkEntity
import com.neverreader.backend.db.BookmarkTagEntity
import com.neverreader.backend.db.NeverReaderDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The reconcile step of a full refresh: delete what the server no longer has.
 *
 * This is the only way a deletion made on the server ever reaches the device,
 * because Wallabag's updatedSince cannot report one - it returns null and that
 * is what sends the repository here. It was broken for the whole time the
 * comments claimed it worked: the refresh upserted every page and never removed
 * anything, so an article deleted on the server stayed in the list forever.
 *
 * Against a real in-memory database rather than a mock, because the part that
 * goes wrong is not the comparison but forgetting to clean up what points at the
 * row: tag links and annotations.
 */
@RunWith(RobolectricTestRunner::class)
class PurgeMissingTest {

    private lateinit var db: NeverReaderDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NeverReaderDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun store(id: String, title: String = id) {
        db.bookmarkDao().upsertAll(
            listOf(
                BookmarkEntity(
                    id = id, url = "https://example.com/$id", title = title, excerpt = "",
                    imageUrl = "", unread = true, favorite = false,
                    readingTimeMinutes = 0, createdAt = 0L, updatedAt = 0L, tagsJson = "[]",
                ),
            ),
        )
    }

    private suspend fun tag(bookmarkId: String, tag: String) {
        db.bookmarkDao().insertTagLinks(listOf(BookmarkTagEntity(bookmarkId, tag)))
    }

    private suspend fun annotate(bookmarkId: String, id: String) {
        db.annotationDao().upsertAll(
            listOf(
                AnnotationEntity(
                    id = id, bookmarkId = bookmarkId, text = "a highlight", comment = null,
                    startSelector = "p#1", startOffset = 0, endSelector = "p#1", endOffset = 4,
                    createdAt = 0L,
                ),
            ),
        )
    }

    @Test
    fun `a bookmark the server no longer has is removed`() = runTest {
        store("kept")
        store("gone")

        purgeMissing(db, setOf("kept"))

        assertEquals(listOf("kept"), db.bookmarkDao().allIds())
    }

    /**
     * The reason this compares ids rather than whole rows: an article that was
     * re-favorited, archived or re-tagged on the server is still the same
     * article and must survive.
     */
    @Test
    fun `a bookmark that changed on the server survives`() = runTest {
        store("changed", title = "New title")

        purgeMissing(db, setOf("changed"))

        assertEquals(listOf("changed"), db.bookmarkDao().allIds())
        assertEquals("New title", db.bookmarkDao().get("changed")?.title)
    }

    @Test
    fun `nothing is removed when the server returned everything`() = runTest {
        store("a")
        store("b")

        purgeMissing(db, setOf("a", "b"))

        assertEquals(listOf("a", "b"), db.bookmarkDao().allIds())
    }

    @Test
    fun `nothing is removed when the server returned nothing and nothing is stored`() = runTest {
        purgeMissing(db, emptySet())

        assertTrue(db.bookmarkDao().allIds().isEmpty())
    }

    /**
     * Tag links point at the bookmark, and a stale one shows the tag in the
     * filter list with a count that no longer adds up.
     */
    @Test
    fun `tag links for a removed bookmark are removed too`() = runTest {
        store("gone")
        tag("gone", "news")
        tag("kept", "news")

        purgeMissing(db, setOf("kept"))

        val tags = db.bookmarkDao().tagCounts().first()
        assertEquals(1, tags.single().count, "only the surviving bookmark should be counted")
    }

    /**
     * Annotations are keyed by their own id, not foreign-keyed to the bookmark,
     * so nothing removes them automatically. Left behind, a highlight on a
     * deleted article is orphaned and would resurface if that id ever came back.
     */
    @Test
    fun `annotations for a removed bookmark are removed too`() = runTest {
        store("gone")
        store("kept")
        annotate("gone", "ann-1")
        annotate("kept", "ann-2")

        purgeMissing(db, setOf("kept"))

        assertTrue(db.annotationDao().byBookmark("gone").isEmpty())
        assertEquals(1, db.annotationDao().byBookmarkFlow("kept").first().size)
    }

    @Test
    fun `several removals are handled in one pass`() = runTest {
        store("a"); store("b"); store("c"); store("d")

        purgeMissing(db, setOf("c", "d"))

        assertEquals(listOf("c", "d"), db.bookmarkDao().allIds().sorted())
    }

    /**
     * The same cleanup, driven by an explicit list of ids rather than by what a
     * refresh did not see. This is the delta-sync path, where the server does
     * report deletions - and where annotations used to be left behind.
     */
    @Test
    fun `removing reported deletions clears their annotations as well`() = runTest {
        store("gone")
        store("kept")
        annotate("gone", "ann-1")
        annotate("kept", "ann-2")
        tag("gone", "news")
        tag("kept", "news")

        removeAll(db, listOf("gone"))

        assertEquals(listOf("kept"), db.bookmarkDao().allIds())
        assertTrue(db.annotationDao().byBookmark("gone").isEmpty())
        assertEquals(1, db.bookmarkDao().tagCounts().first().single().count)
    }

    @Test
    fun `removing nothing is a no-op`() = runTest {
        store("kept")

        removeAll(db, emptyList())

        assertEquals(listOf("kept"), db.bookmarkDao().allIds())
    }

    @Test
    fun `removing everything leaves an empty list rather than failing`() = runTest {
        store("a")
        store("b")

        purgeMissing(db, emptySet())

        assertTrue(db.bookmarkDao().allIds().isEmpty())
    }
}