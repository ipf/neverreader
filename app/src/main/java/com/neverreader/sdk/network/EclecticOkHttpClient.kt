package com.neverreader.sdk.network

import com.neverreader.sdk.network.eclectic.EclecticHttp
import com.neverreader.sdk.network.eclectic.EclecticHttpRequest
import com.neverreader.sdk.network.eclectic.KeyFileValue
import com.neverreader.sdk.network.eclectic.KeyValue
import okhttp3.Call
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.CookieJar
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.InputStream
import java.net.CookieHandler
import java.net.CookieManager
import java.net.URLConnection

/**
 * An [EclecticHttp] powered by OkHttp.
 */
class EclecticOkHttpClient(client: OkHttpClient) : EclecticHttp {
    private val mClient: OkHttpClient

    /** Increments each time release() is called so all requests can be canceled at release() time.  */
    private var mReleaseTag = 1
    private var mIsEnabled = true

    init {
        if (CookieHandler.getDefault() == null) {
            CookieHandler.setDefault(CookieManager())
        }

        mClient = client.newBuilder()
            .cookieJar(CookieJar.NO_COOKIES)
            .build()
    }

    private enum class Method(val value: String) {
        POST("POST"), DELETE("DELETE"), PATCH("PATCH"), PUT("PUT")
    }

    @Throws(Exception::class)
    override fun post(
        request: EclecticHttpRequest?,
        parser: EclecticHttp.ResponseParser?
    ): EclecticHttp.Response {
        return execute(request!!, parser, Method.POST)
    }

    @Throws(Exception::class)
    override fun delete(
        request: EclecticHttpRequest?,
        parser: EclecticHttp.ResponseParser?
    ): EclecticHttp.Response {
        return execute(request!!, parser, Method.DELETE)
    }

    /** Executes requests that use a request body.  */
    @Throws(Exception::class)
    private fun execute(
        request: EclecticHttpRequest,
        parser: EclecticHttp.ResponseParser?,
        method: Method
    ): EclecticHttp.Response {
        checkEnabled()

        val okHttpRequestBuilder = Request.Builder()

        // Headers
        attachHeaders(okHttpRequestBuilder, request)

        // Query
        val params: MutableList<KeyValue> = request.params!!.filterNotNull().toMutableList()
        request.clearQuery() // Moved query to post body, so clear it from the url. TODO don't do this in this way, it makes the request object change, so callers of this have their object change.

        okHttpRequestBuilder
            .tag(mReleaseTag)
            .url(request.url!!)

        val body: RequestBody = body(request, params)
        okHttpRequestBuilder.method(method.name, body)
        return execute(okHttpRequestBuilder.build(), parser)
    }

