# Phase 20 Report - Tutorial / Onboarding

## Implemented

- Added persisted tutorial completion state:
  - `GameState.tutorialCompleted`
- Added `GameViewModel.completeTutorial()` to mark onboarding done and persist the snapshot.
- Added a Tutorial route to the app shell.
- Tutorial opens automatically the first time the loaded active game has not completed onboarding.
- Main Menu now includes a Tutorial button so players can replay it later.
- Added a controlled, deterministic 4x4 tutorial board.
- Added seven interactive tutorial steps:
  - move and merge
  - particle reaction
  - collapse choices
  - energy and chain merges
  - auto collapse concept
  - one-step undo
  - advanced tools
- Each step requires tapping the tutorial action button before Next is enabled.
- Added Skip and Finish paths; both persist tutorial completion.
- Added string resources for all new tutorial UI text.

## Rule Decisions

- The tutorial board is UI-controlled and deterministic rather than connected to the live random game engine.
- This avoids mutating player saves while still making the player interact with each lesson before advancing.
- Tutorial completion is stored in the existing snapshot instead of a separate settings repository, matching the phase requirement.
- Advanced features are introduced in one final step because Entanglement, Tunneling, Superposition, Observer, Energy, Daily, Achievements, and Statistics now exist in this branch.

## Tests Added

- `SnapshotTest.tutorialCompletionRoundTripPersists`
- `SnapshotTest.oldSnapshotWithoutTutorialCompletionDefaultsToFalse`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 1m 38s
24 actionable tasks: 10 executed, 14 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 7s
37 actionable tasks: 4 executed, 33 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- The tutorial uses a controlled UI board and confirm-action gating, not real swipe gesture validation inside the live game board.
- Persian resource values for new tutorial strings are English fallbacks because the existing `values-fa` file is mojibake-encoded in this workspace.
