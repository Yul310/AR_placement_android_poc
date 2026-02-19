package com.willitfit.service

import com.willitfit.data.model.DoorMeasurement
import com.willitfit.data.model.MeasurementResult
import com.willitfit.data.model.Product
import com.willitfit.data.model.RaycastHitType
import com.willitfit.data.model.SpaceMeasurement
import com.willitfit.data.model.Verdict
import kotlin.math.abs
import kotlin.math.min

object VerdictEngine {

    private const val SAFETY_MARGIN_CM = 3.0
    // Raised from 0.6 to 0.7 for more conservative PASS
    private const val MIN_PASS_CONFIDENCE = 0.7

    /**
     * Check if measurement used only estimated surface (no real plane detection)
     */
    private fun usedOnlyEstimatedSurface(measurement: MeasurementResult): Boolean {
        return measurement.hitType == RaycastHitType.ESTIMATED_SURFACE
    }

    fun verdictForDoor(door: DoorMeasurement, product: Product): Verdict {
        val width = door.width ?: return Verdict.NotSure("Incomplete measurement - missing width")
        val height = door.height ?: return Verdict.NotSure("Incomplete measurement - missing height")

        // Check if either measurement used only estimated surface
        if (usedOnlyEstimatedSurface(width) || usedOnlyEstimatedSurface(height)) {
            return Verdict.NotSure("Measurement used estimated surface (no plane detected). Scan the floor/door frame more slowly and try again.")
        }

        val overallConfidence = minOf(width.confidenceScore, height.confidenceScore)

        // Effective width (allow rotation for TVs and similar items)
        val effectiveProductWidth = if (product.allowRotate) {
            minOf(product.widthCm, product.depthCm)
        } else {
            product.widthCm
        }

        // Calculate clearances
        val widthClearance = width.valueCm - effectiveProductWidth
        val heightClearance = height.valueCm - product.heightCm

        // Check for hard failures
        if (widthClearance < 0) {
            return Verdict.Fail(
                "Product width (${effectiveProductWidth.toInt()}cm) exceeds door by ${abs(widthClearance).toInt()}cm"
            )
        }
        if (heightClearance < 0) {
            return Verdict.Fail(
                "Product height (${product.heightCm.toInt()}cm) exceeds door by ${abs(heightClearance).toInt()}cm"
            )
        }

        // Check confidence threshold
        if (overallConfidence < MIN_PASS_CONFIDENCE) {
            return Verdict.NotSure("Low confidence measurement. Try again with better lighting.")
        }

        // Check margins including uncertainty
        val widthMarginNeeded = SAFETY_MARGIN_CM + width.uncertaintyCm
        val heightMarginNeeded = SAFETY_MARGIN_CM + height.uncertaintyCm

        if (widthClearance >= widthMarginNeeded && heightClearance >= heightMarginNeeded) {
            val minClearance = minOf(widthClearance, heightClearance).toInt()
            return Verdict.Pass("Product fits with ${minClearance}cm clearance")
        }

        // Tight fit scenario
        return Verdict.NotSure("Tight fit - measure with tape measure to confirm.")
    }

    fun verdictForSpace(space: SpaceMeasurement, product: Product): Verdict {
        val width = space.width ?: return Verdict.NotSure("Incomplete measurement - missing width")
        val depth = space.depth ?: return Verdict.NotSure("Incomplete measurement - missing depth")
        val height = space.height ?: return Verdict.NotSure("Incomplete measurement - missing height")

        // Check if any critical measurement used only estimated surface
        // Note: ceiling is allowed to use estimated surface since it's hard to detect
        if (usedOnlyEstimatedSurface(width) || usedOnlyEstimatedSurface(depth)) {
            return Verdict.NotSure("Wall measurement used estimated surface. Scan the walls more slowly and try again.")
        }

        val overallConfidence = minOf(width.confidenceScore, depth.confidenceScore, height.confidenceScore)

        // Calculate clearances
        val widthClearance = width.valueCm - product.widthCm
        val depthClearance = depth.valueCm - product.depthCm
        val heightClearance = height.valueCm - product.heightCm

        // Check for hard failures
        if (widthClearance < 0) {
            return Verdict.Fail(
                "Product width (${product.widthCm.toInt()}cm) exceeds space by ${abs(widthClearance).toInt()}cm"
            )
        }
        if (depthClearance < 0) {
            return Verdict.Fail(
                "Product depth (${product.depthCm.toInt()}cm) exceeds space by ${abs(depthClearance).toInt()}cm"
            )
        }
        if (heightClearance < 0) {
            return Verdict.Fail(
                "Product height (${product.heightCm.toInt()}cm) exceeds space by ${abs(heightClearance).toInt()}cm"
            )
        }

        // Check confidence threshold
        if (overallConfidence < MIN_PASS_CONFIDENCE) {
            return Verdict.NotSure("Low confidence measurement. Try again with better lighting.")
        }

        // Check margins including uncertainty
        val widthMarginNeeded = SAFETY_MARGIN_CM + width.uncertaintyCm
        val depthMarginNeeded = SAFETY_MARGIN_CM + depth.uncertaintyCm
        val heightMarginNeeded = SAFETY_MARGIN_CM + height.uncertaintyCm

        if (widthClearance >= widthMarginNeeded &&
            depthClearance >= depthMarginNeeded &&
            heightClearance >= heightMarginNeeded) {
            val minClearance = minOf(widthClearance, depthClearance, heightClearance).toInt()
            return Verdict.Pass("Product fits with ${minClearance}cm clearance")
        }

        // Tight fit scenario
        return Verdict.NotSure("Tight fit - measure with tape measure to confirm.")
    }

    /**
     * Check if rotating the product (swapping width/depth) would help fit.
     */
    fun suggestRotation(door: DoorMeasurement, product: Product): Boolean {
        if (!product.allowRotate) return false

        val width = door.width ?: return false
        val height = door.height ?: return false

        val normalWidthClearance = width.valueCm - product.widthCm
        val rotatedWidthClearance = width.valueCm - product.depthCm

        // Suggest rotation if rotated fits better
        return normalWidthClearance < 0 && rotatedWidthClearance >= SAFETY_MARGIN_CM
    }
}
