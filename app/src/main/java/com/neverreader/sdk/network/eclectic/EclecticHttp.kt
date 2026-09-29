package com.neverreader.sdk.network.eclectic

import okio.BufferedSource
import java.io.InputStream
import java.net.CookieManager


/**
 * A generic interface for interacting with a Network/Http client.
 *
 *
 * Your app can use this interface and then implementations of this client can
 * be created for different http client libraries. This makes it very easy to swap out
 * or update http clients without having to change your app's code.
 */
interface EclecticHttp {
    /**
     * Parses the url and returns a request object that can be further modified or submitted.
     */
    fun buildRequest(url: String?): EclecticHttpRequest?

    /**
     * Send a request via POST. All of the params in your requests Uri will be encoded and sent
     * as the POST body. This method supports uploading files.
     */
    fun post(request: EclecticHttpRequest?, parser: ResponseParser?): Response?

    /**
     * Access the contents of a url.
     */
    fun get(request: EclecticHttpRequest?, parser: ResponseParser?): Response?

    /** Makes a request via DELETE.  */
    fun delete(request: EclecticHttpRequest?, parser: ResponseParser?): Response?

    val cookieManager: CookieManager?

    /**
     * Tell the client it is no longer needed. This will depend on the client but typically
     * this should cancel all pending connections and completely release resources and shut itself
     * down. This may be asynchronous or synchronous.
     */
    fun release()

    /**
     * Control whether or not new, future network connections are currently allowed.
     * While disabled, exceptions should be thrown for methods like get and post.
     * Defaults to enabled.
     */
    fun setEnabled(enabled: Boolean)

    fun interface ResponseParser {
        /**
         * Invoked by the client after connecting, provides an InputStream of the response
         * from the server.
         *
         * @param inputStream The content from the request response
         * @param response A response object so you can read status, headers, etc.
         * @return Optionally return a value you want to be available via [Response.getResponse] later.
         */
        fun readResponse(inputStream: Stream?, response: Response?): Any?
    }

    /** Your choice of how to work with the data. Only call one method, it is an error to call more than one.  */
    interface Stream {
        fun inputStream(): InputStream?
        fun okioBuffer(): BufferedSource?
    }

    interface Response {
        val statusCode: Int
        val response: Any?
        fun getHeader(name: String?): String?

        /** The url that this response came from, this could be different than the requested one if it was redirected.  */
        fun endUrl(): String?
    }

    enum class Logging {
        NONE,
        API,
        EVERYTHING,
    }
}
