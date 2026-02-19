# WillItFit - Android

AR-based measurement app to determine if furniture/appliances will fit through doorways or in spaces.

## Features

- **Door Measurement** - 3-tap corner-based measurement (bottom-left, bottom-right, top-left)
- **Space Measurement** - 6-tap measurement for rooms/alcoves (walls, floor, ceiling)
- **Virtual Placement** - Place a 3D product box to visualize size in real space (TVs on walls, appliances floor-only)
- **Auto-Smoothing** - 0.75s sampling with median filtering for accurate measurements
- **Plane-First Gating** - Floor measurements require detected plane (not estimated surfaces)
- **Y-Consistency Check** - Validates bottom corners are at same height (±3cm)
- **Tracking Monitoring** - Sampling aborts if AR tracking is lost mid-sample
- **Confidence Scoring** - High/Medium/Low badges with conservative thresholds (0.7 for PASS)
- **Verdict Engine** - WILL FIT / WON'T FIT / NOT SURE logic with safety margins

## Tech Stack

| Component | Technology |
|-----------|------------|
| Language | Kotlin |
| UI Framework | Jetpack Compose |
| AR Framework | ARCore + Sceneform |
| Architecture | MVVM with StateFlow |
| Navigation | Jetpack Navigation Compose |
| Async | Kotlin Coroutines + Flow |

## Requirements

- Android 7.0 (API 24) or higher
- ARCore supported device
- Camera permission

## Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Connect an ARCore-supported Android device
4. Run the app

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/com/willitfit/
├── WillItFitApp.kt              # Application class
├── MainActivity.kt               # Single activity host
├── data/
│   ├── model/                    # Data classes
│   └── repository/               # Data loading
├── ui/
│   ├── theme/                    # Material3 theme
│   ├── navigation/               # Navigation graph
│   ├── home/                     # Product selection
│   ├── measurement/              # AR measurement
│   ├── placement/                # Virtual placement
│   └── result/                   # Verdict display
├── ar/
│   ├── ARSessionManager.kt       # ARCore session
│   ├── RaycastHelper.kt          # Hit testing
│   ├── SamplingManager.kt        # Point sampling
│   └── SceneManager.kt           # 3D rendering
├── service/
│   ├── MeasurementFlowController.kt
│   └── VerdictEngine.kt
└── util/
    ├── MathHelpers.kt
    └── ScreenshotHelper.kt
```

## Documentation

- **[Architecture](docs/ARCHITECTURE.md)** — Project structure, measurement process, accuracy safeguards
- **[Project Snapshot](docs/ai-snapshots/project-snapshot.md)** — High-level overview for AI assistants

## License

Private project - All rights reserved
