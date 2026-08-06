# Phase 9 Report - Phase 1 Compound Lab

## Implemented

- Added engine-level Compound Lab transaction through `GameEngine.combineCompound(...)`.
- Compound Lab rules:
  - disabled in Easy
  - enabled where `DifficultyRules.compoundLabEnabled` is true
  - accepts stable element tiles only
  - unresolved quantum tiles cannot enter the lab
  - successful compound synthesis removes source tiles from the board
  - successful compound synthesis does not spawn a tile
  - successful compound synthesis does not increment move count
  - score and energy are updated atomically according to the matched recipe and difficulty rules
- Added `CompoundResult` and `CompoundFailure`.
- Added UI Compound Lab panel above the board in eligible levels.
- Stable element tiles can be tapped or dragged downward into the lab.
- Selected lab tiles are highlighted.
- Matching samples synthesize a compound and clear the lab.
- Invalid samples provide snackbar feedback and leave state unchanged.
- Successful compounds are recorded in the global Collection repository.
- Undo now reverts the prior game state and decrements/removes the last recorded compound discovery.
- Added `CollectionState.unrecord(...)` and `CollectionRepository.unrecord(...)`.

## Files Changed

- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt`
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt`
- `app/src/main/java/com/battleheim/quantum2048/domain/CollectionRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/data/DataStoreCollectionRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/AppShell.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameViewModel.kt`
- `app/src/test/java/com/battleheim/quantum2048/data/CollectionSnapshotTest.kt`
- `app/src/test/java/com/battleheim/quantum2048/engine/CompoundLabEngineTest.kt`

## Tests Added

- `CompoundLabEngineTest.matchingCompoundRemovesTilesWithoutSpawningAndReturnsRecipe`
- `CompoundLabEngineTest.unrelatedCompoundLeavesStateUnchanged`
- `CompoundLabEngineTest.easyCannotUseCompoundLab`
- `CompoundLabEngineTest.hardCompoundSpendsConfiguredEnergyAndAppliesReward`
- `CollectionSnapshotTest.unrecordDecrementsOrRemovesCompoundForUndo`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 37s
24 actionable tasks: 5 executed, 19 up-to-date
```

```text
.\gradlew.bat --no-daemon assembleDebug --console=plain
BUILD SUCCESSFUL in 17s
37 actionable tasks: 3 executed, 34 up-to-date
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Notes

- The UI supports both tap-to-lab and downward drag-to-lab. This keeps the mechanic usable on touch screens while still supporting the requested drag interaction.
- Compound-specific audio reuses the existing merge feedback hook until a dedicated sample-based audio layer exists.
