# Quantum 2048: Fusion

Quantum 2048: Fusion is an offline Android puzzle game that starts with the clean rhythm of 2048 and opens into a stranger lab of particles, energy, and Collapse choices.

Classic mode is here for fast, familiar merge play. Quantum mode adds unstable tiles, particle reactions, manual Collapse, energy management, daily seeded challenges, local achievements, and a growing collection of discovered compounds.

## What Makes It Different

Most 2048 variants ask one question: can you merge higher?

Quantum 2048 asks another one too: when a tile has multiple possible futures, when do you spend energy to force one outcome? Collapse turns a simple board into a puzzle about timing, risk, and control.

## Features

- Classic 2048 merge play with saved progress.
- Quantum puzzle rules with particles, reactions, superposition, Collapse, and energy.
- Daily challenge with a deterministic UTC seed and local 7-day history.
- Achievements with visible progress bars.
- Statistics for Classic and Quantum modes.
- Persian and English UI with RTL support.
- Fully offline local saves using Android DataStore.

## Download

[Coming soon on Google Play]

## Build

Requirements:

- JDK 17
- Android SDK 35
- Gradle wrapper included in this repository

Run:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For deeper architecture notes, persistence details, and historical phase context, see [docs/TECHNICAL.md](docs/TECHNICAL.md).

## Screenshots

Gameplay screenshots and store-ready promotional art still need to be captured from a real device or final emulator build before public release.
