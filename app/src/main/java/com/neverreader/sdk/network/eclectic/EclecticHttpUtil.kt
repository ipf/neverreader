package com.neverreader.sdk.network.eclectic

import com.neverreader.sdk.network.eclectic.EclecticHttp.ResponseParser
import org.apache.commons.lang3.StringUtils
import java.nio.charset.Charset
import java.util.regex.Pattern


/**
 *
 */
object EclecticHttpUtil {
    /**
     * Convenience method for obtaining the content of a url as a string.
     * @param request The request to make as a GET request
     * @return The content or null if there was an error, or it returned anything other than a 200 status.
     * @see .postString
     */
    fun getString(request: EclecticHttpRequest?, client: EclecticHttp): String? {
        try {
            return client.get(
                request,
                ResponseParser { `in`, res ->
                    val statusCode = res!!.statusCode
                    if (statusCode != 200) return@ResponseParser null
                    val type = EclecticHttpUtil.getContentType(res)
                    val charset =
                        if (type != null && type.encoding != null) type.encoding else "UTF-8"
                    `in`!!.okioBuffer()!!.readString(Charset.forName(charset))
                })?.response as String?
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Convenience method for obtaining the content of a url as a string.
     * @param request The request to make as a POST request
     * @return The content or null if there was an error, or it returned anything other than a 200 status.
     * @see .getString
     */
    fun postString(request: EclecticHttpRequest?, client: EclecticHttp): String? {
        try {
            return client.post(
                request,
                ResponseParser { `in`, res ->
                    val statusCode = res!!.statusCode
                    if (statusCode != 200) return@ResponseParser null
                    val type = getContentType(res)
                    val charset =
                        if (type != null && type.encoding != null) type.encoding else "UTF-8"
                    `in`!!.okioBuffer()!!.readString(Charset.forName(charset))
                })?.response as String?
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Helper for extracting the mimeType and encoding of a response.
     * @return The content type or null if it was not present in the headers.
     */
    fun getContentType(response: EclecticHttp.Response): ContentType? {
        val contentType = response.getHeader("Content-Type")
        if (contentType != null) { // OPT reuse pattern instance?
            val matcher = Pattern.compile(
                "([a-z\\-\\_]*/[a-z\\-\\_]*)(?:;\\s*?charset=([a-z\\-\\_0-9]*))?",
                Pattern.CASE_INSENSITIVE
            ).matcher(contentType)
            if (matcher.find()) {
                return ContentType(matcher.group(1), matcher.group(2))
            }
        }
        return null
    }

    /**
     * Helper for extracting the mimeType from a response.
     * @return The mimeType or null if it was not present in the headers.
     * @see .getContentType
     */
    fun getMimeType(response: EclecticHttp.Response): String? {
        val type = getContentType(response)
        return type?.mimeType
    }

    /**
     * Helper for extracting content length as a byte length
     * @return the byte count from the content length header or -1 if it couldn't be read
     */
    fun getContentLength(res: EclecticHttp.Response): Long {
        try {
            val v = res.getHeader("Content-Length")
            return v?.toLong() ?: -1
        } catch (t: Throwable) {
            return -1
        }
    }

    class ContentType(mimeType: String?, encoding: String?) {
        val mimeType: String? = StringUtils.trimToNull(mimeType)
        val encoding: String? = StringUtils.trimToNull(encoding)
    }
}
