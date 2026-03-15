# Implementation Plan: Merge `main` into `compiler-plugin`

## Overview

Merge `main` branch (with multimodule nested tabs support) into `compiler-plugin` branch via a merge commit, then repair all compiler-plugin code to work with main's new `@TabItem(parent, ordinal)` child-to-parent tab annotation system.

**Strategy**: Main branch changes have priority. Merge → resolve 12 conflicts → fix semantic breaks in compiler plugin IR/FIR generators from changed tab APIs.

**Branch state**:
- **Merge-base**: `v0.3.5` (commit `4eebf12`)
- **Main**: 17 commits ahead (PR #42 multimodule-nested-tabs)
- **Compiler-plugin**: 12 commits ahead (Phases 1–5 + interchangeability)
- **Overlapping files**: ~80 files modified on both branches
- **Actual merge conflicts**: 12 files

## Requirements (validated with user)

- Merge main INTO compiler-plugin (not rebase)
- Resolve each of 12 conflicts individually
- Take main's version for all `.github` tooling files
- Take main's TabExtractor entirely for KSP (discard compiler-plugin's KSP TabExtractor changes)
- Full repair scope: update compiler plugin IR generators to support new tab system in this effort
- Deleted KSP tests: review case-by-case during implementation

## Key Architectural Change

Main **reversed the tab annotation pattern**:

| Aspect | OLD (compiler-plugin) | NEW (main) |
|--------|----------------------|------------|
| `@Tabs` | `@Tabs(name, initialTab, items=[...])` | `@Tabs(name)` |
| `@TabItem` | Marker annotation (no params) | `@TabItem(parent=KClass, ordinal=Int)` |
| Discovery | Parent lists children in `items` array | Children declare their parent via `parent` param |
| Initial tab | `initialTab` parameter on `@Tabs` | Tab with `ordinal = 0` |
| Cross-module | Not supported | Supported via classpath parent resolution |
| Nested tabs | Not supported | New `TabItemType.TABS` |

---

## Phase 1: Merge & Conflict Resolution

### Task 1.1: Execute merge

```bash
git merge main --no-ff -m "Merge main (multimodule-nested-tabs) into compiler-plugin"
```

This will produce 12 conflict files. Do NOT commit yet.

### Task 1.2: Resolve `.github` tooling conflicts (5 files)

**Strategy**: Take main's version for all.

| File | Resolution |
|------|-----------|
| `.github/agents/Architect.agent.md` | `git checkout main -- .github/agents/Architect.agent.md` |
| `.github/agents/Developer.agent.md` | `git checkout main -- .github/agents/Developer.agent.md` |
| `.github/agents/Simple-Architect.agent.md` | `git checkout main -- .github/agents/Simple-Architect.agent.md` |
| `.github/agents/Simple-Developer.agent.md` | `git checkout main -- .github/agents/Simple-Developer.agent.md` |
| `.github/prompts/implementation.prompt.md` | `git checkout main -- .github/prompts/implementation.prompt.md` |

### Task 1.3: Resolve `CHANGELOG.md`

Both branches added entries. Manually merge — keep main's entries in chronological order, then append compiler-plugin entries below. Main's version takes priority for any overlapping entries.

### Task 1.4: Resolve `docs/site/src/data/searchData.json`

Accept main's version as base, then add compiler-plugin documentation entries (COMPILER-PLUGIN.md, MIGRATION.md search entries).

### Task 1.5: Resolve `TabAnnotations.kt`

**File**: `quo-vadis-annotations/.../TabAnnotations.kt`

**Resolution**: Accept main's version entirely. Main's `@Tabs(name)` + `@TabItem(parent, ordinal)` signatures are the new standard.

```bash
git checkout main -- quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt
```

### Task 1.6: Resolve `TreeMutatorPaneTest.kt`

**File**: `quo-vadis-core/src/commonTest/.../TreeMutatorPaneTest.kt`

Inspect the conflict. Main added new pane test cases; compiler-plugin may have modified existing ones. Keep both additions where possible; for direct conflicts, main's test logic wins.

### Task 1.7: Resolve demo app conflicts (3 files)

