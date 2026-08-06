# Phase 2 — Quantum Core report

## Delivered

- Two-state serializable quantum tile model
- Centralized validated `QuantumBalance`
- Quantum spawning, merge energy, chain bonus and energy cap
- Manual low/high Collapse with fail-closed validation and atomic energy spend
- Controlled automatic Collapse after valid moves
- Seeded and scripted randomness support
- Full-state undo compatibility
- Independent Classic/Quantum load, save and best-score continuity
- Schema-v2 snapshots with phase-1 compatibility
- Quantum-first Compose UI: mode chips, energy meter, selectable dual-value tiles, cost-aware Collapse dialog, snackbar feedback and non-blocking pulse/collapse animation
- Iran-compatible Gradle and Maven mirror baseline verified by the user's phase-1 Windows build

## Rule decisions

1. Unresolved tiles never merge. This avoids hidden probabilistic merges and makes Collapse a strategic action.
2. Only a resolved 2048 tile triggers victory; a possible 2048 does not.
3. A full board with an affordable unresolved tile remains playable because Collapse may unlock a merge.
4. Manual Collapse neither increments move count nor spawns a tile.
5. Auto Collapse occurs only after a valid move, is weighted toward the lower outcome, and costs no energy.
6. Undo restores the pre-action state for both swipes and manual Collapse.

## Test coverage

Classic tests remain in place. Quantum tests cover spawn type, Classic isolation, unresolved merge prevention, energy and cap, both Collapse choices, insufficient energy, deterministic auto Collapse, seeded replay, premature-loss prevention, schema round-trip and phase-1 snapshot compatibility. Independent Kotlin/JUnit execution passed all 23 engine, undo and persistence tests.

## Environment note

This sandbox does not include Android SDK 35 or cached Android dependencies, so Android Gradle execution cannot be completed here. The exact baseline was previously built successfully on the user's Windows/Android Studio environment (`testDebugUnitTest` and `assembleDebug`). Static XML/Kotlin structure and archive integrity are verified before delivery; the included commands are the final on-device verification gate.
