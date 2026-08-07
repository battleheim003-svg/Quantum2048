# Phase 0 Report - Legacy Cleanup

## Changed

- Removed the old Superposition/Collapse/Energy model and deleted `QuantumBalance.kt`.
- Replaced two-state tiles with a single `TileKind` model: classic number, electron group, proton group, or element.
- Removed manual Collapse, auto-collapse, quantum energy, and energy costs from engine, ViewModel, UI, and tests.
- Updated app name text from Collapse to Fusion.
- Added `AUDIT_LEGACY_TRACES.md` with the legacy locations found before removal.

## Verification

- `.\gradlew.bat assembleDebug --no-daemon` succeeded.
- `.\gradlew.bat testDebugUnitTest --no-daemon` compiled main and unit-test Kotlin, then the Gradle JVM crashed with native memory exhaustion before test execution completed. The crash log reports: `There is insufficient memory for the Java Runtime Environment to continue`.

## Remaining

- Re-run unit tests on a less memory-constrained JVM/session.
- Continue Phase 2+ work for board-size selection, duel mode, and richer animation timing.
