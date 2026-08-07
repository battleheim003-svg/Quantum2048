# Phase 2 Rebuild Report - Board Size Selection

## Changed

- Added `FusionRules.supportedBoardSizes = [4, 6, 8]` and centralized spawn density through `FusionRules.spawnCount(size)`.
- Kept engine movement, merging, spawning, win/loss evaluation, and reactions size-driven through `GameState.size`.
- Added a New Game board-size selector for 4x4, 6x6, and 8x8 before choosing difficulty.
- Extended navigation routes to carry both difficulty and board size.
- Extended `GameRepository` and `DataStoreGameRepository` to save separate snapshots per `difficulty x board size`.
- Kept legacy/default repository methods compatible with 4x4.
- Made the board spacing and quantum tile typography adapt for 6x6 and 8x8 so symbol + game value stay visible.

## Tests

- Added `BoardSizeEngineTest` for:
  - configured size and spawn density across 4x4, 6x6, 8x8
  - dynamic 6x6 movement/merge
  - 8x8 game-over detection across all cells

## Verification

- `.\gradlew.bat assembleDebug --no-daemon` succeeded.
- `.\gradlew.bat testDebugUnitTest --no-daemon` compiled main and unit-test Kotlin, then failed because Gradle Test Executor JVMs crashed with native memory exhaustion.
- `.\gradlew.bat testDebugUnitTest --tests "com.battleheim.quantum2048.engine.BoardSizeEngineTest" --no-daemon --max-workers=1` also failed with the same native-memory JVM crash before producing test assertions.

## Remaining

- Re-run unit tests on a Windows session with a larger paging file or lower global memory pressure.
- Add instrumented UI coverage for the board-size selector once the emulator/device workflow is stable.
