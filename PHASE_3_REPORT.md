# Phase 3 Report - Phase 0 Chemistry Data Model

## Implemented

- Added the engine-only chemistry model in `engine/Chemistry.kt`.
- Introduced distinct data concepts for board-origin elements and off-board compound recipes:
  - `ElementTile`
  - `CompoundRecipe`
  - `Compound`
  - `CompoundRecipeLevel`
- Added deterministic recipe matching through `Chemistry.findRecipe(...)`.
- Added a per-level recipe unlock model so Medium, Hard, and Quantum can expose different recipe sets in later phases.
- Added at least 10 real compound recipes to `QuantumBalance.defaultCompoundRecipes`:
  - H2O, NaCl, CO2, NH3, CH4, CaCO3, MgO, SiO2, Fe2O3, LiCl
- Extended `QuantumSpecies` with chemistry support elements required by the recipe table:
  - Na, Mg, Cl, Ca
- Restored engine-level Quantum superposition behavior so unresolved tiles are again supported by the core rules:
  - Quantum spawn can create unresolved particle tiles.
  - Unresolved tiles do not merge.
  - Auto-collapse is active again through `QuantumBalance`.

## Files Changed

- `app/src/main/java/com/battleheim/quantum2048/engine/Chemistry.kt`
- `app/src/main/java/com/battleheim/quantum2048/engine/Models.kt`
- `app/src/main/java/com/battleheim/quantum2048/engine/QuantumBalance.kt`
- `app/src/main/java/com/battleheim/quantum2048/engine/GameEngine.kt`
- `app/src/main/java/com/battleheim/quantum2048/ui/GameScreen.kt`
- `app/src/test/java/com/battleheim/quantum2048/engine/ChemistryTest.kt`
- `app/src/test/java/com/battleheim/quantum2048/engine/QuantumEngineTest.kt`
- `gradle.properties`

## Tests Added

- `ChemistryTest.matchingRecipeReturnsCompoundRegardlessOfInputOrder`
- `ChemistryTest.unmatchedElementsReturnNull`
- `ChemistryTest.levelFilterHidesRecipesLockedForLaterLevels`
- `ChemistryTest.defaultRecipeTableContainsAtLeastTenRealCompounds`
- Updated Quantum tests to cover unresolved particle spawn and non-merge behavior.

## Verification

Initial Gradle runs crashed because the local JVM reported insufficient native memory. The project Gradle settings were reduced from `-Xmx2g` to `-Xmx1024m`, capped metaspace, limited workers, and used in-process Kotlin compilation.

Final required commands:

```text
.\gradlew.bat --no-daemon testDebugUnitTest --console=plain
BUILD SUCCESSFUL in 30s
24 actionable tasks: 6 executed, 18 up-to-date
```

```text
.\gradlew.bat --no-daemon assembleDebug --console=plain
BUILD SUCCESSFUL in 16s
37 actionable tasks: 4 executed, 33 up-to-date
```

Both commands emitted the existing Android SDK XML version warning, but completed successfully.

## Deferred To Next Phase

- No Compound Lab UI was added in this phase.
- No drag behavior was added.
- No collection persistence was added.
- No difficulty enum or per-difficulty save migration was added yet.
- Phase 1 should wire `CompoundRecipe` matching into an atomic board transaction and undo flow.