| File | Strategy |
|------|----------|
| `composeApp/.../DI.kt` | Main's DI setup as base; re-add any compiler-plugin-specific providers |
| `composeApp/.../StateDrivenDemoDestination.kt` | Main's version as base; layer back compiler-plugin additions |
| `composeApp/.../MainTabsUI.kt` | Main's version as base; re-add compiler-plugin UI changes |

### Task 1.8: Stage resolved conflicts and commit

```bash
git add -A
git commit -m "Merge main (multimodule-nested-tabs) into compiler-plugin"
```

---

## Phase 2: Annotation & Data Model Alignment

These are the foundation changes that everything downstream depends on.

### Task 2.1: Update `NavigationMetadata.kt` (compiler plugin data model)

**File**: `quo-vadis-compiler-plugin/.../common/NavigationMetadata.kt`

Changes:
- `TabsMetadata`: Remove `initialTab: ClassId?` field
- `TabItemMetadata`: Add `ordinal: Int` field
- `TabItemType` enum: Rename `NESTED_STACK` → `STACK`, `FLAT_SCREEN` → `DESTINATION`, add `TABS`

**Acceptance criteria**: Compiles, all downstream references updated (will break temporarily until Phase 3).

### Task 2.2: Verify `TabInfo.kt` (KSP data model) matches main

**File**: `quo-vadis-ksp/.../models/TabInfo.kt`

After merge, verify this file matches main's version:
- `TabInfo`: Should have no `initialTabClass`, no `isNewPattern`, should have `isCrossModule`
- `TabItemInfo`: Should have `ordinal: Int`
- `TabItemType`: Should have `STACK`, `DESTINATION`, `TABS`

If auto-merge left issues, fix to match main exactly.

### Task 2.3: Verify `TabAnnotations.kt` is main's version

Confirm the conflict resolution from Phase 1 left the correct annotation signatures:
- `@Tabs(val name: String)` — only name parameter
- `@TabItem(val parent: KClass<*>, val ordinal: Int)` — parent + ordinal

---

## Phase 3: Compiler Plugin FIR/IR Tab Processing Rewrite

This is the core effort — adapting the K2 compiler plugin IR generators to the new child-to-parent pattern.

### Task 3.1: Add `getIntArgument()` helper to `IrMetadataCollector`

**File**: `quo-vadis-compiler-plugin/.../ir/IrMetadataCollector.kt`

Add an integer argument extraction helper following the existing `getStringArgument`/`getBooleanArgument` patterns. This is needed to read `@TabItem(ordinal = N)`.

```kotlin
private fun IrConstructorCall.getIntArgument(index: Int): Int? {
    val expr = getValueArgument(index) ?: return null
    return (expr as? IrConst)?.value as? Int
}
```

### Task 3.2: Add `intArgument()` helper to FIR `AnnotationExtractor.kt`

**File**: `quo-vadis-compiler-plugin/.../fir/AnnotationExtractor.kt`

Add:
```kotlin
fun FirAnnotation.intArgument(name: String): Int? {
    val argument = argumentMapping.mapping[Name.identifier(name)] ?: return null
    return (argument as? FirLiteralExpression)?.value as? Int
}
```

### Task 3.3: Rewrite `IrMetadataCollector.processTabs()` — child-to-parent discovery

**File**: `quo-vadis-compiler-plugin/.../ir/IrMetadataCollector.kt`

This is the **most significant change**. The current `processTabs()` reads `@Tabs(items=[...])` and iterates the items array. Must be rewritten to:

1. **Extend Pass 1 (or add new pre-pass)**: During class indexing, also collect all `@TabItem`-annotated classes. For each, read:
   - `parent: KClass<*>` at argument index 0 → resolve to `ClassId`
   - `ordinal: Int` at argument index 1
   - Determine `TabItemType` by checking if the class also has `@Tabs` (→ `TABS`), `@Stack` (→ `STACK`), or is a plain `@Destination` (→ `DESTINATION`)
   
2. **Build a map**: `Map<ClassId, List<TabItemMetadata>>` keyed by parent class

3. **Rewrite `processTabs()`**: For each `@Tabs`-annotated class:
   - Read `name` from argument index 0 (unchanged)
   - Look up children from the pre-built `@TabItem` map using the tabs class's `ClassId` as key
   - Sort children by `ordinal`
   - No longer read `initialTab` or `items` arguments

