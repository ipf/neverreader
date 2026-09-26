package com.neverreader.app.list

import com.neverreader.backend.model.Bookmark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The rule that keeps the list from contacting third parties: an image loads only
 * if it is served by the user's own server, or the user opted in.
 */
class ImagePolicyTest {

    private fun bookmark(imageUrl: String?) = Bookmark(
        id = "1",
        url = "https://example.com/article",
        title = "Article",
        excerpt = "",
        imageUrl = imageUrl,
        unread = true,
        favorite = false,
        readingTimeMinutes = 3,
        createdAt = 0,
        updatedAt = 0,
        tags = emptyList(),
    )

    @Test
    fun `reads the host out of a url`() {
        assertEquals("readeck.example.com", hostOf("https://readeck.example.com/api/x.jpg"))
        assertEquals("example.com", hostOf("https://www.example.com/a"))
        assertNull(hostOf("not a url"))
    }

    @Test
    fun `a same-origin thumbnail host is recognised`() {
        assertEquals(
            "readeck.example.com",
            hostOf("https://readeck.example.com/api/bookmarks/1/thumbnail"),
        )
    }
}
