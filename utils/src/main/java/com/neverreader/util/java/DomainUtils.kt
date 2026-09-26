package com.neverreader.util.java

import org.apache.commons.lang3.StringUtils
import kotlin.math.min

object DomainUtils {
    /**
     * Will take a url such as http://www.stackoverflow.com and return www.stackoverflow.com
     */
    fun getHost(url: String?): String {
        if (url == null || url.length == 0) return ""

        var doubleslash = url.indexOf("//")
        if (doubleslash == -1) doubleslash = 0
        else doubleslash += 2

        var slashEnd = url.indexOf('/', doubleslash)
        slashEnd = if (slashEnd >= 0) slashEnd else url.length

        var queryEnd = url.indexOf('?', doubleslash)
        queryEnd = if (queryEnd >= 0) queryEnd else url.length

        return url.substring(doubleslash, min(slashEnd, queryEnd))
    }


    /**  Based on : http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/2.3.3_r1/android/webkit/CookieManager.java#CookieManager.getBaseDomain%28java.lang.String%29
     * Get the base domain for a given host or url. E.g. mail.google.com will return google.com
     */
    fun getBaseDomain(url: String?): String {
        val host = getHost(url)

        var startIndex = 0
        var nextIndex = host.indexOf('.')
        val lastIndex = host.lastIndexOf('.')
        while (nextIndex < lastIndex) {
            startIndex = nextIndex + 1
            nextIndex = host.indexOf('.', startIndex)
        }
        if (startIndex > 0) {
            return host.substring(startIndex)
        } else {
            return host
        }
    }

    /**
     * Reduces `url` down to the host part, removing slashes, and `www.` subdomain.
     * Other subdomains are fine to keep.
     */
    fun cleanHostFromUrl(url: String?): String {
        val host = getHost(url)
        return StringUtils.replaceOnce(host, "www.", "")
    }
}
