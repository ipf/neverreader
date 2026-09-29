package com.neverreader.util.java

class Range constructor(min: Int = 0, max: Int = 0) {
    var min: Int = 0
    var max: Int = 0

    init {
        set(min, max)
    }

    fun set(min: Int, max: Int) {
        this.min = min
        this.max = max
    }

    override fun toString(): String {
        return min.toString() + "-" + max
    }

    /**
     * @param value
     * @return A value <= the max and >= the min of the range. If the value is greater than the max, the max is returned. If the value is less than the min the min is returned. If the value is already within range it is returned.
     */
    fun limit(value: Int): Int {
        if (value >= max) {
            return max
        } else if (value <= min) {
            return min
        }
        return value
    }

    fun contains(value: Int): Boolean {
        return isWithin(min.toFloat(), max.toFloat(), value.toFloat())
    }

    companion object {
        fun limit(min: Float, max: Float, value: Float): Float {
            if (value < min) {
                return min
            } else if (value > max) {
                return max
            } else {
                return value
            }
        }

        fun limit(min: Int, max: Int, value: Int): Int {
            if (value < min) {
                return min
            } else if (value > max) {
                return max
            } else {
                return value
            }
        }

        fun isWithin(min: Float, max: Float, value: Float): Boolean {
            return value <= max && value >= min
        }


        fun percentBetween(percent: Float, min: Float, max: Float): Float {
            val length = max - min
            val position = percent * length
            return min + position
        }
    }
}
