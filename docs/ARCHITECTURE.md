# Architecture & Technical Documentation

This document provides detailed technical information about the WillItFit Android app's architecture, measurement process, and accuracy safeguards.

## Table of Contents

- [Project Structure](#project-structure)
- [Three Modes](#three-modes)
- [Measurement Process](#measurement-process)
- [Accuracy Safeguards](#accuracy-safeguards)
- [Virtual Placement](#virtual-placement)
- [Verdict Logic](#verdict-logic)
- [Adding Custom Products](#adding-custom-products)

---

## Project Structure

```
app/src/main/java/com/willitfit/
├── WillItFitApp.kt                    # Application class
├── MainActivity.kt                     # Single activity host
│
├── data/
│   ├── model/
│   │   ├── Product.kt                  # Product data class
│   │   ├── Measurement.kt              # MeasurementResult, steps, hit types
│   │   └── Verdict.kt                  # Verdict sealed class
│   └── repository/
│       └── ProductRepository.kt        # Load products from JSON
│
├── ui/
│   ├── theme/                          # Material3 theme
│   ├── navigation/NavGraph.kt          # Navigation routes
│   ├── home/                           # Product selection
│   ├── measurement/                    # AR measurement screens
│   ├── placement/                      # Virtual placement
│   └── result/                         # Verdict display
│
├── ar/
│   ├── ARSessionManager.kt             # ARCore session management
│   ├── RaycastHelper.kt                # Priority raycasting with strict mode
│   ├── SamplingManager.kt              # 0.75s point sampling with tracking monitoring
│   └── SceneManager.kt                 # 3D entity management
│
├── service/
│   ├── MeasurementFlowController.kt    # Step orchestration with Y-consistency
│   └── VerdictEngine.kt                # PASS/FAIL/NOT_SURE logic
│
└── util/
    ├── MathHelpers.kt                  # Vector math, distance calculations
    └── ScreenshotHelper.kt             # Capture AR view
```

---

## Three Modes

| Mode | Description | Taps Required |
|------|-------------|---------------|
| **Door** | Measure doorway width & height | 3 taps (corner-based) |
| **Space** | Measure room width, depth & height | 6 taps (2 per dimension) |
| **Virtual Placement** | Place a 3D semi-transparent box in AR | 1 tap to place, then drag/rotate |

---

## Measurement Process

### Door Mode (3 taps - Corner-Based)

User taps **3 corners** of the door frame, each point auto-sampled for 0.75s:

| Step | Point to Tap | Plane Required | What It Measures |
|------|--------------|----------------|------------------|
| 1 | BOTTOM-LEFT corner | Yes (horizontal) | Reference point |
| 2 | BOTTOM-RIGHT corner | Yes (horizontal) | Width (horizontal distance from tap 1) |
| 3 | TOP-LEFT corner | No (vertical OK) | Height (vertical distance from tap 1) |

```
        ┌───────┐
        │       │
tap 3 → •       │   Height = tap 1 to tap 3 (Y-axis only)
        │       │
        └───────┘
      tap 1 ─── tap 2
         Width (X-Z plane only)
```

**Accuracy Safeguards:**
- **Plane-First Gating** — Bottom points (tap 1 & 2) require hitting an actual detected floor plane; estimated surfaces are rejected
- **Y-Consistency Check** — If the two bottom points differ in height by more than 3cm, measurement is rejected

### Space Mode (6 taps)

| Step | Point to Tap | Plane Required | Measurement |
|------|--------------|----------------|-------------|
| 1 | LEFT wall | Yes (vertical) | Width (start) |
| 2 | RIGHT wall | Yes (vertical) | Width (end) |
| 3 | FRONT wall | Yes (vertical) | Depth (start) |
| 4 | BACK wall | Yes (vertical) | Depth (end) |
| 5 | FLOOR | Yes (horizontal) | Height (start) |
| 6 | CEILING | No (estimated OK) | Height (end) |

### Auto-Smoothing (0.75s Sampling)

Each tap triggers a **0.75-second sampling window**:

1. **Plane Requirement Check** — For floor/wall points, verifies a real plane is detected before starting
2. **Continuous Raycasting** — Samples at ~60fps (~45 samples)
3. **Tracking Monitoring** — Aborts if tracking is lost for 10+ consecutive frames
4. **Median Filtering** — Uses median per axis (X, Y, Z) to filter jitter
5. **Stability Check** — Fails if samples spread > 1.5cm

```kotlin
// From SamplingManager
companion object {
    const val SAMPLING_DURATION_MS = 750L
    const val SAMPLE_INTERVAL_MS = 16L  // ~60fps
    const val MIN_SAMPLES = 10
    const val MAX_STABILITY_SPREAD_CM = 1.5
    const val MAX_BAD_TRACKING_FRAMES = 10
}
```

### Confidence Scoring

Each measurement receives a confidence score (0.0 - 1.0) based on:

| Factor | Impact |
|--------|--------|
| **Tracking State** | TRACKING = 1.0, PAUSED = 0.4, STOPPED = 0.0 |
| **Raycast Hit Type** | Existing Plane = 1.0, Depth Point = 0.85, Estimated = 0.6 |
| **Stability** | Spread > 1cm = 0.7×, Spread > 0.5cm = 0.8× |

**Confidence Levels:**
- **High** (≥0.8) — Green badge, full confidence
- **Medium** (≥0.7) — Orange badge, acceptable
- **Low** (<0.7) — Red badge, triggers "NOT SURE" verdict

> **Note:** Measurements that use only estimated surfaces (no actual plane detected) will always return "NOT SURE" regardless of confidence score.

---

## Accuracy Safeguards

### Plane-First Gating

Critical measurement points **require** hitting a detected plane:

```kotlin
// From RaycastHelper
fun performStrictPlaneHitTest(
    frame: Frame,
    x: Float,
    y: Float,
    alignment: PlaneAlignment
): HitResult? {
    return performPriorityHitTest(frame, x, y, alignment, strictPlaneOnly = true)
}
```

If no plane is detected, sampling fails with a helpful message like "No floor plane detected. Scan the floor more slowly."

### Y-Consistency Check

Both door bottom corners must be at the same height (within 3cm):

```kotlin
// From MeasurementFlowController
private const val Y_CONSISTENCY_THRESHOLD_METERS = 0.03f  // 3cm

// After capturing bottom-right point:
val yDifference = abs(bottomLeft.position[1] - point.position[1])
if (yDifference > Y_CONSISTENCY_THRESHOLD_METERS) {
    val yDiffCm = (yDifference * 100).toInt()
    _yConsistencyError.value = "Bottom points not level (${yDiffCm}cm difference). Re-tap both corners."
    // Reset to beginning of door measurement
}
```

### Tracking State Monitoring

Sampling aborts if AR tracking is lost:

```kotlin
// From SamplingManager
private var badTrackingFrameCount = 0

private fun collectSample(...) {
    // Check tracking state
    val trackingState = arSessionManager.getTrackingState()
    if (trackingState != TrackingState.TRACKING) {
        badTrackingFrameCount++
        if (badTrackingFrameCount >= MAX_BAD_TRACKING_FRAMES) {
            onFailed?.invoke("Tracking lost during sampling. Hold phone steadier.")
            return
        }
    } else {
        badTrackingFrameCount = 0
    }
    // ... collect sample
}
```

---

## Virtual Placement

### Box Appearance

The 3D product box features:
- Semi-transparent blue fill (30% opacity) for volume visualization
- White wireframe edges (12 edges) for better shape definition
- Dimensions match actual product size in meters

### Placement Rules

Products have placement restrictions based on their category:

| Product Type | Wall | Floor |
|--------------|------|-------|
| TV (`canMountOnWall: true`) | ✅ Allowed | ✅ Allowed |
| Appliances (refrigerator, washer, etc.) | ❌ Rejected | ✅ Allowed |

```kotlin
// From PlacementViewModel
fun handleTap(screenX: Float, screenY: Float) {
    // ...
    val isVerticalHit = isVerticalPlaneHit(hitResult)

    // Non-TV products cannot be placed on walls
    if (!currentProduct.canMountOnWall && isVerticalHit) {
        return  // Ignore tap
    }
    // ...
}
```

### Gestures

- **Tap** — Place product box (first tap) or move to new position (subsequent taps)
- **Rotation Buttons** — 45° left/right rotation

---

## Verdict Logic

### Safety Margins

```kotlin
// From VerdictEngine
private const val SAFETY_MARGIN_CM = 3.0
private const val MIN_PASS_CONFIDENCE = 0.7  // Raised from 0.6

// Total margin includes measurement uncertainty
val widthMarginNeeded = SAFETY_MARGIN_CM + width.uncertaintyCm
val heightMarginNeeded = SAFETY_MARGIN_CM + height.uncertaintyCm
```

### Verdict Outcomes

| Verdict | Condition |
|---------|-----------|
| **WILL FIT** | Clearance ≥ margin needed AND confidence ≥ 0.7 AND used detected plane |
| **WON'T FIT** | Product dimension > measured dimension (clear fail) |
| **NOT SURE** | Confidence < 0.7, OR clearance positive but < margin, OR used only estimated surfaces |

### Estimated Surface Check

```kotlin
private fun usedOnlyEstimatedSurface(measurement: MeasurementResult): Boolean {
    return measurement.hitType == RaycastHitType.ESTIMATED_SURFACE
}

fun verdictForDoor(door: DoorMeasurement, product: Product): Verdict {
    // ...
    // Check if either measurement used only estimated surface
    if (usedOnlyEstimatedSurface(width) || usedOnlyEstimatedSurface(height)) {
        return Verdict.NotSure("Measurement used estimated surface. Scan more slowly and try again.")
    }
    // ...
}
```

---

## Adding Custom Products

Edit `app/src/main/assets/products.json`:

```json
{
  "products": [
    {
      "id": "UNIQUE-SKU",
      "name": "Product Display Name",
      "category": "Refrigerator",
      "widthCm": 90.0,
      "heightCm": 180.0,
      "depthCm": 70.0,
      "allowRotate": false,
      "imageUrl": null
    }
  ]
}
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Unique product identifier (SKU) |
| `name` | String | Display name |
| `category` | String | Category (Refrigerator, Washer, TV, Oven, etc.) |
| `widthCm` | Double | Width in centimeters |
| `heightCm` | Double | Height in centimeters |
| `depthCm` | Double | Depth in centimeters |
| `allowRotate` | Boolean | Can rotate 90° through doorway |
| `imageUrl` | String? | Product image URL (optional) |

### Categories with Special Behavior

| Category | Special Behavior |
|----------|------------------|
| `TV` | Can be placed on walls in Virtual Placement mode |
| Others | Floor placement only |
