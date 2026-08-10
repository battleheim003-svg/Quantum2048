# Phase 13 Report - Observer Effect

## Implemented

- Added engine-level Observer Effect through `GameEngine.observeSuperposition(...)`.
- Added centralized tuning in `FusionRules`:
  - `observerPreviewScoreCost = 12`
- Added `ObserverResult` and `ObserverFailure` with fail-closed outcomes for:
  - non-Quantum levels
  - inactive games
  - missing tiles
  - stable non-superposition tiles
  - insufficient score
- Observer preview spends score but does not mutate the observed tile.
- The preview value is deterministic and uses the middle value of the three-state superposition.
- Added ViewModel-level temporary preview state.
- Long-pressing a superposition tile shows the preview value temporarily on the tile and then clears it.
- The preview is intentionally lightweight and does not create undo history because it is not a full collapse.

## Rule Decisions

- The current Superposition implementation stores three possible values but no hidden resolved value. Observer Effect therefore previews the middle candidate rather than revealing a separately persisted secret.
- Like Tunneling and Superposition Collapse, Observer spends score because this rebuilt branch has no energy meter.
- Observer does not change the tile's `superpositionValues`, `value`, kind, element, or Entanglement bond.

## Tests Added

- `ObserverEffectEngineTest.observePreviewDoesNotChangeTileStateButSpendsScore`
- `ObserverEffectEngineTest.observeFailsClosedWhenScoreIsInsufficient`
- `ObserverEffectEngineTest.observeFailsClosedForStableTile`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 41s
24 actionable tasks: 5 executed, 19 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 3s
37 actionable tasks: 3 executed, 34 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Observer reveals a deterministic middle candidate, not a hidden persisted actual value.
- Observer spends score rather than energy because this branch has no persisted energy meter.
- No new instructional UI text was added; discovery is via long-press interaction and snackbar feedback.
