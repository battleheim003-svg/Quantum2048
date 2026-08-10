# Phase 18 Report - Statistics Dashboard

## Implemented

- Added persisted statistics fields to `GameState`:
  - `lowCollapseCount`
  - `highCollapseCount`
  - `totalWinEnergy`
  - `winEnergySamples`
  - `totalChainMergeCount`
- Added derived statistic helpers in `FusionRules`:
  - `collapseLowRatio(...)`
  - `averageWinEnergy(...)`
- Superposition Collapse now tracks low-value selections separately from higher-value selections.
- Winning records the current energy into the win-energy sample set.
- Moves with multiple merges add extra merges to `totalChainMergeCount`.
- Added a Statistics route and main-menu button.
- Added a Statistics screen showing:
  - low collapse ratio
  - average win energy
  - total chain merges
- Added string resources for the dashboard labels.
- Statistics fields round-trip through the existing snapshot.

## Rule Decisions

- Statistics are stored in `GameState` to keep the feature local-only and aligned with the existing snapshot persistence.
- "Low collapse" maps to selecting index `0` from a three-state Superposition Collapse. Other choices count as high collapse selections.
- Win energy is sampled only when a state transitions into `WON`, so repeated won-state updates do not duplicate the sample.
- Chain merge count stores only extra merges beyond the first merge in a swipe, matching the chain-bonus concept.

## Tests Added

- `StatisticsEngineTest.collapseRatioUsesLowAndHighCollapseCounts`
- `StatisticsEngineTest.collapseUpdatesLowHighCounters`
- `StatisticsEngineTest.winStoresAverageEnergySample`
- `StatisticsEngineTest.chainMergeCounterAddsExtraMergesOnly`
- `SnapshotTest.statisticsRoundTripPersists`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 46s
24 actionable tasks: 11 executed, 13 up-to-date
```

```text
.\gradlew.bat assembleDebug --console=plain
BUILD SUCCESSFUL in 4s
37 actionable tasks: 3 executed, 34 up-to-date
```

The unit test command emitted the existing Android SDK XML version warning, but completed successfully.

## Known Limitations

- Statistics are tied to the active game snapshot, not yet a global aggregate profile across all historical runs.
- The dashboard is intentionally compact and text-based; no charts were added in this phase.
