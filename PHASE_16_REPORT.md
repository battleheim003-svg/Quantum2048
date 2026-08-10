# Phase 16 Report - Daily Challenge

## Implemented

- Added `Difficulty.DAILY` as a separate Quantum challenge mode.
- Added daily challenge metadata to `GameState`:
  - `dailyChallengeDate`
  - `dailyBestScore`
- Added deterministic daily seed generation in `FusionRules.dailySeed(...)`.
- Added `GameEngine.newDailyChallenge(...)`, which builds the same initial board for the same date.
- Daily challenge boards differ across dates.
- Daily save loading refreshes the board when the saved daily date is not today.
- Daily best score updates from the active daily run score.
- Added Daily mode to Level Select with its own description.
- Added Daily best score display in the game header and end dialog.
- Existing per-difficulty save keys keep Daily state isolated from Classic, Quantum, Zen, Hardcore, and Puzzle.

## Rule Decisions

- Daily Challenge was implemented as a `Difficulty` because the app already has independent persistence and UI selection per difficulty.
- The local leaderboard is a single persisted `dailyBestScore` for the active daily date, matching the local-only requirement without introducing server or account state.
- The daily seed is generated from `LocalDate.toString()` so the same date always maps to the same board.
- Daily uses the current device date when launched from the UI.

## Tests Added

- `DailyChallengeEngineTest.dailySeedIsStableForSameDateAndDifferentForDifferentDates`
- `DailyChallengeEngineTest.dailyChallengeBoardIsDeterministicForDate`
- `DailyChallengeEngineTest.dailyChallengeBoardChangesAcrossDates`
- `DailyChallengeEngineTest.dailyBestScoreTracksBestScoreWithinDailyState`
- `SnapshotTest.dailySnapshotRoundTripPreservesDateAndDailyBest`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 46s
24 actionable tasks: 11 executed, 13 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 4s
37 actionable tasks: 3 executed, 34 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Daily best score is stored in the same active Daily snapshot. Historical daily records are not retained after the app advances to a new date.
- There is no server leaderboard; this is local-only as requested.
- Daily mode uses the device date rather than a fixed UTC day boundary.
