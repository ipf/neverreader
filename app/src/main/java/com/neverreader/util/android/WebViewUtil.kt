package com.neverreader.util.android

import android.webkit.ValueCallback
import android.webkit.WebView
import org.apache.commons.lang3.StringEscapeUtils
import org.apache.commons.lang3.StringUtils

object WebViewUtil {
    /**
     * Gets the currently selected text if any.
     *
     * @param webview The webview to get the text selection from.
     * @param callback A callback after it has retrieved the text selection. Be aware this could potentially call back off the ui thread.
     */
    fun getSelectedText(webview: WebView, callback: SelectedTextCallback) {
        // Chromium based webview, use javascript

        webview.evaluateJavascript("window.getSelection().toString()", ValueCallback { s: String? ->
            var s = s
            s = StringUtils.trimToNull(s)
            s = s.replace("([^\\\\]|^)\"".toRegex(), "$1") // Remove unescaped quotes.
            s = StringEscapeUtils.unescapeJava(s) // This string comes from web escaped for java.
            callback.onTextSelectionRetrieved(s)
        })
    }

    fun selectAll(webView: WebView) {
        webView.evaluateJavascript("document.execCommand(\"selectAll\");", null)
    }

    interface SelectedTextCallback {
        fun onTextSelectionRetrieved(selectedText: String?)
    }
}
