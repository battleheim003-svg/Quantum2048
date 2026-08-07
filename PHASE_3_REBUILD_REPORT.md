# Phase 3 Rebuild Report - Movement and Merge Animation

## Changed

- Added `MoveAnimation`, `MoveAnimationKind`, and per-move animation events from the engine.
- Board rendering now uses an absolute-positioned tile layer over fixed cell backgrounds, allowing real slide offsets from source cell to destination cell.
- Slide timing uses `FastOutSlowInEasing` over 180ms.
- Merge tiles receive a micro-scale pulse.
- Electron/proton reactions receive a distinct stronger scale/glow/alpha pulse.
- Spawned tiles scale/fade in.
- Reduced Motion snaps animations immediately.
- Input is locked for 190ms after accepted moves so a second swipe cannot enter during the animation window.

## Tests

- Added `MoveAnimationEventTest` covering slide source/destination events, merge events, reaction events, and spawn events.
- Existing unit tests still pass with single-worker execution.

## Verification

- `.\gradlew.bat assembleDebug --no-daemon` succeeded.
- `.\gradlew.bat testDebugUnitTest --tests "com.battleheim.quantum2048.engine.MoveAnimationEventTest" --no-daemon --max-workers=1` succeeded.
- `.\gradlew.bat testDebugUnitTest --no-daemon --max-workers=1` succeeded.

## Remaining

- Compose UI timing/video verification still needs an emulator or device run. The current automated coverage verifies the engine animation contract that the Compose layer consumes for non-teleport movement.
