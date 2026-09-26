package com.neverreader.util.java

object IntUtils {
    fun compare(x: Int, y: Int): Int {
        return if (x < y) -1 else (if (x == y) 0 else 1)
    }
}
