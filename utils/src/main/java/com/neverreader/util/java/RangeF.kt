package com.neverreader.util.java

import kotlin.math.max
import kotlin.math.min

class RangeF(val min: Float, val max: Float) {
    enum class Constrain {
        NONE,
        MIN,
        MAX,
        BOTH
    }

    fun percentOf(value: Float): Float {
        return percentOf(min, max, value)
    }

    fun valueOf(percent: Float, constrain: Constrain?): Float {
        return valueOf(min, max, percent, constrain)
    }

    companion object {
        fun percentOf(min: Float, max: Float, value: Float): Float {
            if (value <= min) {
                return 0f
            } else if (value >= max) {
                return 1f
            } else {
                return (value - min) / (max - min)
            }
        }

        fun percentOf(min: Float, max: Float, value: Float, constrain: Constrain?): Float {
            if (value < min && (constrain == Constrain.MIN || constrain == Constrain.BOTH)) {
                return 0f
            } else if (value > max && (constrain == Constrain.MAX || constrain == Constrain.BOTH)) {
                return 1f
            } else {
                return (value - min) / (max - min)
            }
        }

        fun valueOf(min: Float, max: Float, percent: Float, constrain: Constrain?): Float {
            if (percent < 0 && (constrain == Constrain.MIN || constrain == Constrain.BOTH)) {
                return min
            } else if (percent > 1 && (constrain == Constrain.MAX || constrain == Constrain.BOTH)) {
                return max
            } else {
                return (percent * (max - min)) + min
            }
        }

        fun constrain(min: Float, max: Float, value: Float): Float {
            return max(min(value, max), min)
        }
    }
}