4. **Update `TabsMetadata` construction**: Remove `initialTab` parameter, use sorted items list

**Dependencies**: Task 2.1 (updated `NavigationMetadata.kt` data model)

### Task 3.4: Update `BaseConfigIrGenerator.kt` tab registration

**File**: `quo-vadis-compiler-plugin/.../ir/generators/BaseConfigIrGenerator.kt`

Changes:
- Replace `TabItemType.NESTED_STACK` → `TabItemType.STACK` references
- Replace `TabItemType.FLAT_SCREEN` → `TabItemType.DESTINATION` references
- Handle new `TabItemType.TABS` case (nested tabs → generates nested `registerTabsContainer` call)
- Remove `initialTabIndex` computation from `tab.initialTab` — hardcode `irInt(0)` or compute from ordinal=0 position
- Ensure tab items are emitted in ordinal-sorted order

**Dependencies**: Task 2.1, Task 3.3

### Task 3.5: Verify `ContainerRegistryIrGenerator.kt` compatibility

**File**: `quo-vadis-compiler-plugin/.../ir/generators/ContainerRegistryIrGenerator.kt`

After upstream changes, verify this file compiles and handles the renamed `TabItemType` enum values. If `TabItemType.TABS` requires special container dispatch, add handling.

**Dependencies**: Task 2.1

### Task 3.6: Verify `NormalizedContainerBindings.kt` compatibility

**File**: `quo-vadis-compiler-plugin/.../common/NormalizedContainerBindings.kt`

Quick verification — this file operates on ClassIds, not annotation arguments. Should work after model changes.

---

## Phase 4: FIR Checker Updates

### Task 4.1: Add `TabItemChecker` for ordinal/parent validation

**File**: New file or extend `StructuralChecker.kt`

Add FIR-level validation for the new `@TabItem` pattern:
- Every `@TabItem` must reference a valid `@Tabs`-annotated class as `parent`
- No duplicate `ordinal` values within the same parent
- At least one `ordinal = 0` must exist for each parent
- Detect circular nesting (`@Tabs` A → `@TabItem` B → `@Tabs` B → `@TabItem` A)
- Register new diagnostics in `QuoVadisDiagnostics.kt`

Follow existing checker patterns (`ArgumentParityChecker`, `StructuralChecker`).

**Note**: Main's KSP ValidationEngine already has these validations (`validateOrdinalZeroExists`, `validateOrdinalCollisions`, `validateOrdinalContinuity`, `validateCircularTabNesting`, `validateTabNestingDepth`). Mirror the same rules.

### Task 4.2: Register new checker in `QuoVadisAdditionalCheckersExtension`

Add the new `TabItemChecker` to the checker registration.

---

## Phase 5: KSP Module Alignment

### Task 5.1: Verify `TabExtractor.kt` is main's version

**File**: `quo-vadis-ksp/.../extractors/TabExtractor.kt`

The auto-merge may have produced a hybrid. Verify it matches main's child-to-parent implementation exactly. If not, replace with main's version:

```bash
git show main:quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt > quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt
```

### Task 5.2: Verify `ValidationEngine.kt` matches main

**File**: `quo-vadis-ksp/.../validation/ValidationEngine.kt`

Main refactored validation significantly (new ordinal-based validations, removed `resolver` parameter). Verify auto-merge result is correct.

### Task 5.3: Verify `ContainerBlockGenerator.kt` compatibility

**File**: `quo-vadis-ksp/.../generators/dsl/ContainerBlockGenerator.kt`

Check that auto-merged result correctly uses `TabItemType.STACK`/`DESTINATION`/`TABS` (not old enum names) and doesn't reference `initialTab` logic.

### Task 5.4: Verify `NavigationConfigGenerator.kt` compatibility

**File**: `quo-vadis-ksp/.../generators/dsl/NavigationConfigGenerator.kt`

Check auto-merged result for compatibility with new tab patterns.

### Task 5.5: Review deleted KSP tests (case-by-case)

Evaluate each deleted test file:

