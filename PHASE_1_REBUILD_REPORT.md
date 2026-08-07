# Phase 1 Rebuild Report - Unified Particle Fusion Core

## Changed

- Added `FusionRules.kt` as the central rules table for game values, symbols, ranks, element chain, spawn density, and electron/proton reactions.
- Implemented same-kind electron and proton doubling chains.
- Implemented same-kind element fusion with an explicit atomic mapping: H(1), He(2), Be(4), O(8), Ne(10), Si(14), Fe(26), Au(79).
- Implemented cross-category reaction output: `min(electrons, protons)` becomes the nearest defined element at or below that atomic number, and the imbalance remains as a particle tile.
- Updated quantum tile UI to show rank/Z, symbol, and game value together on every tile.

## Tests

- Added `FusionRulesTest` covering electron chain, proton chain, element chain, `10e- + 11p+ -> Ne + 1p+`, balanced reactions, undefined-Z round-down, and non-matching particles.

## Verification

- `.\gradlew.bat assembleDebug --no-daemon` succeeded.
- Unit test execution was blocked by JVM native memory exhaustion after Kotlin compilation; see `PHASE_0_REPORT.md`.

## Remaining

- Add screenshot/video capture once an emulator/device run is available.
- Build Phase 2 UI for 4x4/6x6/8x8 selection and per-size saves.
