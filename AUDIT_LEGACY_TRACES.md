# Legacy Quantum/Cleanup Audit

Scope: `app/src/main`, `app/src/test`.

## Legacy Traces Found Before Removal

- `app/src/main/java/com/battleheim/quantum2048/engine/QuantumBalance.kt:4` defined the old energy/collapse balance object, including max energy 100, starting energy 30, collapse costs 18/30, quantum spawn chance, auto-collapse chance, and compound energy costs.
- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt:80` stored `quantumAlternative` and `quantumAlternativeSpecies` on `Tile`, allowing two-state tiles.
- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt:86` exposed `isQuantum`/`isUnstable` from the two-state tile fields.
- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt:106` stored `quantumEnergy` in `GameState`.
- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt:118` exposed `autoCollapse` on `MoveResult`.
- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt:121` through `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt:125` defined `CollapseEvent`, `CollapseFailure`, and `CollapseResult`.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:20` initialized games with old starting energy.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:65` awarded old quantum energy after merges.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:68` invoked auto-collapse after spawning.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:74` through `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:88` implemented manual Collapse.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:111` through `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:119` spent/refunded energy in the Compound Lab.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:144` through `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:158` spawned unresolved two-state quantum tiles.
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:163` through `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt:174` implemented auto-collapse.
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt:141` rendered the old energy bar.
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt:168` opened the old Collapse dialog.
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt:370` rendered the legacy two-state tile label as `low | high`.
- `app/src/main/java/com/battleheim/quantum2048/ui/GameViewModel.kt:83` through `app/src/main/java/com/battleheim/quantum2048/ui/GameViewModel.kt:113` routed manual Collapse from UI to engine.
- `app/src/test/java/com/battleheim/quantum2048/engine/QuantumEngineTest.kt` tested unresolved quantum spawn, collapse blocking, and old energy gain.
- `app/src/test/java/com/battleheim/quantum2048/engine/DifficultyEngineTest.kt` tested old difficulty-specific energy/collapse behavior.
- `app/src/test/java/com/battleheim/quantum2048/engine/CompoundLabEngineTest.kt` tested compound energy cost/reward behavior.

## Removal Result

- `QuantumBalance.kt` was deleted.
- `Tile` no longer stores `quantumAlternative` or any two-state fields.
- `GameState` no longer stores `quantumEnergy`.
- `CollapseEvent`, `CollapseFailure`, `CollapseResult`, manual collapse, and auto-collapse were removed.
- UI no longer renders an energy bar, collapse dialog, unresolved tile label, or `value1 | value2` tile state.
- `Snapshot.CURRENT_SCHEMA_VERSION` remains `3`; legacy JSON is read with safe defaults and ignored unknown fields, but no energy/collapse compatibility path is preserved.

Final search note: `rg` for the legacy terms only returns Kotlin operator false positives for `|`/`||`; no named legacy mechanism remains under `app/src/main` or `app/src/test`.
