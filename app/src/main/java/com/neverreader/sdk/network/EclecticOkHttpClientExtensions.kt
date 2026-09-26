package com.neverreader.sdk.network

import okhttp3.OkHttpClient

fun OkHttpClient.toEclecticOkHttpClient() = EclecticOkHttpClient(this)
