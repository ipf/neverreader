package com.neverreader.backend.repo

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.test.core.app.ApplicationProvider
import com.neverreader.backend.db.BookmarkEntity
import com.neverreader.backend.db.NeverReaderDatabase
import com.neverreader.backend.model.BookmarkSort
import com.neverreader.backend.model.ListFilter
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * BookmarkQuery, actually executed.
 *
 * The existing test checks the SQL that comes out of sql() and the order of
 * args(). Between them there are things neither can see: whether the
 * placeholders line up with the bound values once SQLite is involved, and
 * whether the LIKE escaping does what it claims against real rows.
 *
 * Run through RoomDatabase.query rather than the paged() path, so what comes
 * back is plain rows to assert on.
 */
@RunWith(RobolectricTestRunner::class)
class BookmarkQueryDatabaseTest {

    private lateinit var db: NeverReaderDatabase
    private lateinit var context: Context

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

    private fun row(
        id: String,
        title: String,
        unread: Boolean = true,
        favorite: Boolean = false,
        tags: List<String> = emptyList(),
        createdAt: Long = 0L,
    ) = BookmarkEntity(
        id = id,
        url = "https://example.com/$id",
        title = title,
        excerpt = "excerpt of $title",
        imageUrl = "",
        unread = unread,
        favorite = favorite,
        readingTimeMinutes = 0,
        createdAt = createdAt,
        updatedAt = createdAt,
        tagsJson = tags.joinToString(",", "[", "]") { "\"$it\"" },
    )

    private suspend fun seed(vararg rows: BookmarkEntity) {
        db.bookmarkDao().upsertAll(rows.toList())
    }