    private fun body(request: EclecticHttpRequest, params: MutableList<KeyValue>): RequestBody {
        // Post body
        if (request.json != null) {
            // JSON body
            val mediaType: MediaType? = "application/json".toMediaTypeOrNull()
            val json = request.json
            return json!!.toRequestBody(mediaType!!)
        } else if (request.files?.isNotEmpty() == true) {
            // Multipart

            val multiBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)

            // Files
            val files: MutableList<KeyFileValue> = request.files!!.filterNotNull().toMutableList()
            for (file in files) {
                val headerMap: MutableMap<String?, String?> = HashMap()
                headerMap["Content-Disposition"] = "form-data;" +
                        " " +
                        "name=\"" + file.key + "\";" +
                        " " +
                        "filename=\"" + file.value!!.name + "\""
                val headers: Headers = Headers.Builder().build()

                var mediaType: MediaType? = null
                val mimeType = URLConnection.guessContentTypeFromName(file.value.absolutePath)
                if (mimeType != null) {
                    mediaType = mimeType.toMediaTypeOrNull()
                }

                multiBuilder.addFormDataPart(file.key!!, file.value.name, RequestBody.create(mediaType,
                    file.value
                ))
            }

            // Query
            for (param in params) {
                multiBuilder.addFormDataPart(param.key!!, param.value!!)
            }

            return multiBuilder.build()
        } else {
            // Encoded query only
            val queryBuilder = FormBody.Builder()
            for (param in params) {
                queryBuilder.add(param.key!!, param.value!!)
            }
            return queryBuilder.build()
        }
    }

    @Throws(Exception::class)
    override fun get(request: EclecticHttpRequest?, parser: EclecticHttp.ResponseParser?): EclecticHttp.Response {
        checkEnabled()

        val okHttpRequestBuilder = Request.Builder()
        attachHeaders(okHttpRequestBuilder, request!!)
        val okRequest: Request = okHttpRequestBuilder
            .url(request.url!!)
            .tag(mReleaseTag)
            .build()

        return execute(okRequest, parser)
    }

    override fun setEnabled(enabled: Boolean) {
        mIsEnabled = enabled
    }

    private fun checkEnabled() {
        if (!mIsEnabled) {
            throw RuntimeException("Network disabled")
        }
    }

    private fun attachHeaders(okHttpRequestBuilder: Request.Builder, request: EclecticHttpRequest) {
        val headers: MutableSet<KeyValue> = request.headers!!.filterNotNull().toMutableSet()
        for (header in headers) {
            if (StringUtils.equalsIgnoreCase(header.value, "gzip") && StringUtils.equalsIgnoreCase(
                    header.key,
                    "Accept-Encoding"
                )
            ) {
                continue  // OkHttp handles gzip by default and has a bug if you declare it manually where some cases won't ungzip https://github.com/square/okhttp/issues/1927
            }
            okHttpRequestBuilder.header(header.key!!, header.value!!)
        }
    }

    @Throws(Exception::class)
    private fun execute(request: Request, parser: EclecticHttp.ResponseParser?): EclecticHttp.Response {
        val okResponse = mClient.newCall(request).execute()
        try {
            val result = OkResponseWrapper(okResponse)
            if (parser != null) {
                result.response = parser.readResponse(object : EclecticHttp.Stream {
                    var used: Boolean = false
                    fun used() {
                        if (used) throw RuntimeException("stream already used")
                        used = true
                    }

                    override fun inputStream(): InputStream {
                        used()
                        return okResponse.body!!.byteStream()
                    }

                    override fun okioBuffer(): BufferedSource {
                        used()
                        return okResponse.body!!.source()
                    }
                }, result)
            }
            return result
        } finally {
            okResponse.close()
        }
    }

    override val cookieManager: CookieManager?
        get() = CookieHandler.getDefault() as CookieManager?

    override fun release() {
        cancelByTag(mClient.dispatcher.runningCalls() as MutableList<Call>, mReleaseTag)
        cancelByTag(mClient.dispatcher.queuedCalls() as MutableList<Call>, mReleaseTag)
        mReleaseTag++
    }

    private fun cancelByTag(calls: MutableList<Call>, releaseTag: Int) {
        for (call in calls) {
            val tag = call.request().tag()
            if (tag != null && tag == releaseTag) {
                call.cancel()
            }
        }
    }

    private class OkResponseWrapper(res: Response) : EclecticHttp.Response {
        override val statusCode: Int
        private val mHeaders: Headers
        private val mEndUrl: String

        override var response: Any? = null

        init {
            this.statusCode = res.code
            mHeaders = res.headers
            mEndUrl = res.request.url.toString()
        }

        override fun getHeader(name: String?): String? {
            return name?.let { mHeaders[it] }
        }

        override fun endUrl(): String {
            return mEndUrl
        }
    }

    override fun buildRequest(url: String?): EclecticHttpRequest {
        val request = url?.toHttpUrlOrNull() ?: throw RuntimeException("Could not parse $url")
        return object : EclecticHttpRequest {
            private val builder: HttpUrl.Builder = request.newBuilder()
            override val headers: MutableSet<KeyValue?> = HashSet<KeyValue?>()
            override val files: MutableList<KeyFileValue?> = ArrayList<KeyFileValue?>()
            override var json: String? = null

            override fun appendQueryParameter(key: String?, value: String?): EclecticHttpRequest {
                if (key != null) {
                    builder.addQueryParameter(key, value)
                }
                return this
            }

            override fun addFile(name: String?, file: File?): EclecticHttpRequest {
                files.add(KeyFileValue(name, file))
                return this
            }

            override fun setHeader(name: String?, value: String?): EclecticHttpRequest {
                headers.add(KeyValue(name, value))
                return this
            }

            override fun setJson(body: String?): EclecticHttpRequest {
                json = body
                return this
            }

            override fun clearQuery(): EclecticHttpRequest {
                builder.query(null)
                return this
            }

            override val url: String
                get() = builder.toString()

            override val params: MutableList<KeyValue?>
                get() {
                    val keyvals: MutableList<KeyValue?> = ArrayList<KeyValue?>()
                    val built: HttpUrl = builder.build()
                    for (name in built.queryParameterNames) {
                        keyvals.add(KeyValue(name, built.queryParameter(name)))
                    }
                    return keyvals
                }

            override val path: String
                get() = builder.build().encodedPath

        }
    }


}
