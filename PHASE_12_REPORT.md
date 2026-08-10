# Phase 12 Report - Superposition Chains

## Implemented

- Added three-state superposition support to the current particle/element architecture.
- Added `Tile.superpositionValues` with a safe empty-list default for older snapshots.
- Added centralized tuning in `FusionRules`:
  - `superpositionScoreThreshold = 512`
  - `superpositionSpawnChance = 0.10`
  - `superpositionCollapseCosts = [32, 48, 64]`
- Quantum spawns can create a three-state tile once the run score reaches the threshold.
- Superposition tiles display their possible values as `1 | 2 | 4`.
- Superposition tiles do not merge or react until resolved.
- Added `GameEngine.collapseSuperposition(...)`:
  - validates Quantum-only use
  - validates tile and choice
  - spends the configured score cost
  - resolves the tile to the selected value
  - does not spawn a tile
  - does not increment move count
- Added a tap-to-collapse UI dialog with three choices and costs.
- Undo restores the full prior state after collapse.

## Rule Decisions

- This phase was adapted to the rebuilt Chemistry/Fusion architecture instead of restoring the older unresolved numeric `8 | 16` model.
- Because this branch has no energy field, collapse costs spend score. The values are centralized in `FusionRules` so they can move to energy if that economy returns.
- Three-state tiles are currently particle tiles because quantum spawn in this branch creates particles. Element-level superposition can be layered later once element spawning exists.
- Entanglement pairing skips unresolved superposition tiles so the two mechanics do not create ambiguous chain outcomes.

## Tests Added

- `SuperpositionChainEngineTest.quantumSpawnCanCreateThreeStateTileAfterScoreThreshold`
- `SuperpositionChainEngineTest.superpositionTilesDoNotMergeBeforeCollapse`
- `SuperpositionChainEngineTest.collapseCanResolveEachOfThreeValuesWithConfiguredCost`
- `SuperpositionChainEngineTest.collapseFailsClosedWhenScoreIsInsufficient`
- `SnapshotTest.schemaThreeTileWithoutSuperpositionValuesRemainsReadable`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 43s
24 actionable tasks: 15 executed, 9 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 5s
37 actionable tasks: 4 executed, 33 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Collapse currently spends score rather than energy because the current branch has no persisted energy meter.
- The UI uses a simple dialog instead of a dedicated animated collapse panel.
- Persian resource values for new strings are English fallbacks because the existing `values-fa` resource file is mojibake-encoded in this workspace.
