package com.neverreader.app.reader

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * The document the reader hands to the WebView.
 *
 * The margin matters because the stylesheet zeroes every margin and padding, so
 * the article ran edge to edge until --article-margin was introduced.
 */
@RunWith(RobolectricTestRunner::class)
class ArticleHtmlTest {

    // RuntimeEnvironment rather than ApplicationProvider: androidx.test:core is
    // not on the test classpath and adding it just for this is not worth it.
    private val context: android.content.Context = RuntimeEnvironment.getApplication()

    private fun html(dark: Boolean = false) = buildArticleHtml(context, "<p>Body</p>", dark)

    /** Comments stripped: they explain the history of these rules and quote them. */
    private fun stylesheet() =
        context.assets.open(READER_STYLESHEET).bufferedReader().use { it.readText() }
            .replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")

    @Test
    fun `the margin comes from the resource, not a literal`() {
        val expected = context.resources.getInteger(com.neverreader.app.R.integer.article_default_margin)

        assertTrue(html(), "--article-margin: ${expected}px" in html())
    }

    @Test
    fun `the stylesheet is inlined so there is no second request`() {
        assertTrue(html(), "@font-face" in html())
    }

    @Test
    fun `the article itself is inlined`() {
        assertTrue("<p>Body</p>" in html())
    }

    @Test
    fun `the stylesheet consumes the variable, with a fallback`() {
        val css = stylesheet()

        assertTrue(css, "var(--article-margin" in css)
    }

    @Test
    fun `the body is not left hidden waiting for javascript`() {
        // Scoped to the body rule on purpose: .RIL_IMG:after legitimately uses
        // visibility:hidden as a clearfix.
        val hiddenBody = Regex("""body\s*\{[^}]*visibility:\s*hidden""")
        assertTrue(stylesheet(), !hiddenBody.containsMatchIn(stylesheet()))
    }

    @Test
    fun `article images are not hidden waiting for a javascript class`() {
        // .RIL_IMG used to be display:none until script added .loaded.
        assertTrue(stylesheet(), Regex("""\.RIL_IMG\s*\{[^}]*display:\s*none""").containsMatchIn(stylesheet()).not())
    }

    @Test
    fun `dark mode still selects the dark palette`() {
        assertTrue("""textStyle="1" """ in html(dark = true))
        assertTrue("""textStyle="0" """ in html(dark = false))
    }
}
