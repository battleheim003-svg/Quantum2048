# Phase 24 Report - Profile Reset Controls

## Scope
- Completed the next-step Phase 4 work for profile management.
- Added a player-facing way to clear global profile progress after Phase 22 moved achievements and statistics into a persistent profile store.

## Implementation
- Wired `ProfileRepository` into `SettingsScreen`.
- Added a Reset Profile action in Settings.
- Added a confirmation dialog before clearing profile data.
- `ProfileRepository.clear()` is now reachable from UI and clears:
  - unlocked achievements
  - global collapse counters
  - global win-energy statistics
  - global chain-merge statistics
- Added English and Persian string resources for the new reset flow.

## Verification
- `.\gradlew.bat --no-daemon testDebugUnitTest --console=plain`
- `.\gradlew.bat assembleDebug --console=plain`

Both verification commands completed successfully.

## Notes
- Existing reset actions for collection and per-difficulty saves remain unchanged.
- The confirmation dialog follows the same pattern as the existing collection and save reset controls.
