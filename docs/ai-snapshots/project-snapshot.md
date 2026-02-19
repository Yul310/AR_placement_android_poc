# Project Snapshot

## What this app does (plain English)

- **Helps you check if furniture or appliances will fit** through a doorway or into a room before you buy or move them
- **Uses your phone's camera with AR** (Augmented Reality - technology that overlays digital information onto the real world through your camera) to measure real spaces
- **Shows a clear verdict**: "WILL FIT", "WON'T FIT", or "NOT SURE" based on your measurements
- **3D box with wireframe edges** for better visibility in virtual placement mode
- **Includes pre-loaded products** like refrigerators, sofas, TVs, washers, and beds with their exact dimensions
- **Offers three measurement modes**: measure a door opening, measure a room/alcove space, or place a virtual 3D box to visualize the product
- **Provides confidence ratings** (High/Medium/Low) so you know how reliable the measurement is
- **Accounts for safety margins** - adds a few centimeters of buffer to avoid tight fits
- **Validates measurement quality** - requires detected planes for floor points, checks Y-consistency between bottom corners
- **Monitors tracking state** - aborts sampling if AR tracking is lost during measurement

**Who uses it**: Anyone moving furniture, buying large appliances, or checking if items will fit through doorways or into specific spaces.

## Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose (modern Android UI toolkit) |
| AR Framework | ARCore + Sceneform (Google's AR libraries) |
| Architecture | MVVM with StateFlow |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines + Flow |
| JSON Parsing | Gson |
| Image Loading | Coil |
| Min Android | 7.0 (API 24) |
| Target Android | 14 (API 34) |

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────────┐
│                         UI Layer                                 │
│  ┌──────────┐  ┌───────────────┐  ┌───────────┐  ┌────────────┐ │
│  │HomeScreen│  │ARMeasurement- │  │VirtualPlac│  │ResultScreen│ │
│  │          │  │Screen         │  │ementScreen│  │            │ │
│  └────┬─────┘  └───────┬───────┘  └─────┬─────┘  └─────┬──────┘ │
│       │                │                │              │         │
│  ┌────▼─────┐  ┌───────▼───────┐  ┌─────▼─────┐        │         │
│  │HomeView- │  │Measurement-   │  │Placement- │        │         │
│  │Model     │  │ViewModel      │  │ViewModel  │        │         │
│  └──────────┘  └───────────────┘  └───────────┘        │         │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                       Service Layer                              │
│  ┌─────────────────────────┐   ┌──────────────────────────────┐ │
│  │MeasurementFlowController│   │VerdictEngine                 │ │
│  │(state machine for taps) │   │(pass/fail logic with margins)│ │
│  └─────────────────────────┘   └──────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                         AR Layer                                 │
│  ┌─────────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ARSessionManager │  │RaycastHelper│  │SamplingManager      │  │
│  │(camera + ARCore)│  │(hit testing)│  │(0.75s median filter)│  │
│  └─────────────────┘  └─────────────┘  └─────────────────────┘  │
│                            ┌──────────────┐                      │
│                            │SceneManager  │                      │
│                            │(3D box render│                      │
│                            └──────────────┘                      │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────────┐
│                        Data Layer                                │
│  ┌─────────────────────┐   ┌──────────────────────────────────┐ │
│  │ProductRepository    │   │products.json (17 preset products)│ │
│  │(loads from JSON)    │   │                                  │ │
│  └─────────────────────┘   └──────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

### Data Storage

- **Local JSON file** (`products.json`) bundled in app assets with 17 preset products
- **No database** - all data is in-memory
- **No user data persistence** - measurements are not saved between sessions

### Integrations

- **ARCore** - Google's AR platform for plane detection and depth sensing
- **Sceneform** - 3D rendering library for placing virtual objects
- **Camera** - Required for AR functionality

## Main flow (one end-to-end)

**User measures a door to check if a refrigerator will fit:**

1. **HomeScreen** → User opens app, sees product list grouped by category
2. User taps "French Door Refrigerator" (91 x 178 x 74 cm)
3. **ModeSelectionSheet** → Bottom sheet appears with 3 options; user selects "Door"
4. **Navigation** → App navigates to `ARMeasurementScreen`
5. **Camera permission** → App requests camera access
6. **ARSessionManager.initialize()** → Starts ARCore session with plane detection
7. **MeasurementFlowController.startFlow(DOOR)** → Sets first step: "Tap BOTTOM-LEFT corner"
8. User taps screen → **SamplingManager** collects 0.75 seconds of samples
9. **Median filter** calculates stable 3D position with confidence score
10. **Step 2**: "Tap BOTTOM-RIGHT corner" → calculates horizontal distance (width)
11. **Step 3**: "Tap TOP-LEFT corner" → calculates vertical distance (height)
12. **MeasurementFlowController** marks `isComplete = true`
13. **VerdictEngine.verdictForDoor()** runs:
    - Compares door width (measured) vs product width (91cm)
    - Compares door height (measured) vs product height (178cm)
    - Applies 3cm safety margin + measurement uncertainty
    - Returns: `Verdict.Pass`, `Verdict.Fail`, or `Verdict.NotSure`
14. **ResultScreen** displays verdict with color-coded icon and detailed measurements

## Accuracy Safeguards

The app implements several safeguards to ensure measurement accuracy:

### Plane-First Gating
- Floor measurements (door bottom corners, space floor) **require** hitting a detected plane
- If user taps before a floor plane is detected, sampling fails with "No floor plane detected" message
- Prevents inaccurate measurements from estimated surfaces

### Y-Consistency Check
- After capturing both door bottom corners, validates they are at the same height
- If Y-difference exceeds 3cm, measurement is rejected
- User must re-tap both corners on a flat floor

### Tracking State Monitoring
- During 0.75s sampling, monitors ARCore tracking state each frame
- If tracking is lost for 10+ consecutive frames, sampling aborts
- Prevents drift from corrupting measurements

### Conservative Confidence Scoring
- Pass confidence threshold raised from 0.6 to **0.7**
- If measurement used only estimated surfaces (no real plane detected), returns "NOT SURE" regardless of confidence
- Better to say "NOT SURE" than give a false positive

### Virtual Placement Rules
| Product Type | Wall Placement | Floor Placement |
|--------------|----------------|-----------------|
| TV (`canMountOnWall: true`) | ✅ Allowed | ✅ Allowed |
| Appliances (refrigerator, washer, etc.) | ❌ Rejected | ✅ Allowed |

## Key folders/files (entry points)

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Single activity that hosts the Compose UI |
| `NavGraph.kt` | Navigation routes and shared state between screens |
| `HomeScreen.kt` | Product selection list |
| `ARMeasurementScreen.kt` | Main AR camera view with tap handling |
| `VerdictEngine.kt` | Core logic for pass/fail decisions |
| `MeasurementFlowController.kt` | State machine for 3-tap (door) or 6-tap (space) flow |
| `ARSessionManager.kt` | ARCore session lifecycle and raycast management |
| `products.json` | Preset product dimensions (appliances, furniture, TVs) |

## How to run / test / build

### Build
```bash
./gradlew assembleDebug
```

### Run
1. Open project in Android Studio
2. Sync Gradle files
3. Connect an ARCore-supported Android device (emulator won't work for AR)
4. Run the app

### Test
```bash
./gradlew test              # Unit tests
./gradlew connectedAndroidTest  # Instrumentation tests
```

### Requirements
- Android 7.0+ (API 24)
- ARCore-supported device (camera required)
- Camera permission granted

## Risks / TODOs

1. **No data persistence** - Measurements are lost when leaving the result screen; users cannot save or review past measurements
2. **Fixed product catalog** - Users cannot add custom products with their own dimensions
3. **No offline product images** - `image_url` fields are null for all products; product cards have no visual preview
4. **Sceneform deprecated** - Google deprecated Sceneform in 2020; may need migration to SceneView or ARCore Extensions
5. **No unit tests visible** - No test files found in the project; verdict logic and measurement calculations are untested
6. **ProGuard disabled** - `isMinifyEnabled = false` in release build; APK size not optimized
7. **No error analytics** - AR failures silently fail with local error messages; no crash/error reporting
8. **Single activity, no deep links** - Cannot share or bookmark specific measurements

## Questions to confirm (max 5)

1. **Product persistence**: Should users be able to add their own products with custom dimensions?
2. **Measurement history**: Should measurements be saved locally so users can review past results?
3. **Sceneform migration**: Is there a plan to migrate from deprecated Sceneform to a supported library?
4. **Product images**: Will product images be added? If so, where will they be hosted?
5. **Analytics**: Should crash reporting or usage analytics be integrated?
