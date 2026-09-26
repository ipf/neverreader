package com.neverreader.util.java

import org.apache.commons.lang3.StringUtils
import java.math.RoundingMode
import java.text.DecimalFormat


class StopWatch {
    private var currentStart: Long = 0
    private var total: Long = 0
    private var min: Long = 0
    private var max: Long = 0
    private var lastLap: Long = 0
    private var intervals = 0

    init {
        warmup()
    }

    private fun warmup() {
        resume()
        pause()
        reset()
    }

    fun resume() {
        if (currentStart == 0L) {
            currentStart = System.nanoTime()
            intervals++
        }
    }

    fun pause() {
        if (currentStart > 0) {
            val lap = System.nanoTime() - currentStart
            total += lap
            if (intervals == 1) {
                max = lap
                min = lap
            } else {
                max = kotlin.math.max(max, lap)
                min = kotlin.math.min(min, lap)
            }
            lastLap = lap
        }
        currentStart = 0
    }

    /**
     * Manually record a lap in nanoseconds.
     * Can be useful for combining results in a multithreaded case.
     */
    @Synchronized
    fun addLap(lap: Long) {
        intervals++
        total += lap
        if (intervals == 1) {
            max = lap
            min = lap
        } else {
            max = kotlin.math.max(max, lap)
            min = kotlin.math.min(min, lap)
        }
        lastLap = lap
    }

    /**
     * Combine results.
     * Can be useful for combining results in a multithreaded case.
     */
    @Synchronized
    fun merge(src: StopWatch) {
        currentStart = 0
        total += src.total
        min = kotlin.math.min(min, src.min)
        max = kotlin.math.max(max, src.max)
        lastLap = src.lastLap
        intervals += src.intervals
    }

    fun length(): Long {
        return millis(lengthNanos())
    }

    fun lengthNanos(): Long {
        var length = total
        if (currentStart > 0) {
            length += System.nanoTime() - currentStart
        }
        return length
    }

    fun intervals(): Long {
        return intervals.toLong()
    }

    fun avg(): Long {
        return millis(avgNanos())
    }

    fun avgNanos(): Long {
        if (intervals > 0) {
            return (lengthNanos() / intervals.toDouble()).toLong()
        } else {
            return -1
        }
    }

    fun min(): Long {
        return millis(min)
    }

    fun minNanos(): Long {
        return min
    }

    fun max(): Long {
        return millis(max)
    }

    fun maxNanos(): Long {
        return max
    }

    fun reset() {
        currentStart = 0
        total = 0
        intervals = 0
        min = 0
        max = 0
    }

    fun lastLap(): Long {
        return millis(lastLap)
    }

    fun lastLapNanos(): Long {
        return lastLap
    }

    /**
     * Returns the current [.length] and resets the time back to 0.
     * If already running, it continues running, if stopped it stays stopped.
     * @return
     */
    fun extract(): Long {
        return millis(extractNanos())
    }

    fun extractNanos(): Long {
        val length = lengthNanos()
        total = 0
        if (currentStart > 0) {
            currentStart = System.nanoTime()
        }
        return length
    }

    override fun toString(): String {
        return prettyPrint()
    }

    fun log(): String {
        return "laps:" + intervals +
                " min:" + min() +
                " max:" + max() +
                " avg:" + avg() +
                " total:" + length()
    }

    fun logNanos(): String {
        return "laps:" + intervals +
                " min:" + minNanos() +
                " max:" + maxNanos() +
                " avg:" + avgNanos() +
                " total:" + lengthNanos()
    }

    /**
     * Produces a log of all values in fixed column widths so when comparing multiple outputs they line up in a readable way.
     * For example, here are four example outputs of this method in a row:
     * <pre>
     * 300      laps |     11.905ms min |    110.004ms max |     26.501ms avg |   7950.223ms total
     * 300      laps |     11.611ms min |     56.118ms max |     22.565ms avg |   6769.625ms total
     * 300      laps |      0.000ms min |      0.037ms max |      0.002ms avg |      0.496ms total
     * 300      laps |      0.000ms min |      0.006ms max |      0.000ms avg |      0.105ms total
    </pre> *
     */
    @Synchronized
    fun prettyPrint(): String {
        val b = StringBuilder()
        b.append(StringUtils.rightPad(intervals.toString(), 8)).append(" laps | ")
        b.append(formatted(lastLap, 6, 3)).append("ms lap | ") // The previous lap
        b.append(formatted(minNanos(), 6, 3)).append("ms min | ")
        b.append(formatted(maxNanos(), 6, 3)).append("ms max | ")
        b.append(formatted(avgNanos(), 6, 3)).append("ms avg | ")
        b.append(formatted(lengthNanos(), 6, 3)).append("ms total")
        return b.toString()
    }

    companion object {
        fun formatted(nanoseconds: Long, integerPlacesMin: Int, decimalPlacesMin: Int): String {
            val df = DecimalFormat(
                StringUtils.repeat('#', integerPlacesMin) + "." + StringUtils.repeat(
                    '#',
                    decimalPlacesMin
                )
            )
            df.setRoundingMode(RoundingMode.HALF_UP)
            val str: String? = df.format(millisPrecise(nanoseconds))
            val parts = StringUtils.split(str, ".")
            return (StringUtils.leftPad(parts[0], integerPlacesMin, ' ')
                    + "."
                    + StringUtils.rightPad(
                if (parts.size > 1) parts[1] else "",
                decimalPlacesMin,
                '0'
            ))
        }

        private fun millis(nanos: Long): Long {
            return millisPrecise(nanos).toLong()
        }

        private fun millisPrecise(nanos: Long): Double {
            return nanos / 1000000.0
        }
    }
}
