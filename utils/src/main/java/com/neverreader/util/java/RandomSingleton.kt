package com.neverreader.util.java

import java.util.Random

/**
 * A shared, single instance of [Random].
 */
enum class RandomSingleton {
    INSTANCE;

    private val random = Random()

    companion object {
        fun get(): Random {
            return RandomSingleton.INSTANCE.random
        }
    }
}
