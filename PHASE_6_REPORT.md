# Phase 6 Report - Phase 4 App Shell / Navigation

## Implemented

- Added Compose Navigation dependency.
- Added `QuantumAppShell` with a navigation graph:
  - `MainMenu`
  - `LevelSelect`
  - `Game(difficulty)`
  - `Collection`
  - `Settings`
  - `Pause`
- Added Main Menu actions:
  - Continue, enabled when any saved difficulty exists.
  - New game -> Level Select.
  - Collection.
  - Settings.
- Added Level Select with four difficulty cards:
  - Easy
  - Medium
  - Hard
  - Quantum
- Added Collection screen backed by the global collection repository and codex locked/discovered rows.
- Added Settings screen:
  - Sound toggle
  - Music toggle
  - Haptics toggle
  - Reset collection confirmation
  - Reset per-difficulty progress confirmation
- Added Settings persistence through `SettingsRepository` and `DataStoreSettingsRepository`.
- Added Pause screen:
  - Resume
  - Main menu
  - Restart current difficulty with confirmation
- Converted `MainActivity` to launch the app shell instead of directly launching `GameScreen`.
- Converted `GameScreen` into a parameterized navigation destination with a Pause action.
- Removed the old in-game Classic/Quantum selector so level choice lives in Level Select.
- Added Android UI navigation test source for:
  - MainMenu -> LevelSelect -> Game -> Pause -> MainMenu

## Files Changed

- `app/build.gradle.kts`
- `app/src/main/java/com/battleheim/quantum2048/MainActivity.kt`
- `app/src/main/java/com/battleheim/quantum2048/QuantumApp.kt`
- `app/src/main/java/com/battleheim/quantum2048/data/DataStoreSettingsRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/domain/SettingsRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/AppShell.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameViewModel.kt`
- `app/src/androidTest/java/com/battleheim/quantum2048/ui/AppShellNavigationTest.kt`

## Tests Added

- `AppShellNavigationTest.mainMenuLevelSelectGamePauseMainMenu`

This is an Android instrumentation UI test and needs a device/emulator to execute.

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 35s
24 actionable tasks: 7 executed, 17 up-to-date
```

```text
.\gradlew.bat --no-daemon assembleDebug --console=plain
BUILD SUCCESSFUL in 24s
37 actionable tasks: 7 executed, 30 up-to-date
```

Attempted Android UI test package compilation:

```text
.\gradlew.bat --no-daemon assembleDebugAndroidTest --console=plain
```

The first run timed out, and the retry failed because the local JVM could not allocate native memory. Windows reported the paging file was too small. The app unit tests and debug APK build both completed successfully.

## Deliberately Deferred

- Language switching was deferred because `values-fa/strings.xml` is currently mojibake/corrupted and needs a localization repair pass first.
- Compound Lab drag mechanics are still not implemented because Phase 1 was skipped by request.
- Design polish is deferred to Phase 5.
- Movement/merge/collapse polish, sound effects, haptics, and end-game summary are deferred to Phase 6.
