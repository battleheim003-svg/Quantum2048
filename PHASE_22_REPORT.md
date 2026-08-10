# Phase 22 Report - Global Profile Persistence

## Implemented

- Added a local profile persistence layer independent of active game snapshots.
- Added `ProfileState` with global counters:
  - unlocked achievements
  - successful collapse count
  - low/high collapse counts
  - total win energy
  - win energy samples
  - total chain merge count
- Added `ProfileRepository`.
- Added `DataStoreProfileRepository` using a separate `profile_state_v1` DataStore.
- `GameViewModel.persist()` now saves the active game and records its progress into the global profile.
- Collection achievements now read from `ProfileRepository` rather than the active game snapshot.
- Statistics dashboard now reads from `ProfileRepository` rather than the active game snapshot.
- Wired profile persistence through `QuantumApp`, `MainActivity`, and `QuantumAppShell`.

## Rule Decisions

- Profile state merges counters using the largest observed value from any saved game state. This prevents undo/restores from reducing global progress.
- Achievements are unioned into the global profile once unlocked.
- Active snapshots still keep their fields for compatibility and local run context, but UI-level achievements/statistics now use the global profile.

## Tests Added

- `ProfileStateTest.recordMergesAchievementsAndKeepsLargestCounters`
- `ProfileStateTest.derivedStatisticsUseProfileCounters`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 1m 1s
24 actionable tasks: 13 executed, 11 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 6s
37 actionable tasks: 4 executed, 33 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Profile aggregation currently records progress when `GameViewModel.persist()` runs. If future features mutate progress outside that path, they should also call `ProfileRepository.record(...)`.
- The profile uses max counters rather than additive historical aggregation because existing per-run snapshots do not carry event deltas.
