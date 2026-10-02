package com.neverreader.util.java

import android.util.Patterns
import android.webkit.URLUtil
import java.util.regex.Pattern

object UrlFinder {
    private var mPattern: Pattern? = null

    /**
     * Convenience for [.getUrlsFromText] with no limit.
     */
    fun getUrlsFromText(textToSearch: String?): ArrayList<String?>? {
        return getUrlsFromText(textToSearch, 0)
    }

    /**
     * Returns a list of UrlMatchs for any urls found within the provided text. If the provided text is null
     * then this method will return null. If no urls are found it will return an empty list. All urls are validated with
     * URLUtil.isValidUrl(url)) before being added to the list. If the url is not valid it will not be included in the list.
     *
     * @param limit The maximum number of urls to find. Pass 0 for no limit.
     */
    fun getUrlsFromText(textToSearch: String?, limit: Int): ArrayList<String?>? {
        var limit = limit
        if (textToSearch == null) {
            return null
        }

        val limitResults = limit > 0
        val urls = ArrayList<String?>()

        try {
            if (mPattern == null) {
                mPattern = Patterns.WEB_URL
            }

            val matcher = mPattern!!.matcher(textToSearch)
            while ((!limitResults || limit > 0) && matcher.find()) {
                val url = matcher.group()
                if (URLUtil.isValidUrl(url)) {
                    urls.add(url)
                    limit--
                } else {
                    try {
                        // Does this check ever fail? Do Patterns.WEB_URL ever match invalid urls?
                    } catch (ignored: Exception) {
                        // Don't crash if logging fails.
                    }
                }
            }
        } catch (t: Throwable) {
            return null
        }

        return urls
    }

    /**
     * Returns the first match from a getUrlsFromText() call. If you pass null text this will return null.
     * If no valid urls are found this will return null.
     */
    fun getFirstUrlOrNull(textToSearch: String?): String? {
        val urls = getUrlsFromText(textToSearch, 1)
        if (urls.isNullOrEmpty()) {
            return null
        }

        return urls[0]
    }
}
