package com.neverreader.app.reader

import com.neverreader.backend.model.Bookmark
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

/**
 * The reader's app bar title.
 *
 * It used to be the host, which sat immediately right of the back arrow and so
 * read as the back button's label, while the article itself had no title.
 */
// Robolectric because the host fallback goes through android.net.Uri, which is
// a framework class and returns nothing on a bare JVM.
@RunWith(RobolectricTestRunner::class)
class ReaderTitleTest {

    private fun bookmark(title: String) = Bookmark(
        id = "1",
        url = "https://laut.de/article",
        title = title,
        excerpt = "",
        imageUrl = null,
        unread = true,
        favorite = false,
        readingTimeMinutes = 4,
        createdAt = 0L,
        updatedAt = 0L,
        tags = emptyList(),
    )

    private val url = "https://laut.de/article"

    @Test
    fun `the article title is shown`() {
        assertEquals("Groove, Härte und Chaos.", readerTitle(bookmark("Groove, Härte und Chaos."), url))
    }

    @Test
    fun `the host is only a fallback while the bookmark is still being read`() {
        assertEquals("laut.de", readerTitle(null, url))
    }

    @Test
    fun `a blank title falls back to the host rather than an empty bar`() {
        assertEquals("laut.de", readerTitle(bookmark(""), url))
        assertEquals("laut.de", readerTitle(bookmark("   "), url))
    }
}
