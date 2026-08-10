# Phase 11 Report - Quantum Tunneling

## Implemented

- Added engine-level Quantum Tunneling through `GameEngine.tunnel(...)`.
- Added centralized tuning in `FusionRules`:
  - `tunnelingScoreCost = 96`
- Added `TunnelResult` and `TunnelFailure` with fail-closed outcomes for:
  - non-Quantum levels
  - inactive games
  - missing source tiles
  - occupied or invalid destinations
  - insufficient score
- Tunneling moves one tile to an empty destination without:
  - spawning a new tile
  - incrementing move count
  - changing `nextTileId`
- Tunneling preserves tile identity and existing Entanglement bonds.
- Added `MoveAnimationKind.TUNNEL` and UI feedback hooks.
- Added a board-level Tunnel flow:
  - tap Tunnel
  - tap a source tile
  - tap an empty destination cell
  - Undo restores the prior full state
- Added localized string keys for the Tunnel button and cancel state.

## Rule Decisions

- The current rebuilt architecture does not have an energy field in `GameState`, so Tunneling spends score as the available economy.
- The score cost is centralized in `FusionRules` and documented as the migration point if an energy meter is reintroduced later.
- Tunneling is Quantum-only and disabled in Classic.
- Tunneling is not a swipe move and intentionally does not spawn a tile, matching the Collapse/utility-action pattern from the original phase request.

## Tests Added

- `TunnelingEngineTest.tunnelMovesTileToEmptyDestinationWithoutSpawningOrCountingMove`
- `TunnelingEngineTest.tunnelFailsClosedWhenDestinationIsOccupied`
- `TunnelingEngineTest.tunnelFailsClosedWhenScoreIsInsufficient`
- `TunnelingEngineTest.classicModeCannotTunnel`
- `TunnelingEngineTest.tunnelPreservesEntanglementBondOnMovedTile`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 50s
24 actionable tasks: 15 executed, 9 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
Exit code 0
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Tunneling currently spends score instead of energy because this branch has no persisted energy model.
- The Tunnel UI is a simple tap source/tap destination flow; it does not yet support drag-to-destination.
- Persian resource values were added as plain fallback labels because the existing `values-fa` file is mojibake-encoded in this workspace.
