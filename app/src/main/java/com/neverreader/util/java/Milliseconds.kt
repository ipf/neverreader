package com.neverreader.util.java


object Milliseconds {
    const val SECOND: Long = 1000
    val MINUTE: Long = SECOND * 60
    val HOUR: Long = MINUTE * 60
    val DAY: Long = HOUR * 24
    val WEEK: Long = DAY * 7
    val YEAR: Long = DAY * 365

    /** @return The provided number of seconds as milliseconds
     */
    fun seconds(seconds: Int): Long {
        return SECOND * seconds
    }

    /** @return The provided number of minutes as milliseconds
     */
    fun minutesToMillis(minutes: Int): Long {
        return MINUTE * minutes
    }

    /** @return The provided number of millis as minutes
     */
    fun millisToMinutes(millis: Long): Long {
        return (millis / MINUTE.toDouble()).toLong()
    }

    /**
     * How long has it been since a previous time.
     *
     * @param time
     * @return
     */
    fun since(time: Long): Long {
        return System.currentTimeMillis() - time
    }

    /**
     * Converts a millis timestamp to a seconds one.
     * @param millis
     * @return
     */
    fun toSeconds(millis: Long): Long {
        return (millis / SECOND.toDouble()).toLong()
    }

    fun fromNanos(nanos: Double): Long {
        return (nanos / 1000000).toLong()
    }
}
