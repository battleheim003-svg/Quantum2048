# Phase 21 Report - Integration Balance Pass

## Implemented

- Migrated utility-action costs from score to energy now that `GameState.energy` exists.
- Replaced score costs with centralized energy costs in `FusionRules`:
  - `tunnelingEnergyCost = 42`
  - `superpositionCollapseEnergyCosts = [18, 28, 40]`
  - `observerPreviewEnergyCost = 8`
- `GameEngine.tunnel(...)` now spends energy and leaves score unchanged.
- `GameEngine.collapseSuperposition(...)` now spends energy and leaves score unchanged.
- `GameEngine.observeSuperposition(...)` now spends energy and leaves score unchanged.
- Updated UI copy and Superposition dialog labels from score costs to energy costs.
- Updated failure messages to say energy rather than score.
- Updated affected unit tests to assert energy deductions and score preservation.

## Rule Decisions

- This pass unifies the post-energy economy while keeping the utility action behavior from earlier phases:
  - no spawn
  - no move-count increment
  - fail-closed on insufficient resource
- The cost values are deliberately below the existing 100 energy cap so regular Quantum mode can use each tool, while still making Tunneling and high Collapse meaningful spends.
- Existing score-based reports from earlier phases are superseded by this balance pass.

## Tests Updated

- `TunnelingEngineTest`
- `SuperpositionChainEngineTest`
- `ObserverEffectEngineTest`
- `AchievementEngineTest`
- `StatisticsEngineTest`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 39s
24 actionable tasks: 2 executed, 22 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 3s
37 actionable tasks: 3 executed, 34 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Hardcore-specific utility multipliers are still not implemented; Hardcore inherits the same energy costs but starts with zero energy.
- No full economy tuning simulation was added beyond the existing deterministic/unit coverage.
