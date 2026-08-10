# Phase 14 Report - Energy Overflow Bonus

## Implemented

- Added a lightweight persisted Quantum energy field to `GameState`.
- Added centralized energy tuning in `FusionRules`:
  - `initialEnergy = 30`
  - `maxEnergy = 100`
  - `energyPerMerge = 6`
  - `energyPerChainMerge = 3`
  - `overflowScorePerEnergy = 4`
- Quantum merges now award energy.
- Multiple merges in one swipe award the configured chain energy bonus.
- Energy is capped at 100.
- Any energy above the cap converts into score through `overflowScorePerEnergy`.
- Added `MoveResult.energyOverflowBonus` so UI and tests can inspect the awarded bonus.
- Added a compact energy readout to the Quantum game header.
- Added snackbar feedback when a move produces overflow score.

## Rule Decisions

- This branch previously did not persist energy. Phase 14 introduces the minimal energy model needed for overflow without rewriting Tunneling, Superposition, or Observer costs.
- Utility actions still spend score as documented in their phase reports. Future balancing can migrate them to energy now that `GameState.energy` exists.
- Overflow bonus is score-only and does not recursively feed back into energy.
- Classic mode keeps energy at 0 and never receives overflow bonus.

## Tests Added

- `EnergyOverflowEngineTest.quantumMergeAddsEnergyWithoutOverflowWhenBelowCap`
- `EnergyOverflowEngineTest.chainMergeAddsBaseAndChainEnergy`
- `EnergyOverflowEngineTest.overflowEnergyConvertsToScoreBonus`
- `EnergyOverflowEngineTest.classicMergeDoesNotUseQuantumEnergyOverflow`
- `SnapshotTest.schemaThreeQuantumSnapshotWithoutEnergyUsesInitialEnergy`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 41s
24 actionable tasks: 11 executed, 13 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 2s
37 actionable tasks: 3 executed, 34 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Tunneling, Superposition Collapse, and Observer still spend score because those phases were implemented before this energy field was introduced.
- No dedicated overflow animation was added; the current feedback is a snackbar plus score/energy update.
