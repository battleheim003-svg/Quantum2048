# Phase 5 Report - Phase 3 Collection / Codex

## Implemented

- Added a global collection model independent of active game saves.
- Added `CollectionEntry` with:
  - compound symbol
  - English/Persian names
  - first discovery timestamp
  - last discovery timestamp
  - first discovery difficulty
  - last discovery difficulty
  - discovery counter
- Added immutable `CollectionState.record(...)` reducer.
- Repeated compounds now increment `discoveryCount` and update last discovery metadata instead of creating duplicate records.
- Added `CollectionState.codex(...)` to produce discovered and locked codex rows from the full recipe table.
- Added `CollectionRepository` domain contract.
- Added `DataStoreCollectionRepository` using a separate Preferences DataStore:
  - store name: `collection_state_v1`
  - key: `collection_snapshot_v1`
- Added `CollectionSnapshot` for JSON persistence.
- Exposed `collectionRepository` from `QuantumApp` for future Compound Lab and Collection UI integration.

## Files Changed

- `app/src/main/java/com/battleheim/quantum2048/domain/CollectionRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/data/CollectionSnapshot.kt`
- `app/src/main/java/com/battleheim/quantum2048/data/DataStoreCollectionRepository.kt`
- `app/src/main/java/com/battleheim/quantum2048/QuantumApp.kt`
- `app/src/test/java/com/battleheim/quantum2048/data/CollectionSnapshotTest.kt`

## Tests Added

- `CollectionSnapshotTest.repeatedCompoundIncrementsCounterWithoutAddingDuplicateEntry`
- `CollectionSnapshotTest.collectionSnapshotPersistsBetweenJsonSessions`
- `CollectionSnapshotTest.codexIncludesLockedEntriesForUndiscoveredRecipes`

## Verification

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 34s
24 actionable tasks: 11 executed, 13 up-to-date
```

```text
.\gradlew.bat --no-daemon assembleDebug --console=plain
BUILD SUCCESSFUL in 17s
37 actionable tasks: 5 executed, 32 up-to-date
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Deliberately Deferred

- No Collection screen UI was added because the navigation shell belongs to the later app-shell phase.
- No Compound Lab transaction writes to the collection yet because the drag/combine mechanic is still deferred.
- Reset collection UI and confirmation dialogs belong to Settings in the app-shell phase.
