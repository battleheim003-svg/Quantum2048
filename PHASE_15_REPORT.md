# Phase 15 Report - New Game Modes

## Implemented

- Added three new Quantum difficulties:
  - `ZEN`
  - `HARDCORE`
  - `PUZZLE`
- Reused the existing per-difficulty persistence model, so each new mode has independent saved state and best score keys.
- Added centralized mode tuning in `FusionRules`:
  - `zenEnergy = 1000`
  - `hardcoreInitialEnergy = 0`
  - `puzzleMoveLimit = 3`
  - `puzzleTargetValue = 128`
- Zen starts with a relaxed high energy pool.
- Hardcore starts with zero energy and disables undo.
- Puzzle starts from a deterministic fixed board.
- Puzzle does not spawn random tiles after moves.
- Puzzle wins when the board reaches the configured target value.
- Puzzle loses when the move limit is exhausted before reaching the target.
- Added Level Select descriptions for the new modes.
- Added theme/repository coverage for the new `Difficulty` enum values.

## Rule Decisions

- The current app already persists state per `Difficulty`, so new modes were added as new difficulties instead of introducing a parallel game-mode enum.
- Zen uses a high finite energy value instead of true infinity to avoid special cases in UI, serialization, and arithmetic.
- Hardcore disables the ViewModel undo credit for moves and utility actions.
- Puzzle currently ships one fixed starter board as the first playable challenge. The rules are centralized so more fixed puzzles can be layered in a later pass.

## Tests Added

- `GameModesEngineTest.zenStartsWithRelaxedEnergyPoolAndIndependentDifficulty`
- `GameModesEngineTest.hardcoreStartsWithZeroEnergyAndDisablesUndo`
- `GameModesEngineTest.puzzleStartsFromFixedBoardWithoutRandomSpawn`
- `GameModesEngineTest.puzzleCanBeSolvedByReachingTargetWithinMoveLimitWithoutSpawning`
- `GameModesEngineTest.puzzleLosesAfterMoveLimitWhenTargetIsNotReached`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 43s
24 actionable tasks: 11 executed, 13 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 2s
37 actionable tasks: 3 executed, 34 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Puzzle mode currently has one fixed challenge board, not three hand-authored puzzle levels.
- Zen is implemented as a high energy cap rather than literal infinite energy.
- Hardcore cost multipliers are not yet applied to utility actions because those actions still spend score in this branch.
