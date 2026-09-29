package com.neverreader.app.reader

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.neverreader.app.R
import com.neverreader.app.settings.Theme
import com.neverreader.backend.model.Bookmark
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import dagger.hilt.android.AndroidEntryPoint

/**
 * The article reader: renders the backend's article HTML in a WebView.
 *
 * The WebView stays because it is the platform's HTML renderer; the surrounding
 * chrome and theming are Compose. Pocket's own reader stylesheet
 * (`assets/html/c/text.css`) is injected and driven by the body attributes it
 * expects, so articles are centred and legible instead of raw page HTML.
 */
/**
 * The article reader: renders the backend's article HTML in a WebView.
 *
 * A navigation destination. The id and url arrive as parameters rather than as
 * navigation arguments read back out of a Bundle.
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    articleId: String?,
    articleUrl: String,
    darkTheme: Boolean,
    onBack: () -> Unit,
    onShare: (String, String) -> Unit,
) {
    LaunchedEffect(articleId, articleUrl) { viewModel.load(articleUrl, articleId) }
    val current by viewModel.state.collectAsStateWithLifecycle()
    val state = current
    val bookmark = (state as? ReaderViewModel.State.Content)?.bookmark
        ?: (state as? ReaderViewModel.State.Error)?.bookmark

    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar and the article drew over each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            navigationIcon = {
                AppIconButton(onClick = onBack) { UpIcon() }
            },
            title = {
                // The article's own title. This used to be the host, which sat
                // immediately right of the back arrow and so read as the back
                // button's label - and the article had no title at all.
                Text(
                    text = readerTitle(bookmark, state.url),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            actions = {
                val item = bookmark
                if (item == null) return@AppBar
                AppIconButton(onClick = { viewModel.toggleFavorite(item) }) {
                    Icon(
                        painter = painterResource(
                            if (item.favorite) {
                                com.neverreader.ui.R.drawable.ic_nr_favorite_solid
                            } else {
                                com.neverreader.ui.R.drawable.ic_nr_favorite_line
                            }
                        ),
                        contentDescription = stringResource(com.neverreader.ui.R.string.ic_favorite),
                        tint = if (item.favorite) AppTheme.colors.amber3 else AppTheme.colors.grey3,
                    )
                }
                AppIconButton(onClick = { onShare(item.url, item.title) }) {
                    Icon(
                        painter = painterResource(
                            com.neverreader.ui.R.drawable.ic_nr_android_share_solid
                        ),
                        contentDescription = stringResource(com.neverreader.ui.R.string.ic_share),
                        tint = AppTheme.colors.grey3,
                    )
                }
                AppIconButton(onClick = { viewModel.archive(item) }) {
                    Icon(
                        painter = painterResource(com.neverreader.ui.R.drawable.ic_nr_archive_line),
                        contentDescription = stringResource(com.neverreader.ui.R.string.ic_archive),
                        tint = AppTheme.colors.grey3,
                    )
                }
            },
        )

        when (state) {
            is ReaderViewModel.State.Loading -> Box(
                Modifier.fillMaxSize(),
                Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            is ReaderViewModel.State.Error -> Box(
                Modifier.fillMaxSize(),
                Alignment.Center,
            ) {
                Text(
                    // A self-hosted server can fail in a dozen ways; "failed to
                    // load" on its own was not actionable.
                    text = stringResource(R.string.reader_load_failed) + "\n\n" + state.reason,
                    style = AppTheme.typography.p3,
                    color = AppTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(AppTheme.dimensions.sideGrid),
                )
            }

            is ReaderViewModel.State.Content -> ArticleWebView(
                html = state.html,
                baseUrl = state.url,
                darkTheme = darkTheme,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/**
 * The article itself. JavaScript stays off: article HTML comes from a server the
 * user chose, and the reader stylesheet needs no scripting.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ArticleWebView(
    html: String,
    baseUrl: String,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.domStorageEnabled = false
                // targetSdk is never set, so it defaults to 26, where this
                // defaults to true: article HTML from the user's server could
                // otherwise read local files.
                settings.allowFileAccess = false
                // Follow the system font scale. This was pinned to 100, so reader
                // text ignored the user's font size setting entirely.
                settings.textZoom = (context.resources.configuration.fontScale * 100).toInt()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView,
                        request: WebResourceRequest,
                    ): Boolean {
                        // Keep in-article navigation in the WebView; anything else is
                        // dropped rather than launched. Note this only covers
                        // main-frame navigations - images, CSS and frames are
                        // subresources and still reach the article's own host, which
                        // is inherent to rendering HTML.
                        return request.url.host != Uri.parse(baseUrl).host
                    }
                }
            }
        },
        update = { webView ->
            // update runs on every recomposition. Reloading each time threw away
            // the scroll position on every theme or state change.
            if (webView.tag != html) {
                webView.tag = html
                webView.loadDataWithBaseURL(
                    baseUrl,
                    buildArticleHtml(webView.context, html, darkTheme),
                    "text/html",
                    "utf-8",
                    null,
                )
            }
        },
        modifier = modifier,
        onRelease = { it.stopLoading(); it.destroy() },
    )
}

/** Injects the reader stylesheet and the body attributes it keys off. */
/** Internal rather than private so the generated document can be asserted on. */
internal fun buildArticleHtml(context: Context, article: String, darkTheme: Boolean): String {
    val css = context.assets.open(READER_STYLESHEET).bufferedReader().use { it.readText() }
    val resources = context.resources
    return """
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
        <style>
        :root { --article-margin: ${resources.getInteger(R.integer.article_default_margin)}px; }
        $css
        </style>
        </head>
        <body textStyle="${if (darkTheme) 1 else 0}"
              lineHeightSetting="${resources.getInteger(R.integer.article_default_line_height)}"
              fontSizeSetting="${resources.getInteger(R.integer.article_default_font_size)}">
        $article
        </body>
        </html>
    """.trimIndent()
}

internal const val READER_STYLESHEET = "html/c/text.css"

/** Falls back to the host while the bookmark is still being read. */
internal fun readerTitle(bookmark: Bookmark?, url: String): String =
    bookmark?.title?.takeIf { it.isNotBlank() } ?: url.displayHost()

private fun String.displayHost(): String =
    runCatching { Uri.parse(this).host }.getOrNull().orEmpty()
