# Phase 4 Report - Phase 2 Difficulty System

## Implemented

- Added `Difficulty { EASY, MEDIUM, HARD, QUANTUM }` in the engine.
- Kept `GameMode` for backward compatibility while making `GameState` carry an explicit `difficulty`.
- Added explicit per-difficulty rules in `QuantumBalance` through `DifficultyRules`.
- Parameterized `GameEngine.newGame(...)`, spawning, energy gain, collapse availability, and fusion limits by difficulty.
- Preserved Quantum balance values for the Quantum difficulty:
  - starting energy 30
  - quantum spawn chance 0.18
  - auto-collapse chance 0.08
  - existing collapse costs remain unchanged
- Difficulty behavior:
  - Easy: Classic 2048, no particles, no energy, no collapse, no Compound Lab.
  - Medium: stable particle/element mode, fusion capped at Neon, no collapse, no energy, simple recipe tier enabled.
  - Hard: full fusion chain to Gold, energy enabled, collapse disabled, Hard recipe tier enabled, compound actions reserved behind an explicit energy cost.
  - Quantum: full fusion chain, full energy, unresolved tiles, manual/auto collapse, full recipe tier enabled.
- Added v3 snapshot schema and per-difficulty storage slots:
  - `snapshot_v3_easy`
  - `snapshot_v3_medium`
  - `snapshot_v3_hard`
  - `snapshot_v3_quantum`
- Kept backward compatibility with v2 Classic/Quantum saves:
  - Classic maps to Easy.
  - Quantum maps to Quantum.
- Restored tap-to-collapse for unresolved tiles in the current game screen because Quantum difficulty must keep existing collapse rules.

## Files Changed

- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt`
- `app/src/main/java/com/battleheim/quantum2048/engine/QuantumBalance.kt`
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt`
- `app/src/main/java/com/battleheim/quantum2048/domain/GameRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/data/GameSnapshot.kt`
- `app/src/main/java/com/battleheim/quantum2048/data/DataStoreGameRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameViewModel.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt`
- `app/src/test/java/com/battleheim/quantum2048/data/SnapshotTest.kt`
- `app/src/test/java/com/battleheim/quantum2048/engine/DifficultyEngineTest.kt`

## Tests Added

- `DifficultyEngineTest.easyIsClassic2048WithoutParticlesOrEnergy`
- `DifficultyEngineTest.mediumSpawnsStableParticlesAndDisablesCollapseEnergy`
- `DifficultyEngineTest.hardUsesFullFusionChainAndEnergyWithoutCollapse`
- `DifficultyEngineTest.quantumKeepsUnresolvedSpawnCollapseAndEnergyRules`
- `DifficultyEngineTest.mediumFusionStopsAtNeon`
- `DifficultyEngineTest.recipeAccessIsExplicitPerDifficulty`
- Updated snapshot tests for schema v3 and v2 migration defaults.

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 20s
24 actionable tasks: 2 executed, 22 up-to-date
```

```text
.\gradlew.bat --no-daemon assembleDebug --console=plain
BUILD SUCCESSFUL in 17s
37 actionable tasks: 4 executed, 33 up-to-date
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Deliberately Deferred

- Phase 1 Compound Lab drag transactions are still not implemented, so Phase 2 only exposes difficulty-specific recipe access and compound energy policy.
- No navigation shell, level select, menu, settings, collection repository, or collection UI was added.
- UI still has the current mode selector; full four-level selection belongs to Phase 4 in the original plan.
