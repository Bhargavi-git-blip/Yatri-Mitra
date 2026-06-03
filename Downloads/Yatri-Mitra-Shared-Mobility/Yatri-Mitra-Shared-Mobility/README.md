# 🛺 Yatri-Mitra — Shared Auto Tracker

> A communal transit tracker for small-town India. Real-time ETA. Live vehicle simulation.

---

## Project Structure

```
YatriMitra/
└── app/src/main/
    ├── java/com/yatrimitra/app/
    │   ├── model/
    │   │   └── Models.kt              ← Data classes (Auto, RouteStop, SimulationState)
    │   ├── simulation/
    │   │   └── SimulationEngine.kt    ★ Core simulation + ETA logic (fully documented)
    │   ├── viewmodel/
    │   │   └── RouteViewModel.kt      ← StateFlow + coroutine ticker
    │   └── ui/
    │       ├── MainActivity.kt        ← Lifecycle-aware state collection
    │       └── RouteCanvasView.kt     ← Custom Canvas rendering
    └── res/
        ├── layout/activity_main.xml   ← UI structure
        └── values/colors.xml          ← Color palette
```

---

## Architecture

```
SimulationEngine          RouteViewModel           MainActivity
(Pure Functions)    ←→    (StateFlow + Coroutine)  (Lifecycle Observer)
                                ↓
                         RouteCanvasView
                         (Canvas Drawing)
```

- **SimulationEngine** — Stateless. Takes state in, returns new state. Easily testable.
- **RouteViewModel** — Holds the single source of truth via `MutableStateFlow`. Survives rotation.
- **MainActivity** — Collects flow updates with `repeatOnLifecycle`. Passes state to views.
- **RouteCanvasView** — Custom View. Redraws every ~50ms via `invalidate()`.

---

## Key Concepts

### ETA Formula

```
ETA (minutes) = (distanceToStop in km / speed in km/h) × 60

where:
  distanceToStop = (stopPosition - autoPosition) × routeLengthKm
```

If an auto has passed the stop, it wraps around:
```
distanceToStop = (1.0 - autoPosition + stopPosition) × routeLengthKm
```

### Simulation Tick (Position Update)

```kotlin
// Speed (km/h) → normalized position per second
val positionDelta = auto.speedKmph / (routeLengthKm × 3600f) × deltaTime
val newPosition   = (auto.position + positionDelta) % 1.0f
```

### Screen Rotation Safety

1. `by viewModels()` → same ViewModel instance after rotation
2. `StateFlow` → always emits current value to new collectors
3. `repeatOnLifecycle(STARTED)` → auto-pauses/resumes collection

### Smooth Animation (No Jumping)

- Positions are `Float` — no integer rounding in canvas mapping
- 50ms tick = 20fps which appears smooth to human eye
- Canvas uses `Paint.ANTI_ALIAS_FLAG` for sub-pixel rendering

---

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1) or later
- Android SDK 34
- Kotlin 1.9+
- JDK 11+

### Build & Run
```bash
# Clone / unzip project
cd YatriMitra

# Open in Android Studio, then:
# Build → Make Project
# Run → Run 'app' (on emulator or device, API 24+)
```

### Dependencies (auto-resolved via Gradle)
- `lifecycle-viewmodel-ktx` — ViewModel + viewModelScope
- `lifecycle-runtime-ktx` — repeatOnLifecycle
- `kotlinx-coroutines-android` — StateFlow, coroutines

---

## Student Checklist (Success Criteria)

| Criterion | Implementation Location |
|-----------|------------------------|
| ✅ Smooth vehicle movement (no jumping) | `RouteCanvasView.positionToX()` — float math, no rounding |
| ✅ Real-time ETA countdown | `SimulationEngine.calculateEta()` — documented formula |
| ✅ Screen rotation without reset | `RouteViewModel` — StateFlow survives via ViewModel |
| ✅ Documented Simulation Logic | `SimulationEngine.kt` — every method has KDoc comments |

---

## Extending the App

- **Add more autos**: Append to `SimulationEngine.defaultAutos`
- **Real GPS**: Replace `StateFlow` emissions with `FusedLocationProviderClient` updates
- **Push notifications**: When ETA < 2 min, trigger a `NotificationCompat` builder
- **Passenger cluster heatmap**: Overlay a `LinearGradient` on the canvas based on stop tap counts

---

*Built for the GenAI Android Development challenge — Yatri-Mitra demonstrating ETA Logic.*
