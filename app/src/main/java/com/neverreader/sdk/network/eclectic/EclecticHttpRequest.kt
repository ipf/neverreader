package com.neverreader.sdk.network.eclectic

import java.io.File

/**
 * A representation of an http request. Use [EclecticHttp.buildRequest] to obtain a new instance.
 */
interface EclecticHttpRequest {
    fun appendQueryParameter(key: String?, value: String?): EclecticHttpRequest?
    fun addFile(name: String?, file: File?): EclecticHttpRequest?
    fun setHeader(name: String?, value: String?): EclecticHttpRequest?
    fun setJson(body: String?): EclecticHttpRequest?
    fun clearQuery(): EclecticHttpRequest?

    val url: String?
    val params: MutableList<KeyValue?>?
    val files: MutableList<KeyFileValue?>?
    val headers: MutableSet<KeyValue?>?
    val path: String?
    val json: String?
}
