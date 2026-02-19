package com.willitfit.service

import com.willitfit.data.model.DoorMeasurement
import com.willitfit.data.model.MeasurementMode
import com.willitfit.data.model.MeasurementPoint
import com.willitfit.data.model.MeasurementResult
import com.willitfit.data.model.MeasurementStep
import com.willitfit.data.model.RaycastHitType
import com.willitfit.data.model.SamplingResult
import com.willitfit.data.model.SpaceMeasurement
import com.willitfit.util.MathHelpers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs
import kotlin.math.sqrt

class MeasurementFlowController {

    companion object {
        private const val BASE_UNCERTAINTY_CM = 0.5
        private const val Y_CONSISTENCY_THRESHOLD_METERS = 0.03f  // 3cm
    }

    private val _currentStep = MutableStateFlow<MeasurementStep>(MeasurementStep.DoorBottomLeft)
    val currentStep: StateFlow<MeasurementStep> = _currentStep

    private val _doorMeasurement = MutableStateFlow(DoorMeasurement())
    val doorMeasurement: StateFlow<DoorMeasurement> = _doorMeasurement

    private val _spaceMeasurement = MutableStateFlow(SpaceMeasurement())
    val spaceMeasurement: StateFlow<SpaceMeasurement> = _spaceMeasurement

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    // Corner point storage
    private var doorBottomLeftPoint: MeasurementPoint? = null
    private var doorBottomRightPoint: MeasurementPoint? = null

    // Y-consistency error message (exposed for UI to display)
    private val _yConsistencyError = MutableStateFlow<String?>(null)
    val yConsistencyError: StateFlow<String?> = _yConsistencyError

    // Space measurement points
    private var spaceWidthLeftPoint: MeasurementPoint? = null
    private var spaceDepthFrontPoint: MeasurementPoint? = null
    private var spaceHeightFloorPoint: MeasurementPoint? = null

    private var currentMode: MeasurementMode = MeasurementMode.DOOR

    fun startFlow(mode: MeasurementMode) {
        currentMode = mode
        reset()

        _currentStep.value = when (mode) {
            MeasurementMode.DOOR -> MeasurementStep.DoorBottomLeft
            MeasurementMode.SPACE -> MeasurementStep.SpaceWidthLeft
            MeasurementMode.VIRTUAL_PLACEMENT -> MeasurementStep.Complete
        }
    }

    fun handleSamplingComplete(result: SamplingResult, trackingConfidence: Double) {
        val point = MeasurementPoint(
            position = result.position,
            hitType = result.primaryHitType,
            trackingConfidence = trackingConfidence,
            stabilitySpreadCm = result.stabilitySpreadCm
        )
        processPoint(point)
    }

    private fun processPoint(point: MeasurementPoint) {
        when (_currentStep.value) {
            // Door measurement (3 taps)
            MeasurementStep.DoorBottomLeft -> {
                doorBottomLeftPoint = point
                _currentStep.value = MeasurementStep.DoorBottomRight
            }
            MeasurementStep.DoorBottomRight -> {
                doorBottomRightPoint = point
                doorBottomLeftPoint?.let { bottomLeft ->
                    // Validate Y-consistency: bottom points should be at same height
                    val yDifference = abs(bottomLeft.position[1] - point.position[1])
                    if (yDifference > Y_CONSISTENCY_THRESHOLD_METERS) {
                        val yDiffCm = (yDifference * 100).toInt()
                        _yConsistencyError.value = "Bottom points not level (${yDiffCm}cm difference). Re-tap both corners on flat floor."
                        // Reset to start of door measurement
                        doorBottomLeftPoint = null
                        doorBottomRightPoint = null
                        _currentStep.value = MeasurementStep.DoorBottomLeft
                        return
                    }

                    // Clear any previous error
                    _yConsistencyError.value = null

                    val width = createHorizontalMeasurement(bottomLeft, point, "width")
                    _doorMeasurement.value = _doorMeasurement.value.copy(width = width)
                    _currentStep.value = MeasurementStep.DoorTopLeft
                }
            }
            MeasurementStep.DoorTopLeft -> {
                doorBottomLeftPoint?.let { bottomLeft ->
                    val height = createVerticalMeasurement(bottomLeft, point, "height")
                    _doorMeasurement.value = _doorMeasurement.value.copy(height = height)
                    _currentStep.value = MeasurementStep.Complete
                    _isComplete.value = true
                }
            }

            // Space measurement (6 taps)
            MeasurementStep.SpaceWidthLeft -> {
                spaceWidthLeftPoint = point
                _currentStep.value = MeasurementStep.SpaceWidthRight
            }
            MeasurementStep.SpaceWidthRight -> {
                spaceWidthLeftPoint?.let { leftPoint ->
                    val width = createHorizontalMeasurement(leftPoint, point, "width")
                    _spaceMeasurement.value = _spaceMeasurement.value.copy(width = width)
                    _currentStep.value = MeasurementStep.SpaceDepthFront
                }
            }
            MeasurementStep.SpaceDepthFront -> {
                spaceDepthFrontPoint = point
                _currentStep.value = MeasurementStep.SpaceDepthBack
            }
            MeasurementStep.SpaceDepthBack -> {
                spaceDepthFrontPoint?.let { frontPoint ->
                    val depth = createHorizontalMeasurement(frontPoint, point, "depth")
                    _spaceMeasurement.value = _spaceMeasurement.value.copy(depth = depth)
                    _currentStep.value = MeasurementStep.SpaceHeightFloor
                }
            }
            MeasurementStep.SpaceHeightFloor -> {
                spaceHeightFloorPoint = point
                _currentStep.value = MeasurementStep.SpaceHeightCeiling
            }
            MeasurementStep.SpaceHeightCeiling -> {
                spaceHeightFloorPoint?.let { floorPoint ->
                    val height = createVerticalMeasurement(floorPoint, point, "height")
                    _spaceMeasurement.value = _spaceMeasurement.value.copy(height = height)
                    _currentStep.value = MeasurementStep.Complete
                    _isComplete.value = true
                }
            }

            MeasurementStep.Complete -> {
                // Already complete, ignore
            }
        }
    }

