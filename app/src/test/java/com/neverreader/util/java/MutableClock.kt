package com.neverreader.util.java

class MutableClock(var time: Long) : Clock {
    override fun now(): Long {
        return time
    }
}
