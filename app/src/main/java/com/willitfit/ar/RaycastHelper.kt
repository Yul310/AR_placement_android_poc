package com.willitfit.ar

import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.DepthPoint
import com.google.ar.core.Point
import com.google.ar.core.Trackable
import com.willitfit.data.model.PlaneAlignment
import com.willitfit.data.model.RaycastHitType

class RaycastHelper {

    /**
     * Performs a priority-based hit test following the same logic as iOS:
     * 1. Existing plane geometry (highest priority)
     * 2. Depth points
     * 3. Estimated plane / feature points (lowest priority)
     *
     * @param strictPlaneOnly If true, only returns hits on actual planes (no fallback to depth/feature points)
     */
    fun performPriorityHitTest(
        frame: Frame,
        x: Float,
        y: Float,
        alignment: PlaneAlignment,
        strictPlaneOnly: Boolean = false
    ): HitResult? {
        val hitResults = frame.hitTest(x, y)

        if (hitResults.isEmpty()) return null

        // Filter by alignment preference
        val filteredResults = hitResults.filter { hitResult ->
            matchesAlignment(hitResult.trackable, alignment)
        }

        if (filteredResults.isEmpty()) {
            // If strict mode, don't fall back
            if (strictPlaneOnly) return null
            // Fall back to any hit if no alignment match
            return hitResults.firstOrNull()
        }

        // Priority 1: Existing plane with geometry (required for strict mode)
        filteredResults.firstOrNull { hitResult ->
            val trackable = hitResult.trackable
            trackable is Plane && trackable.isPoseInPolygon(hitResult.hitPose)
        }?.let { return it }

        // If strict plane mode, don't fall back to weaker hits
        if (strictPlaneOnly) return null

        // Priority 2: Depth points (requires depth API)
        filteredResults.firstOrNull { hitResult ->
            hitResult.trackable is DepthPoint
        }?.let { return it }

        // Priority 3: Feature points or any plane
        filteredResults.firstOrNull { hitResult ->
            hitResult.trackable is Point || hitResult.trackable is Plane
        }?.let { return it }

        // Return first available
        return filteredResults.firstOrNull()
    }

    /**
     * Strict plane-only hit test for measurement steps that require plane detection.
     * Returns null if no actual plane is detected.
     */
    fun performStrictPlaneHitTest(
        frame: Frame,
        x: Float,
        y: Float,
        alignment: PlaneAlignment
    ): HitResult? {
        return performPriorityHitTest(frame, x, y, alignment, strictPlaneOnly = true)
    }

    private fun matchesAlignment(trackable: Trackable, alignment: PlaneAlignment): Boolean {
        return when (alignment) {
            PlaneAlignment.ALL -> true
            PlaneAlignment.HORIZONTAL -> {
                when (trackable) {
                    is Plane -> trackable.type == Plane.Type.HORIZONTAL_UPWARD_FACING ||
                            trackable.type == Plane.Type.HORIZONTAL_DOWNWARD_FACING
                    else -> true // Allow non-plane hits
                }
            }
            PlaneAlignment.VERTICAL -> {
                when (trackable) {
                    is Plane -> trackable.type == Plane.Type.VERTICAL
                    else -> true // Allow non-plane hits
                }
            }
        }
    }

    fun getHitResultType(hitResult: HitResult): RaycastHitType {
        val trackable = hitResult.trackable

        return when {
            // Existing plane geometry
            trackable is Plane && trackable.isPoseInPolygon(hitResult.hitPose) -> {
                RaycastHitType.EXISTING_PLANE
            }
            // Depth point
            trackable is DepthPoint -> {
                RaycastHitType.DEPTH_POINT
            }
            // Plane but not in polygon (estimated)
            trackable is Plane -> {
                RaycastHitType.ESTIMATED_SURFACE
            }
            // Feature point
            trackable is Point -> {
                RaycastHitType.DEPTH_POINT
            }
            // Default fallback
            else -> {
                RaycastHitType.ESTIMATED_SURFACE
            }
        }
    }
}