    private fun ids(filter: ListFilter): List<String> =
        db.query(BookmarkQuery.build(filter)).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.getString(cursor.getColumnIndexOrThrow("id"))) }
        }

    @Test
    fun `no filter returns everything`() = runTest {
        seed(row("a", "A"), row("b", "B"), row("c", "C"))

        assertEquals(listOf("a", "b", "c"), ids(ListFilter()).sorted())
    }

    @Test
    fun `unread filter`() = runTest {
        seed(row("a", "A", unread = true), row("b", "B", unread = false))

        assertEquals(listOf("a"), ids(ListFilter(unread = true)))
        assertEquals(listOf("b"), ids(ListFilter(unread = false)))
    }

    @Test
    fun `favorite filter`() = runTest {
        seed(row("a", "A", favorite = true), row("b", "B", favorite = false))

        assertEquals(listOf("a"), ids(ListFilter(favorite = true)))
    }

    @Test
    fun `filters combine`() = runTest {
        seed(
            row("both", "Both", unread = true, favorite = true),
            row("unreadOnly", "Unread", unread = true, favorite = false),
            row("favOnly", "Fav", unread = false, favorite = true),
        )

        assertEquals(
            listOf("both"),
            ids(ListFilter(unread = true, favorite = true)),
        )
    }

    @Test
    fun `search matches the title`() = runTest {
        seed(row("a", "Interesting thing"), row("b", "Nothing in particular"))

        assertEquals(listOf("a"), ids(ListFilter(search = "Interesting")))
    }

    @Test
    fun `search matches the excerpt and the url`() = runTest {
        seed(
            BookmarkEntity(
                id = "byExcerpt", url = "https://example.com/e", title = "Plain",
                excerpt = "a memorable phrase", imageUrl = "", unread = true, favorite = false,
                readingTimeMinutes = 0, createdAt = 0L, updatedAt = 0L, tagsJson = "[]",
            ),
            BookmarkEntity(
                id = "byUrl", url = "https://memorable.example.com", title = "Plain",
                excerpt = "nothing", imageUrl = "", unread = true, favorite = false,
                readingTimeMinutes = 0, createdAt = 0L, updatedAt = 0L, tagsJson = "[]",
            ),
        )

        assertEquals(listOf("byExcerpt", "byUrl"), ids(ListFilter(search = "memorable")).sorted())
    }

    @Test
    fun `tag filter finds a tag`() = runTest {
        seed(
            row("a", "A", tags = listOf("news")),
            row("b", "B", tags = listOf("sport")),
        )

        assertEquals(listOf("a"), ids(ListFilter(tag = "news")))
    }

    /**
     * The one that made this test worth writing. tagsJson is a JSON array, so a
     * plain %news% also matches any tag that merely contains "news" - picking
     * the "ew" chip would have shown everything tagged "news".
     */
    @Test
    fun `a tag filter does not match a longer tag that contains it`() = runTest {
        seed(
            row("exact", "Exact", tags = listOf("news")),
            row("longer", "Longer", tags = listOf("newsletter")),
            row("substring", "Substring", tags = listOf("my-news")),
        )

        assertEquals(listOf("exact"), ids(ListFilter(tag = "news")))
    }

    @Test
    fun `a tag filter matches every row carrying that tag`() = runTest {
        seed(
            row("a", "A", tags = listOf("news", "sport")),
            row("b", "B", tags = listOf("sport")),
            row("c", "C", tags = listOf("news")),
        )

        assertEquals(listOf("a", "b"), ids(ListFilter(tag = "sport")).sorted())
    }

    /** An empty tag list has to match nothing rather than everything. */
    @Test
    fun `a tag filter does not match untagged rows`() = runTest {
        seed(row("tagged", "A", tags = listOf("news")), row("untagged", "B", tags = emptyList()))

        assertEquals(listOf("tagged"), ids(ListFilter(tag = "news")))
    }

    @Test
    fun `search is case insensitive`() = runTest {
        seed(row("a", "Berlin Trip"), row("b", "Other"))

        assertEquals(listOf("a"), ids(ListFilter(search = "berlin")))
    }

    /**
     * A % or _ typed into search is literal text, not a wildcard. % would
     * otherwise match every row and _ any single character.
     */
    @Test
    fun `wildcards in a search term are literal`() = runTest {
        seed(
            row("percent", "100% cotton"),
            row("underscore", "a_b"),
            row("other", "nothing special"),
        )

        assertEquals(listOf("percent"), ids(ListFilter(search = "100%")))
        assertEquals(listOf("underscore"), ids(ListFilter(search = "a_b")))
    }

    @Test
    fun `sorting by newest and oldest`() = runTest {
        seed(
            row("old", "Old", createdAt = 100L),
            row("new", "New", createdAt = 300L),
            row("mid", "Mid", createdAt = 200L),
        )

        assertEquals(
            listOf("new", "mid", "old"),
            ids(ListFilter(sort = BookmarkSort.NEWEST)),
        )
        assertEquals(
            listOf("old", "mid", "new"),
            ids(ListFilter(sort = BookmarkSort.OLDEST)),
        )
    }

    @Test
    fun `title sorting ignores case`() = runTest {
        seed(
            row("b", "banana"),
            row("A", "Apple"),
            row("c", "cherry"),
        )

        assertEquals(
            listOf("A", "b", "c"),
            ids(ListFilter(sort = BookmarkSort.TITLE_ASC)),
        )
    }

    /**
     * The tie-break the ORDER BY exists for: without a total order, paging over
     * rows sharing a timestamp can repeat or skip them.
     */
    @Test
    fun `rows sharing a timestamp still come back in a stable order`() = runTest {
        seed(
            row("z", "Same", createdAt = 500L),
            row("m", "Same", createdAt = 500L),
            row("a", "Same", createdAt = 500L),
        )

        assertEquals(listOf("a", "m", "z"), ids(ListFilter()))
    }

    @Test
    fun `a filter matching nothing returns nothing`() = runTest {
        seed(row("a", "A"))

        assertEquals(emptyList(), ids(ListFilter(search = "nothing matches this")))
    }
}