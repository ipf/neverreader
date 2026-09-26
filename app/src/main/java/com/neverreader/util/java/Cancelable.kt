package com.neverreader.util.java

interface Cancelable {
    fun cancel()
    val isCancelled: Boolean
}

