# Phase 17 Report - Achievements

## Implemented

- Added local-only achievement progress to `GameState`:
  - `successfulCollapseCount`
  - `usedUndo`
  - `unlockedAchievements`
- Added centralized achievement IDs and unlock evaluation in `FusionRules`:
  - `collapse_century`
  - `resolved_2048`
  - `no_undo_win`
- Superposition Collapse increments the successful collapse counter.
- Reaching 100 successful collapses without being in a lost state unlocks Collapse Century.
- Winning with a 2048-or-higher resolved board and no unresolved superposition tiles unlocks Resolved 2048.
- Winning without using Undo unlocks Clean Run.
- Undo marks the restored state as `usedUndo = true`, preventing the no-undo achievement for that run.
- Achievement progress is serialized with the existing game snapshot.
- Collection screen now displays achievement rows with locked/unlocked state from the active game snapshot.

## Rule Decisions

- Achievement progress is stored in `GameState` instead of a separate repository to keep the implementation local-only and schema-compatible with existing snapshot persistence.
- "Collapse" maps to the current architecture's Superposition Collapse action.
- "Resolved 2048" checks for a won state, a 2048-or-higher board value, and no unresolved superposition tiles.
- The Collection screen was reused as the local achievement surface to avoid introducing another navigation destination in this phase.

## Tests Added

- `AchievementEngineTest.oneHundredSuccessfulCollapsesUnlocksCollapseCentury`
- `AchievementEngineTest.resolved2048WinUnlocksResolvedAndNoUndoAchievements`
- `AchievementEngineTest.undoUsedPreventsNoUndoWinAchievement`
- `SnapshotTest.achievementProgressRoundTripPersists`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 1m
24 actionable tasks: 10 executed, 14 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
Exit code 0
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Achievement progress is tied to the active game snapshot, not a separate global profile repository.
- No toast/modal unlock celebration was added; unlock state is visible in Collection.