| Deleted Test | Recommendation |
|-------------|----------------|
| `ContainerBlockGeneratorTabEntryTest` | Likely needs full rewrite for new tab pattern — **restore if tab entry generation is non-trivial** |
| `TabItemTypeTest` | Enum values changed — **restore with updated enum names** |
| `FakeKspTypes` | Test utility — **restore if other tests need it** |
| `ValidationEngineContainerReferenceTest` | Validation logic changed — **restore with main's validation rules** |
| `ValidationEngineOrdinalTest` | Main added ordinal validation — **check if main already has equivalent tests** |

For each: check if main already provides equivalent test coverage. If so, skip restoration.

---

## Phase 6: Test Source & Test Updates (Compiler Plugin)

### Task 6.1: Update `TestSources.kt` tab test snippets

**File**: `quo-vadis-compiler-plugin/.../testing/TestSources.kt`

Update ALL tab-related test source regions to use new pattern:

| Region | Old Pattern | New Pattern |
|--------|------------|------------|
| Region 3 (`tabsWithItems`) | `@Tabs(name, initialTab, items=[...])` + `@TabItem` marker | `@Tabs(name)` + `@TabItem(parent, ordinal)` |
| Region 4 (mixed tabs) | Same old pattern | Same new pattern |
| Region 6 (full graph) | Same old pattern for `AppTabs` | Same new pattern |
| Region 14 (empty tabs) | `@Tabs(name, items=[])` | `@Tabs(name)` — no items to list |
| Region 17 (scoped) | Same old pattern for `ScopedTabs` | Same new pattern |

**Template change per tab item**:
```kotlin
// OLD:
@TabItem
@Stack(name = "homeTab", startDestination = HomeScreen::class)
sealed class HomeTab : NavDestination { ... }

// NEW:
@TabItem(parent = MainTabs::class, ordinal = 0)
@Stack(name = "homeTab", startDestination = HomeScreen::class)
sealed class HomeTab : NavDestination { ... }
```

**Template change per tabs container**:
```kotlin
// OLD:
@Tabs(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ProfileTab::class])
object MainTabs

// NEW:
@Tabs(name = "mainTabs")
object MainTabs
```

### Task 6.2: Update `IrCodegenTests.kt` tab tests

**File**: `quo-vadis-compiler-plugin/.../ir/IrCodegenTests.kt`

After TestSources update:
- Verify tab compilation tests still pass
- Update any assertions that check for `initialTab` in generated output
- Add test for nested tabs (`TabItemType.TABS`) if main supports it

### Task 6.3: Update `FirDiagnosticTests.kt`

**File**: `quo-vadis-compiler-plugin/.../fir/FirDiagnosticTests.kt`

- Update existing tab diagnostic tests to use new annotation pattern
- Add new test cases:
  - Duplicate ordinal within same parent → error
  - Missing ordinal=0 for a parent → error
  - Invalid parent reference (non-`@Tabs` class) → error
  - Circular nesting → error (if FIR checker added in Phase 4)

---

## Phase 7: Demo App & Documentation

### Task 7.1: Fix `MainTabs.kt` tab annotations

**File**: `composeApp/.../destinations/MainTabs.kt`

Convert all tab items to new pattern:
```kotlin
@Tabs(name = "mainTabs")
sealed class MainTabs : NavDestination {
    @TabItem(parent = MainTabs::class, ordinal = 0)
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem(parent = MainTabs::class, ordinal = 1)
    @Stack(name = "exploreTabStack", startDestination = ExploreScreen::class)
    sealed class ExploreTab : MainTabs() { ... }

    @TabItem(parent = MainTabs::class, ordinal = 2)
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()

    @TabItem(parent = MainTabs::class, ordinal = 3)
    @Stack(name = "settingsTabStack", startDestination = SettingsScreen::class)
    sealed class SettingsTab : MainTabs() { ... }
}
```

### Task 7.2: Fix `TabsDestination.kt` if affected

**File**: `composeApp/.../destinations/TabsDestination.kt`

If this file uses `@Tabs` with old pattern, convert similarly.

### Task 7.3: Update `feature1`/`feature2` if affected

Check if feature modules use tab annotations. If so, convert to new pattern.

### Task 7.4: Update `COMPILER-PLUGIN.md` documentation

