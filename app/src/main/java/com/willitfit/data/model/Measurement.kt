package com.willitfit.data.model

enum class MeasurementMode {
    DOOR,
    SPACE,
    VIRTUAL_PLACEMENT
}

enum class ConfidenceLevel {
    HIGH,
    MEDIUM,
    LOW
}

enum class RaycastHitType(
    val confidenceMultiplier: Double,
    val uncertaintyAddition: Double
) {
    EXISTING_PLANE(1.0, 0.0),
    DEPTH_POINT(0.85, 0.5),
    ESTIMATED_SURFACE(0.6, 1.5)
}

data class MeasurementResult(
    val dimension: String,
    val valueCm: Double,
    val uncertaintyCm: Double,
    val confidenceScore: Double,
    val hitType: RaycastHitType
) {
    val confidenceLevel: ConfidenceLevel
        get() = when {
            confidenceScore >= 0.8 -> ConfidenceLevel.HIGH
            confidenceScore >= 0.6 -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }

    val formattedValue: String
        get() = "${valueCm.toInt()} cm"

    val formattedUncertainty: String
        get() = "+/- ${String.format("%.1f", uncertaintyCm)} cm"
}

data class DoorMeasurement(
    val width: MeasurementResult? = null,
    val height: MeasurementResult? = null
) {
    val isComplete: Boolean
        get() = width != null && height != null
}

data class SpaceMeasurement(
    val width: MeasurementResult? = null,
    val depth: MeasurementResult? = null,
    val height: MeasurementResult? = null
) {
    val isComplete: Boolean
        get() = width != null && depth != null && height != null
}

enum class PlaneAlignment {
    HORIZONTAL,
    VERTICAL,
    ALL
}

sealed class MeasurementStep(
    val instruction: String,
    val alignment: PlaneAlignment,
    val requiresPlaneHit: Boolean
) {
    // Door (3 taps)
    object DoorBottomLeft : MeasurementStep(
        "Tap BOTTOM-LEFT corner of door",
        PlaneAlignment.HORIZONTAL,
        requiresPlaneHit = true  // Bottom points MUST be on floor plane
    )
    object DoorBottomRight : MeasurementStep(
        "Tap BOTTOM-RIGHT corner of door",
        PlaneAlignment.HORIZONTAL,
        requiresPlaneHit = true  // Bottom points MUST be on floor plane
    )
    object DoorTopLeft : MeasurementStep(
        "Tap TOP-LEFT corner of door",
        PlaneAlignment.VERTICAL,
        requiresPlaneHit = false  // Top corner can use estimated if needed
    )

    // Space (6 taps)
    object SpaceWidthLeft : MeasurementStep(
        "Tap LEFT wall",
        PlaneAlignment.VERTICAL,
        requiresPlaneHit = false  // Walls - prefer plane but allow fallback
    )
    object SpaceWidthRight : MeasurementStep(
        "Tap RIGHT wall",
        PlaneAlignment.VERTICAL,
        requiresPlaneHit = false
    )
    object SpaceDepthFront : MeasurementStep(
        "Tap FRONT wall",
        PlaneAlignment.VERTICAL,
        requiresPlaneHit = false
    )
    object SpaceDepthBack : MeasurementStep(
        "Tap BACK wall",
        PlaneAlignment.VERTICAL,
        requiresPlaneHit = false
    )
    object SpaceHeightFloor : MeasurementStep(
        "Tap FLOOR",
        PlaneAlignment.HORIZONTAL,
        requiresPlaneHit = true  // Floor point must be on floor plane
    )
    object SpaceHeightCeiling : MeasurementStep(
        "Tap CEILING",
        PlaneAlignment.ALL,
        requiresPlaneHit = false  // Ceiling is hard, allow estimated
    )

    object Complete : MeasurementStep(
        "Measurement complete!",
        PlaneAlignment.ALL,
        requiresPlaneHit = false
    )
}

sealed class SamplingState {
    object Idle : SamplingState()
    data class Sampling(val progress: Double) : SamplingState()
    data class Failed(val reason: String) : SamplingState()
}
