package com.neverreader.util.java

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * URL extraction, which sits between the user pasting something and the app
 * saving it.
 *
 * Two callers care about it: the add-URL sheet, which offers the first URL it
 * finds in whatever is in the field, and the clipboard action, which saves
 * whatever the clipboard holds if it looks like a link. Both depend on it not
 * inventing a URL out of text that has none, which is why the "no URLs" and
 * "prose that merely contains the word http" cases are here.
 *
 * Under Robolectric because Patterns.WEB_URL and URLUtil are framework calls.
 */
@RunWith(RobolectricTestRunner::class)
class UrlFinderTest {

    @Test
    fun `finds a bare url`() {
        val urls = UrlFinder.getUrlsFromText("https://example.com/article")

        assertEquals(1, urls?.size)
        assertTrue(urls!!.first()!!.startsWith("https://example.com"))
    }

    @Test
    fun `finds a url embedded in a sentence`() {
        val urls = UrlFinder.getUrlsFromText(
            "I thought this was interesting: https://example.com/x and worth saving.",
        )

        assertEquals(1, urls?.size)
        assertTrue(urls!!.first()!!.contains("example.com/x"))
    }

    @Test
    fun `finds several urls`() {
        val urls = UrlFinder.getUrlsFromText(
            "https://one.example.com and https://two.example.com and https://three.example.com",
        )

        assertEquals(3, urls?.size)
    }

    /** Null in, null out - the caller uses that to mean "nothing to look at". */
    @Test
    fun `null text returns null`() {
        assertNull(UrlFinder.getUrlsFromText(null))
        assertNull(UrlFinder.getUrlsFromText(null, 5))
    }

    /**
     * Empty rather than null: the text was there, there was just no link in it.
     * The two are different to the caller and must not be conflated.
     */
    @Test
    fun `text with no url returns an empty list, not null`() {
        val urls = UrlFinder.getUrlsFromText("just some words with no link in them")

        assertTrue(urls != null, "no match should still be a list")
        assertTrue(urls!!.isEmpty())
    }

    @Test
    fun `empty text returns an empty list`() {
        assertTrue(UrlFinder.getUrlsFromText("")!!.isEmpty())
    }

    /**
     * The limit is what keeps the add-URL sheet from offering a guess when a
     * paragraph contains three links: the first is the intended one.
     */
    @Test
    fun `the limit caps how many urls are returned`() {
        val text = "https://one.example.com https://two.example.com https://three.example.com"

        assertEquals(2, UrlFinder.getUrlsFromText(text, 2)?.size)
        assertEquals(1, UrlFinder.getUrlsFromText(text, 1)?.size)
    }

    @Test
    fun `a limit of zero means no limit`() {
        val urls = UrlFinder.getUrlsFromText("https://a.example.com https://b.example.com", 0)

        assertEquals(2, urls?.size)
    }

    // ---- getFirstUrlOrNull -------------------------------------------------

    @Test
    fun `first url or null returns the first one`() {
        val first = UrlFinder.getFirstUrlOrNull(
            "https://first.example.com then https://second.example.com",
        )

        assertTrue(first!!.contains("first.example.com"))
    }

    @Test
    fun `first url or null returns null when there is none`() {
        assertNull(UrlFinder.getFirstUrlOrNull("no links here"))
        assertNull(UrlFinder.getFirstUrlOrNull(null))
        assertNull(UrlFinder.getFirstUrlOrNull(""))
    }
}