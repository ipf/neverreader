package com.neverreader.util.java.function

/**
 * A functional interface that takes a value and does not return a value
 * @param <V> the input value type
</V> */
interface Take<V> {
    fun apply(value: V?)
}
