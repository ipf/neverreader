package com.neverreader.backend.repo

import com.neverreader.backend.model.BookmarkSort
import com.neverreader.backend.model.ListFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The paged list query, which is what search and sort both ride on.
 */
class BookmarkQueryTest {

    private fun sql(filter: ListFilter) = BookmarkQuery.build(filter).sql

    private fun args(filter: ListFilter) = BookmarkQuery.args(filter)

    @Test
    fun `default order is newest saved, unchanged from before sorting existed`() {
        val query = sql(ListFilter())

        assertTrue(query, query.endsWith("ORDER BY createdAt DESC, id ASC"))
    }

    @Test
    fun `every sort maps to its own fragment`() {
        assertEquals("createdAt DESC", BookmarkQuery.orderBy(BookmarkSort.NEWEST))
        assertEquals("createdAt ASC", BookmarkQuery.orderBy(BookmarkSort.OLDEST))
        assertEquals("title COLLATE NOCASE ASC", BookmarkQuery.orderBy(BookmarkSort.TITLE_ASC))
        assertEquals("title COLLATE NOCASE DESC", BookmarkQuery.orderBy(BookmarkSort.TITLE_DESC))
    }

    @Test
    fun `the chosen sort reaches the query`() {
        for (sort in BookmarkSort.entries) {
            assertTrue(
                sort.name,
                sql(ListFilter(sort = sort)).contains(BookmarkQuery.orderBy(sort)),
            )
        }
    }

    /**
     * The tie-break on id is what stops paging repeating or skipping rows when two
     * bookmarks share a timestamp, which is the normal case after a bulk sync.
     */
    @Test
    fun `every order ends with a total tie-break`() {
        for (sort in BookmarkSort.entries) {
            assertTrue(sort.name, sql(ListFilter(sort = sort)).endsWith(", id ASC"))
        }
    }

    @Test
    fun `search covers title, excerpt and url with one bound pattern`() {
        val filter = ListFilter(search = "kotlin")
        val query = sql(filter)

        assertTrue(query, query.contains("title LIKE ?"))
        assertTrue(query, query.contains("excerpt LIKE ?"))
        assertTrue(query, query.contains("url LIKE ?"))
        assertEquals(listOf("%kotlin%", "%kotlin%", "%kotlin%"), args(filter))
    }

    @Test
    fun `search is never interpolated into the sql`() {
        val query = sql(ListFilter(search = "'; DROP TABLE bookmarks; --"))

        assertFalse(query, query.contains("DROP TABLE"))
        assertEquals("%'; DROP TABLE bookmarks; --%", args(ListFilter(search = "'; DROP TABLE bookmarks; --")).first())
    }

    /**
     * The bug this fixes: search built its pattern as "%$it%", so a % matched every
     * row and _ matched any character. Tags already escaped via likeFor.
     */
    @Test
    fun `percent and underscore in a search term are literal`() {
        assertEquals("%100\\%%", BookmarkQuery.likePattern("100%"))
        assertEquals("%a\\_b%", BookmarkQuery.likePattern("a_b"))
        assertEquals("%c:\\\\path%", BookmarkQuery.likePattern("c:\\path"))
    }

    @Test
    fun `tags and search escape the same way`() {
        // Same escaping, different wrapping: a tag is matched as a whole element
        // of the tagsJson array, so its quotes are part of the pattern.
        assertEquals(
            "%\"a\\%b\"%",
            args(ListFilter(tag = "a%b")).single(),
        )
        // The search pattern is the plain wrapped form.
        assertEquals(
            BookmarkQuery.likePattern("a%b"),
            args(ListFilter(search = "a%b")).first(),
        )
    }

    @Test
    fun `filters compose rather than replace each other`() {
        val filter = ListFilter(unread = true, favorite = false, tag = "news", search = "berlin", sort = BookmarkSort.TITLE_ASC)
        val query = sql(filter)

        assertTrue(query, query.contains("unread = ?"))
        assertTrue(query, query.contains("favorite = ?"))
        assertTrue(query, query.contains("tagsJson LIKE ?"))
        assertTrue(query, query.contains("title LIKE ?"))
        assertTrue(query, query.endsWith("ORDER BY title COLLATE NOCASE ASC, id ASC"))
    }
}
