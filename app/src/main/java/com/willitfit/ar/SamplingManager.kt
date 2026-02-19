package com.willitfit.ar

import com.google.ar.core.TrackingState
import com.willitfit.data.model.PlaneAlignment
import com.willitfit.data.model.RaycastHitType
import com.willitfit.data.model.SamplingResult
import com.willitfit.util.MathHelpers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SamplingManager(
    private val arSessionManager: ARSessionManager,
    private val scope: CoroutineScope
) {
    companion object {
        const val SAMPLING_DURATION_MS = 750L
        const val SAMPLE_INTERVAL_MS = 16L  // ~60fps
        const val MIN_SAMPLES = 10
        const val MAX_STABILITY_SPREAD_CM = 1.5
        const val MAX_BAD_TRACKING_FRAMES = 10
    }

    private data class PointSample(
        val position: FloatArray,
        val hitType: RaycastHitType,
        val timestamp: Long
    )

    private val samples = mutableListOf<PointSample>()
    private var samplingJob: Job? = null
    private var requirePlaneHit: Boolean = false
    private var badTrackingFrameCount: Int = 0
    private var currentAlignment: PlaneAlignment = PlaneAlignment.ALL

    var onComplete: ((SamplingResult) -> Unit)? = null
    var onFailed: ((String) -> Unit)? = null
    var onProgress: ((Double) -> Unit)? = null

    fun startSampling(screenX: Float, screenY: Float, alignment: PlaneAlignment, requirePlane: Boolean = false) {
        cancel()
        samples.clear()
        requirePlaneHit = requirePlane
        badTrackingFrameCount = 0
        currentAlignment = alignment

        // Do initial check for plane hit if required
        if (requirePlane) {
            val initialHit = arSessionManager.performStrictPlaneHitTest(screenX, screenY, alignment)
            if (initialHit == null) {
                val surfaceType = if (alignment == PlaneAlignment.HORIZONTAL) "floor" else "wall"
                onFailed?.invoke("No $surfaceType plane detected. Scan the $surfaceType slowly, then tap again.")
                return
            }
        }

        samplingJob = scope.launch {
            val startTime = System.currentTimeMillis()

            while (isActive && (System.currentTimeMillis() - startTime) < SAMPLING_DURATION_MS) {
                val shouldContinue = collectSample(screenX, screenY, alignment)
                if (!shouldContinue) {
                    return@launch
                }

                val progress = (System.currentTimeMillis() - startTime).toDouble() / SAMPLING_DURATION_MS
                onProgress?.invoke(progress.coerceIn(0.0, 1.0))

                delay(SAMPLE_INTERVAL_MS)
            }

            if (isActive) {
                finishSampling()
            }
        }
    }

    /**
     * Returns false if sampling should be aborted
     */
    private fun collectSample(screenX: Float, screenY: Float, alignment: PlaneAlignment): Boolean {
        // Check tracking state
        val trackingState = arSessionManager.trackingState.value
        if (trackingState != TrackingState.TRACKING) {
            badTrackingFrameCount++
            if (badTrackingFrameCount > MAX_BAD_TRACKING_FRAMES) {
                onFailed?.invoke("Tracking lost. Hold phone steadier and ensure good lighting.")
                return false
            }
            // Skip this sample but continue
            return true
        }

        // Reset bad frame counter on good tracking
        badTrackingFrameCount = 0

        // Get hit result (strict or priority based on requirements)
        val hitResult = if (requirePlaneHit) {
            arSessionManager.performStrictPlaneHitTest(screenX, screenY, alignment)
        } else {
            arSessionManager.performPriorityHitTest(screenX, screenY, alignment)
        }

        if (hitResult == null) {
            // If we require plane and didn't get one, skip but continue
            return true
        }

        val hitType = arSessionManager.getHitResultType(hitResult)

        // If we require plane hits, skip estimated surface hits
        if (requirePlaneHit && hitType == RaycastHitType.ESTIMATED_SURFACE) {
            return true
        }

        val pose = hitResult.hitPose
        val position = floatArrayOf(
            pose.tx(),
            pose.ty(),
            pose.tz()
        )

        samples.add(
            PointSample(
                position = position,
                hitType = hitType,
                timestamp = System.currentTimeMillis()
            )
        )

        return true
    }

    private fun finishSampling() {
        if (samples.size < MIN_SAMPLES) {
            if (requirePlaneHit) {
                val surfaceType = if (currentAlignment == PlaneAlignment.HORIZONTAL) "floor" else "wall"
                onFailed?.invoke("Could not detect $surfaceType plane reliably. Scan the $surfaceType more slowly.")
            } else {
                onFailed?.invoke("Not enough samples (${samples.size}/$MIN_SAMPLES). Try better lighting.")
            }
            return
        }

        // Calculate median position per axis
        val xValues = samples.map { it.position[0] }.sorted()
        val yValues = samples.map { it.position[1] }.sorted()
        val zValues = samples.map { it.position[2] }.sorted()

        val medianX = MathHelpers.median(xValues)
        val medianY = MathHelpers.median(yValues)
        val medianZ = MathHelpers.median(zValues)

        val medianPosition = floatArrayOf(medianX, medianY, medianZ)

        // Calculate stability spread (max distance from median)
        val maxSpread = samples.maxOf { sample ->
            MathHelpers.distance3D(sample.position, medianPosition) * 100.0 // Convert to cm
        }

        if (maxSpread > MAX_STABILITY_SPREAD_CM) {
            onFailed?.invoke("Position unstable (+/- ${String.format("%.1f", maxSpread)}cm). Hold steadier.")
            return
        }

        // Determine primary hit type (most common)
        val primaryHitType = samples
            .groupBy { it.hitType }
            .maxByOrNull { it.value.size }
            ?.key ?: RaycastHitType.ESTIMATED_SURFACE

        onComplete?.invoke(
            SamplingResult(
                position = medianPosition,
                stabilitySpreadCm = maxSpread,
                sampleCount = samples.size,
                primaryHitType = primaryHitType
            )
        )
    }

    fun cancel() {
        samplingJob?.cancel()
        samplingJob = null
    }
}