    private fun createHorizontalMeasurement(
        from: MeasurementPoint,
        to: MeasurementPoint,
        dimension: String
    ): MeasurementResult {
        // Horizontal distance (X-Z plane only, ignoring Y)
        val dx = to.position[0] - from.position[0]
        val dz = to.position[2] - from.position[2]
        val distanceM = sqrt((dx * dx + dz * dz).toDouble())
        val distanceCm = distanceM * 100.0

        return createMeasurementResult(dimension, distanceCm, from, to)
    }

    private fun createVerticalMeasurement(
        from: MeasurementPoint,
        to: MeasurementPoint,
        dimension: String
    ): MeasurementResult {
        // Vertical distance (Y axis only)
        val dy = abs(to.position[1] - from.position[1])
        val distanceCm = dy * 100.0

        return createMeasurementResult(dimension, distanceCm, from, to)
    }

    private fun createMeasurementResult(
        dimension: String,
        distanceCm: Double,
        from: MeasurementPoint,
        to: MeasurementPoint
    ): MeasurementResult {
        // Combine hit types - use worst (lowest confidence)
        val hitType = if (from.hitType.confidenceMultiplier < to.hitType.confidenceMultiplier) {
            from.hitType
        } else {
            to.hitType
        }

        // Calculate confidence score
        val trackingFactor = (from.trackingConfidence + to.trackingConfidence) / 2.0
        val hitTypeFactor = hitType.confidenceMultiplier
        val stabilityFactor = calculateStabilityFactor(from.stabilitySpreadCm, to.stabilitySpreadCm)

        val confidenceScore = (trackingFactor * hitTypeFactor * stabilityFactor).coerceIn(0.0, 1.0)

        // Calculate uncertainty
        val baseUncertainty = BASE_UNCERTAINTY_CM
        val stabilityUncertainty = (from.stabilitySpreadCm + to.stabilitySpreadCm) / 2.0
        val hitTypeUncertainty = hitType.uncertaintyAddition
        val totalUncertainty = baseUncertainty + stabilityUncertainty + hitTypeUncertainty

        return MeasurementResult(
            dimension = dimension,
            valueCm = distanceCm,
            uncertaintyCm = totalUncertainty,
            confidenceScore = confidenceScore,
            hitType = hitType
        )
    }

    private fun calculateStabilityFactor(spread1: Double, spread2: Double): Double {
        val avgSpread = (spread1 + spread2) / 2.0
        return when {
            avgSpread < 0.5 -> 1.0
            avgSpread < 1.0 -> 0.9
            avgSpread < 1.5 -> 0.8
            else -> 0.7
        }
    }

    fun retryCurrentPoint() {
        // Just stay on current step, user can tap again
    }

    fun reset() {
        _currentStep.value = MeasurementStep.DoorBottomLeft
        _doorMeasurement.value = DoorMeasurement()
        _spaceMeasurement.value = SpaceMeasurement()
        _isComplete.value = false
        _yConsistencyError.value = null

        doorBottomLeftPoint = null
        doorBottomRightPoint = null
        spaceWidthLeftPoint = null
        spaceDepthFrontPoint = null
        spaceHeightFloorPoint = null
    }

    fun clearYConsistencyError() {
        _yConsistencyError.value = null
    }

    fun getCurrentMode(): MeasurementMode = currentMode
}
