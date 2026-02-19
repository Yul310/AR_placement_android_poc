package com.willitfit.data.model

// Note: RaycastHitType is defined in Measurement.kt
// This file exists for organizational clarity but the enum is consolidated there

// Additional hit result data class used during AR sampling
data class SamplingResult(
    val position: FloatArray,
    val stabilitySpreadCm: Double,
    val sampleCount: Int,
    val primaryHitType: RaycastHitType
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SamplingResult
        if (!position.contentEquals(other.position)) return false
        if (stabilitySpreadCm != other.stabilitySpreadCm) return false
        if (sampleCount != other.sampleCount) return false
        if (primaryHitType != other.primaryHitType) return false
        return true
    }

    override fun hashCode(): Int {
        var result = position.contentHashCode()
        result = 31 * result + stabilitySpreadCm.hashCode()
        result = 31 * result + sampleCount
        result = 31 * result + primaryHitType.hashCode()
        return result
    }
}

data class MeasurementPoint(
    val position: FloatArray,
    val hitType: RaycastHitType,
    val trackingConfidence: Double,
    val stabilitySpreadCm: Double
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MeasurementPoint
        if (!position.contentEquals(other.position)) return false
        if (hitType != other.hitType) return false
        if (trackingConfidence != other.trackingConfidence) return false
        if (stabilitySpreadCm != other.stabilitySpreadCm) return false
        return true
    }

    override fun hashCode(): Int {
        var result = position.contentHashCode()
        result = 31 * result + hitType.hashCode()
        result = 31 * result + trackingConfidence.hashCode()
        result = 31 * result + stabilitySpreadCm.hashCode()
        return result
    }
}
