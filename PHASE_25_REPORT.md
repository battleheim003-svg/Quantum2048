# Phase 25 Report - Daily Best Profile History

## Scope
- Completed the next-step Phase 5 work for Daily Challenge persistence.
- Moved Daily best tracking from a snapshot-only concern into the persistent global profile.

## Implementation
- Added `dailyBestScores` to `ProfileState`.
- Added `ProfileState.dailyBestScore(date)` for safe lookups.
- `ProfileState.record(...)` now records the best score for each `dailyChallengeDate`.
- Daily records are monotonic:
  - lower same-day results do not overwrite a better score
  - higher scores update the date entry
  - different dates are tracked independently
- `GameViewModel.load(...)` now restores the current Daily best from `ProfileRepository` when loading or creating a Daily challenge.
- Statistics now shows today's Daily best from the global profile.
- Added English and Persian string resources for the new statistics row.

## Tests
- Added `ProfileStateTest.recordKeepsDailyBestScoreByDate`.

## Verification
- `.\gradlew.bat --no-daemon testDebugUnitTest --console=plain`
- `.\gradlew.bat assembleDebug --console=plain`

Both verification commands completed successfully.

## Notes
- Existing `GameState.dailyBestScore` remains as the active run/display value for compatibility.
- The profile now acts as the source of truth for historical Daily best scores.
