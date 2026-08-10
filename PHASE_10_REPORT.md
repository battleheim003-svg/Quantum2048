# Phase 10 Report - Entanglement

## Implemented

- Added engine-level Entanglement for the current particle/element architecture.
- Added `Tile.entanglementGroupId` as an optional persisted bond marker with safe default compatibility for older snapshots.
- Added central Entanglement tuning in `FusionRules`:
  - `entanglementSpawnChance = 0.12`
  - newly spawned quantum tiles can pair with one adjacent unpaired quantum tile
  - Classic tiles never entangle
- Added chain collapse behavior:
  - when one member of an entangled pair participates in a merge or particle reaction, the surviving partner collapses into the primary fusion output
  - the partner collapse costs no move and creates no extra spawn
  - the bond is consumed after collapse
- Added `MoveResult.entanglementCollapseCount` and `MoveAnimationKind.ENTANGLEMENT`.
- Added a distinct magenta border/glow animation for entangled or chain-collapsed tiles in the board UI.
- Updated Quantum Hard bot scoring so it remains aware of Entanglement without over-prioritizing future chemistry potential.

## Rule Decisions

- Entanglement was adapted to the current Chemistry/Fusion architecture rather than restoring the older `QuantumBalance.kt` / `8 | 16` model.
- Pairing happens only during Quantum spawn, with a small configurable chance, and only if an adjacent quantum tile is available and unpaired.
- The second collapse mirrors the primary fusion output. This keeps the rule readable: "if a bonded tile fuses, its partner resolves to the same result."
- Entanglement collapse is part of the same atomic move transaction. Undo already restores the prior full `GameState`, so no separate undo path was required.

## Tests Added

- `EntanglementEngineTest.quantumSpawnCanPairWithAdjacentUnpairedQuantumTile`
- `EntanglementEngineTest.entangledPartnerCollapsesToPrimaryFusionOutputWhenPairMemberMerges`
- `EntanglementEngineTest.nonEntangledTilesAreNotChangedByEntanglementCollapse`
- `SnapshotTest.schemaThreeTileWithoutEntanglementGroupRemainsReadable`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 46s
24 actionable tasks: 4 executed, 20 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
Exit code 0
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- The visual indicator is a shared magenta border/glow, not a drawn connector line between board cells.
- Entanglement currently supports pairs only, not larger groups.
- No new user-facing text was added for this phase, so no string resources were required.