**File**: `docs/COMPILER-PLUGIN.md`

Update any annotation examples to show new `@TabItem(parent, ordinal)` pattern.

### Task 7.5: Update `MIGRATION.md`

**File**: `docs/MIGRATION.md`

Add a section about the tab annotation pattern change if not already covered.

---

## Phase 8: Build Verification

### Task 8.1: Compile all modules

```bash
./gradlew assemble
```

Fix any compilation errors from the model/enum renames cascading through the codebase.

### Task 8.2: Run all tests

```bash
./gradlew test
```

Fix failing tests. Expected failures:
- Compiler plugin tests (updated in Phase 6)
- KSP tests (if any were broken by auto-merge)

### Task 8.3: Run detekt

```bash
./gradlew detekt
```

Update detekt baselines if needed.

### Task 8.4: Run e2e compiler plugin tests

```bash
./scripts/e2e-compiler-plugin.sh
```

### Task 8.5: Build demo app

```bash
./gradlew :composeApp:assembleDebug
```

Verify the demo app compiles and runs with the new tab pattern.

---

## Sequencing Diagram

```
Phase 1: Merge & Conflicts
  ├── 1.1 Execute merge
  ├── 1.2-1.7 Resolve 12 conflicts
  └── 1.8 Commit merge
          │
Phase 2: Data Models (foundation)
  ├── 2.1 NavigationMetadata.kt (compiler-plugin)
  ├── 2.2 TabInfo.kt (KSP) — verify
  └── 2.3 TabAnnotations.kt — verify
          │
Phase 3: Compiler Plugin IR/FIR ─────────────┐
  ├── 3.1 getIntArgument() helper             │
  ├── 3.2 FIR intArgument() helper            │
  ├── 3.3 IrMetadataCollector rewrite  ◄──────┤
  ├── 3.4 BaseConfigIrGenerator update        │
  ├── 3.5 ContainerRegistryIrGenerator verify │
  └── 3.6 NormalizedContainerBindings verify  │
          │                                    │
Phase 4: FIR Checkers                         │
  ├── 4.1 TabItemChecker (new)                │
  └── 4.2 Register checker                    │
          │                                    │
Phase 5: KSP Alignment ──────────────────────┘
  ├── 5.1 TabExtractor.kt — verify/replace
  ├── 5.2 ValidationEngine.kt — verify
  ├── 5.3 ContainerBlockGenerator — verify
  ├── 5.4 NavigationConfigGenerator — verify
  └── 5.5 Review deleted tests
          │
Phase 6: Tests
  ├── 6.1 TestSources.kt
  ├── 6.2 IrCodegenTests.kt
  └── 6.3 FirDiagnosticTests.kt
          │
Phase 7: Demo & Docs
  ├── 7.1-7.3 Demo app fixes
  └── 7.4-7.5 Documentation
          │
Phase 8: Verification
  ├── 8.1 ./gradlew assemble
  ├── 8.2 ./gradlew test
  ├── 8.3 ./gradlew detekt
  ├── 8.4 e2e-compiler-plugin.sh
  └── 8.5 :composeApp:assembleDebug
```

## Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| `registerTabsContainerFun` IR signature mismatch after main's changes | High — compiler plugin generates IR calls to core API | Verify function signatures in core `DslNavigationConfig`/`NavigationConfigBuilder` before generating IR |
| `CompositeNavigationConfig.mergeTabNodes()` incompatibility with compiler plugin output | Medium — aggregated configs may fail to compose | Test with multi-module demo (feature1 + feature2 + composeApp) |
| Auto-merged KSP files have subtle semantic errors | Medium — tests may pass but behavior wrong | Run full KSP-based demo build separately to verify |
| Circular nesting detection in FIR vs. runtime | Low — edge case | Mirror KSP `ValidationEngine`'s cycle detection logic exactly |

## Open Questions

1. Does the compiler plugin's `MultiModuleDiscovery.kt` need updates for main's cross-module tab resolution approach?
2. Should the `AggregatedConfigIrGenerator` emit `mergeTabNodes()` calls for cross-module tab support?
3. Are there any `@Volatile` or thread-safety changes from main that affect compiler plugin's IR generation?
