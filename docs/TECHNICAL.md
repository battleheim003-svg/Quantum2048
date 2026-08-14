# Quantum 2048: Collapse — Phase 2 Quantum Core

Battleheim Studio's offline Android puzzle game. This build contains two independently saved modes: deterministic Classic 2048 and the first complete Quantum ruleset.

## Phase 2 gameplay contract

- A quantum tile displays two ordered possibilities such as `8 | 16`.
- An unresolved quantum tile cannot merge. Tap it to open Collapse and stabilize one value.
- Low-value Collapse costs 18 energy; high-value Collapse costs 30.
- Each merge earns 6 energy. Multiple merges in one swipe add a 3-energy chain bonus per extra merge.
- Energy is capped at 100 and Quantum games start with 30.
- Quantum spawn chance is 18%. After a valid move, automatic Collapse has an 8% chance and favors the lower result 65/35. It never consumes energy.
- Manual Collapse does not count as a move and does not spawn a tile. It is a single atomic state transaction.
- Undo restores the entire prior state, including energy and superposition. Undo remains one-level and session-only.

All values are centralized in `engine/QuantumBalance.kt`. All randomness passes through `RandomProvider`; `SeededRandomProvider` makes replays deterministic.

## Architecture

- `engine`: immutable state, Classic/Quantum movement, energy, Collapse, status rules, injected randomness
- `domain`: repository and fair one-level undo contracts
- `data`: schema-v2 JSON snapshots in Preferences DataStore, separate keys per mode, phase-1-compatible defaults
- `ui`: StateFlow ViewModel, mode switching, atomic input lock, Collapse dialog, energy UI and non-blocking pulse animation
- `designsystem`, `audio`, `ads`: replaceable visual and service boundaries

## Verified Windows/Iran-compatible toolchain

- JDK 17, AGP 8.9.1, Gradle 8.11.1
- Android SDK 35 (`minSdk 26`, `targetSdk 35`)
- Activity Compose 1.10.1
- Aliyun/Huawei dependency mirrors and Tencent Gradle distribution mirror

Open in Android Studio, then run:

```powershell
.\gradlew.bat testDebugUnitTest --console=plain
.\gradlew.bat assembleDebug --console=plain
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Persistence

Snapshots now use `schemaVersion = 2`. New quantum fields have safe defaults, so existing phase-1 Classic saves remain readable. Classic and Quantum records and active games are stored independently.

## Phase boundary

Phase 3 will add navigation/menu, interactive tutorial, settings, sound/haptics, statistics, full Persian/English resources, polished motion and accessibility. Entanglement, black holes, daily challenges and monetization remain intentionally disabled.
