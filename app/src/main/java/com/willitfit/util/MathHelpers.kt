package com.willitfit.util

import kotlin.math.sqrt

object MathHelpers {

    /**
     * Calculate 3D Euclidean distance between two points
     */
    fun distance3D(p1: FloatArray, p2: FloatArray): Double {
        require(p1.size >= 3 && p2.size >= 3) { "Points must have at least 3 components" }

        val dx = (p2[0] - p1[0]).toDouble()
        val dy = (p2[1] - p1[1]).toDouble()
        val dz = (p2[2] - p1[2]).toDouble()

        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculate 2D horizontal distance (X-Z plane, ignoring Y)
     */
    fun horizontalDistance(p1: FloatArray, p2: FloatArray): Double {
        require(p1.size >= 3 && p2.size >= 3) { "Points must have at least 3 components" }

        val dx = (p2[0] - p1[0]).toDouble()
        val dz = (p2[2] - p1[2]).toDouble()

        return sqrt(dx * dx + dz * dz)
    }

    /**
     * Calculate vertical distance (Y axis only)
     */
    fun verticalDistance(p1: FloatArray, p2: FloatArray): Double {
        require(p1.size >= 2 && p2.size >= 2) { "Points must have at least 2 components" }

        return kotlin.math.abs((p2[1] - p1[1]).toDouble())
    }

    /**
     * Calculate median of a sorted list
     */
    fun median(sortedValues: List<Float>): Float {
        if (sortedValues.isEmpty()) return 0f

        val middle = sortedValues.size / 2

        return if (sortedValues.size % 2 == 0) {
            (sortedValues[middle - 1] + sortedValues[middle]) / 2f
        } else {
            sortedValues[middle]
        }
    }

    /**
     * Calculate median of a list (will sort internally)
     */
    fun medianUnsorted(values: List<Float>): Float {
        return median(values.sorted())
    }

    /**
     * Linear interpolation between two values
     */
    fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }

    /**
     * Clamp a value between min and max
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    /**
     * Convert meters to centimeters
     */
    fun metersToCm(meters: Double): Double = meters * 100.0

    /**
     * Convert centimeters to meters
     */
    fun cmToMeters(cm: Double): Double = cm / 100.0

    /**
     * Calculate angle between two 2D vectors (in degrees)
     */
    fun angleBetweenVectors(v1x: Float, v1z: Float, v2x: Float, v2z: Float): Float {
        val dot = v1x * v2x + v1z * v2z
        val mag1 = sqrt((v1x * v1x + v1z * v1z).toDouble())
        val mag2 = sqrt((v2x * v2x + v2z * v2z).toDouble())

        if (mag1 == 0.0 || mag2 == 0.0) return 0f

        val cosAngle = (dot / (mag1 * mag2)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(kotlin.math.acos(cosAngle)).toFloat()
    }
}
