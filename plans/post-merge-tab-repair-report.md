# Post-Merge Tab Repair Report

**Date**: 15 March 2026  
**Context**: After merging `main` → `compiler-plugin`, identify every file that assumes the OLD `@Tabs(items=[...])` / `@TabItem` (marker) pattern and must be updated to the NEW `@TabItem(parent=KClass, ordinal=Int)` child-to-parent pattern.

---

## Executive Summary

The compiler-plugin branch was built against **`@Tabs(name, initialTab, items)` + `@TabItem` (marker)**. Main replaced this with **`@Tabs(name)` + `@TabItem(parent, ordinal)`**. The merge will auto-resolve many non-tab files cleanly, but every file that reads, validates, models, generates, or tests tab annotations needs repair.

**Total files requiring repair**: 13  
**Estimated total effort**: Significant — the tab system touches the full pipeline from annotation definition → FIR extraction → IR metadata collection → IR code generation → tests → demo app.

---

## 1. Compiler Plugin Source Files

### 1.1 `NavigationMetadata.kt` — Data Model

**File**: `quo-vadis-compiler-plugin/.../common/NavigationMetadata.kt`  
**Lines affected**: 63–76 (`TabsMetadata`, `TabItemMetadata`, `TabItemType`)

**Old pattern assumptions**:
- `TabsMetadata` has `initialTab: ClassId?` — must be **removed** (ordinal=0 is initial tab)
- `TabsMetadata` has `items: List<TabItemMetadata>` — field name is fine but semantics change
- `TabItemMetadata` has only `classId` and `type` — must add `ordinal: Int`
- `TabItemType` enum has `NESTED_STACK` / `FLAT_SCREEN` — must rename to `STACK` / `DESTINATION` and add `TABS`

**Specific changes needed**:
```
TabsMetadata:
  - REMOVE: initialTab: ClassId?
  - KEEP:   items: List<TabItemMetadata>  (rename to `tabs` for consistency with KSP model — optional)

TabItemMetadata:
  + ADD:    ordinal: Int

TabItemType:
  - RENAME: NESTED_STACK → STACK
  - RENAME: FLAT_SCREEN → DESTINATION
  + ADD:    TABS
```

**Complexity**: **Trivial** — mechanical data class edits. But changes cascade to all consumers.

---

### 1.2 `IrMetadataCollector.kt` — Tab IR Extraction

**File**: `quo-vadis-compiler-plugin/.../ir/IrMetadataCollector.kt`  
**Lines affected**: 88–103 (Pass 3 call), 200–248 (`processTabs` method)

**Old pattern assumptions**:
- **`processTabs()`** (line 200) reads from the `@Tabs` annotation at **positional indices**:
  - Index 0: `name: String` ✅ (unchanged)
  - Index 1: `initialTab: KClass<*>` ❌ **removed on main**
  - Index 2: `items: Array<KClass<*>>` ❌ **removed on main**
- Tab items are determined from the `items` array on `@Tabs` — this is the **core old pattern**
- `@TabItem` is only used as a marker (never read for arguments)

**Required changes**:
1. **Reverse the discovery direction**: Instead of reading `items` from `@Tabs`, scan all classes for `@TabItem` annotations and read `parent: KClass<*>` (index 0) and `ordinal: Int` (index 1)
2. **Add a new pass or extend Pass 1**: Collect all `@TabItem`-annotated classes, group by `parent` ClassId
3. **Rewrite `processTabs()`**: For each `@Tabs`-annotated class, look up its children from the collected `@TabItem` map, sorted by ordinal
4. **Remove `initialTab` extraction**: Tab at ordinal=0 is the initial tab
5. **Remove `getClassArrayArgument(2)` call**: No more `items` array
6. **Add `getIntArgument()` helper** for reading `ordinal` (currently missing — only string/boolean/class/enum/classArray helpers exist)
7. **Detect `TABS` item type**: A `@TabItem`-annotated class that itself has `@Tabs` is type `TABS` (nested tabs)

**Complexity**: **Significant rewrite** — the entire tab discovery algorithm changes from parent-to-child to child-to-parent. The multi-pass architecture (Pass 1 → index, Pass 3 → tabs) needs restructuring.

---

### 1.3 `AnnotationExtractor.kt` — FIR Annotation Helpers

**File**: `quo-vadis-compiler-plugin/.../fir/AnnotationExtractor.kt`  
**Lines affected**: N/A (additive only)

**Old pattern assumptions**: None of the existing helpers are wrong, but:
- Missing `intArgument(name)` method needed for reading `@TabItem(ordinal = N)`
- The FIR annotation extractors are only used by FIR checkers (not by `IrMetadataCollector` which has its own IR-level helpers)

**Required changes**:
- Add `fun FirAnnotation.intArgument(name: String): Int?` for reading integer annotation arguments
- This is needed by FIR checkers that will validate `@TabItem` ordinals

**Complexity**: **Trivial** — single method addition following existing pattern.

---

### 1.4 `QuoVadisPredicates.kt` — FIR Predicates

**File**: `quo-vadis-compiler-plugin/.../fir/QuoVadisPredicates.kt`

**Old pattern assumptions**: None — `TAB_ITEM_FQN` and `TABS_FQN` are already defined correctly and match the same annotation class names (the annotation class names didn't change, only their parameters). `HAS_TAB_ITEM` predicate is also present.

**Required changes**: None.

**Complexity**: **None** — already correct.

---

### 1.5 `BaseConfigIrGenerator.kt` — IR Code Generation for Tab Registration

**File**: `quo-vadis-compiler-plugin/.../ir/generators/BaseConfigIrGenerator.kt`  
**Lines affected**: 130–195 (tab registration loop)

**Old pattern assumptions**:
- Reads `tab.items` and maps to `TabItemType.NESTED_STACK` for boolean list (line 151)
- Reads `tab.initialTab` to compute `initialTabIndex` (line 156–157)
- Passes `initialTabIndex` as argument 6 to `registerTabsContainerFun` (line 168)
- Tab ordering comes from the `items` array order

**Required changes**:
1. Replace `TabItemType.NESTED_STACK` → `TabItemType.STACK` (enum rename)
2. Remove `initialTabIndex` computation from `tab.initialTab` — ordinal=0 is always initial (hardcode `irInt(0)`)
3. Ensure items are sorted by ordinal before generating registration calls
4. Handle new `TabItemType.TABS` case in the boolean/null-instance mapping

**Complexity**: **Moderate** — straightforward enum substitutions and removal of `initialTab` logic, but must verify the `registerTabsContainerFun` signature on the core side matches.

---

### 1.6 `ContainerRegistryIrGenerator.kt` — Container Registry Code Gen

**File**: `quo-vadis-compiler-plugin/.../ir/generators/ContainerRegistryIrGenerator.kt`

**Old pattern assumptions**: This file operates on `ValidatedTabsContainerBinding` which is downstream of tab metadata. It doesn't directly read tab annotations.

**Required changes**: None directly, but if `TabItemType.TABS` requires different container registry dispatch logic, this would need updating.

**Complexity**: **Likely none** — indirect dependency. Verify after upstream fixes.

---

### 1.7 `NormalizedContainerBindings.kt` — Tab/Pane Binding Normalization

**File**: `quo-vadis-compiler-plugin/.../common/NormalizedContainerBindings.kt`  
**Lines affected**: 24–52

**Old pattern assumptions**: References `tabsMetadata.name`, `tabsMetadata.classId` — both still valid after the model change. No reference to `initialTab` or `items` directly.

**Required changes**: None — already operates on normalized ClassId keys.

**Complexity**: **None**.

---

### 1.8 FIR Checkers — Structural & Diagnostic Validation

**Files affected**:
- `StructuralChecker.kt` — validates `@Destination` inside container hierarchies. References `TABS_CLASS_ID` in `CONTAINER_ANNOTATIONS`. **No change needed** — still correctly identifies `@Tabs` containers.
- `ContainerRoleChecker.kt` — pane-only. **No change needed**.
- **New checker needed**: Ordinal validation for `@TabItem` — must verify:
  - Every `@TabItem` has `parent` referencing a valid `@Tabs` class
  - No duplicate ordinals within the same parent
  - At least one `ordinal=0` per parent (initial tab)
  - No circular nesting (tabs within tabs referencing each other)

**Required changes**: Add a new `TabItemChecker` FIR checker (or extend `StructuralChecker`) to validate the new `@TabItem(parent, ordinal)` semantics.

**Complexity**: **Moderate** — new validation logic, but follows existing checker patterns.

---

## 2. Compiler Plugin Test Files

### 2.1 `TestSources.kt` — Test Source Snippets

**File**: `quo-vadis-compiler-plugin/.../testing/TestSources.kt`  
**Lines affected**: ALL tab-related source snippets — regions 3, 4 (mixed tabs), 6 (full graph), 14 (empty tabs), 17 (scoped destinations)

**Old pattern assumptions** — every test source uses the OLD annotation pattern:

```kotlin
// OLD (regions 3, 4, 6, 14, 17):
@TabItem          // ← marker annotation, no params
@Stack(name = "homeTab", ...)
sealed class HomeTab : NavDestination { ... }

@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,        // ← REMOVED on main
    items = [HomeTab::class, ...]       // ← REMOVED on main
)
object MainTabs
```

**Must become**:
```kotlin
// NEW:
@TabItem(parent = MainTabs::class, ordinal = 0)    // ← child declares parent + ordinal
@Stack(name = "homeTab", ...)
sealed class HomeTab : NavDestination { ... }

@Tabs(name = "mainTabs")     // ← only name param
object MainTabs
```

**Specific regions to update**:
| Region | Name | Change scope |
|--------|------|-------------|
| 3 | `tabsWithItems` | Rewrite `@TabItem` → `@TabItem(parent=MainTabs::class, ordinal=N)`, strip `initialTab`/`items` from `@Tabs` |
| 4 (mixed tabs) | `mixedTabsWithStackBackedItem` | Same pattern rewrite |
| 6 | `fullNavigationGraph` | Same pattern rewrite for `AppTabs` + tab items |
| 14 | `emptyTabs` | Remove `items = []` — just `@Tabs(name = "emptyTabs")` |
| 17 | `scopedDestinations` | Same pattern rewrite for `ScopedTabs` |

**Complexity**: **Moderate** — mechanical but many snippets to update, and must ensure test assertions still pass.

---

### 2.2 `IrCodegenTests.kt` — IR Codegen Integration Tests

**File**: `quo-vadis-compiler-plugin/.../ir/IrCodegenTests.kt`  
**Lines affected**: 188–215 (tab test cases)

**Old pattern assumptions**: Tests compile `TestSources.tabsWithItems` and `TestSources.mixedTabsWithStackBackedItem` and verify destination classes are loadable.

**Required changes**:
- After `TestSources` is updated, these tests should still work structurally
- May need assertion updates if the generated config structure changes (e.g., no `initialTab` in metadata)
- Verify that the generated `registerTabsContainer` call signature matches the updated core API

**Complexity**: **Trivial to Moderate** — depends on how much the generated output shape changes.

---

### 2.3 `FirDiagnosticTests.kt` — FIR Diagnostic Tests

**File**: `quo-vadis-compiler-plugin/.../fir/FirDiagnosticTests.kt`  
**Lines affected**: 81–137 (tab diagnostic tests)

**Old pattern assumptions**:
- `tabs without wrapper produce actionable error` — uses `TestSources.tabsWithItems`
- `valid tabs with items and wrapper produce no error` — uses `TestSources.tabsWithItems` + `tabsContainerWrapper`
- Error messages reference "missing @TabsContainer wrapper for @Tabs container" and "test.MainTabs"

**Required changes**:
- After `TestSources` is updated, these tests should pass with the same diagnostic messages
- May need NEW test cases for ordinal validation diagnostics (duplicate ordinals, missing ordinal=0, invalid parent reference)
- The `emptyTabs` test behavior may change — main treats empty tabs as a warning (children may be in downstream modules), not an error

**Complexity**: **Moderate** — need new diagnostic test cases and potentially changed expectations.

---

## 3. KSP Module Files

### 3.1 `TabExtractor.kt` — KSP Tab Extraction

**File**: `quo-vadis-ksp/.../extractors/TabExtractor.kt`

**Old pattern assumptions**: **Completely built around old pattern**:
- Looks for `items` array argument on `@Tab`/`@Tabs`
- Has `extractFromItemsArray()` and `extractFromSealedSubclasses()` for dual-pattern support
- Has `resolveInitialTabClass()` reading `initialTab` KClass and `initialTabLegacy` String
- Has `tabItemCache` for legacy nested pattern
- Supports both `@Tab` and `@Tabs` annotation names

**Required changes**: **Complete rewrite** to match main's approach:
1. Phase 1: Scan all `@TabItem`-annotated classes, read `parent` and `ordinal`
2. Phase 2: Scan all `@Tabs`-annotated classes, match children from Phase 1
3. Remove `extractFromItemsArray`, `extractFromSealedSubclasses`, `resolveInitialTabClass`, `tabItemCache`
4. Remove `@Tab` support (only `@Tabs`)
5. Add cross-module resolution (`resolver.getClassDeclarationByName` for classpath parents)

**Complexity**: **Significant rewrite** — main already did this rewrite. The best approach is to adopt main's version, then ensure compatibility with compiler-plugin-specific features if any.

---

### 3.2 `TabInfo.kt` — KSP Tab Data Model

**File**: `quo-vadis-ksp/.../models/TabInfo.kt`

**Old pattern assumptions**:
- `TabInfo` has `initialTabClass: KSClassDeclaration?` — **removed on main**
- `TabInfo` has `isNewPattern: Boolean` — **removed on main** (no legacy pattern)
- `TabItemInfo` lacks `ordinal: Int` — **added on main**
- `TabItemType` has `FLAT_SCREEN` / `NESTED_STACK` — **renamed** to `DESTINATION` / `STACK`, plus new `TABS`

**Required changes**:
```
TabInfo:
  - REMOVE: initialTabClass
  - REMOVE: isNewPattern
  + ADD:    isCrossModule: Boolean = false

TabItemInfo:
  + ADD:    ordinal: Int

TabItemType:
  - RENAME: FLAT_SCREEN → DESTINATION
  - RENAME: NESTED_STACK → STACK
  + ADD:    TABS
```

**Complexity**: **Trivial to Moderate** — data class changes, but cascades to `TabExtractor`, `ValidationEngine`, `ContainerBlockGenerator`.

---

## 4. Demo App

### 4.1 `MainTabs.kt` — Demo Tab Definitions

**File**: `composeApp/.../destinations/MainTabs.kt`

**Old pattern assumptions**: Uses **hybrid** old pattern — `@Tabs(items=[...])` on sealed class with `@TabItem` markers on nested subclasses:

```kotlin
@Tabs(
    name = "mainTabs",
    initialTab = HomeTab::class,
    items = [HomeTab::class, ExploreTab::class, ProfileTab::class, SettingsTab::class]
)
sealed class MainTabs : NavDestination {
    @TabItem
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem
    @Stack(name = "exploreTabStack", ...)
    sealed class ExploreTab : MainTabs() { ... }
    // ...
}
```

**Required changes**: Convert to new child-to-parent pattern:
```kotlin
@Tabs(name = "mainTabs")
sealed class MainTabs : NavDestination {
    @TabItem(parent = MainTabs::class, ordinal = 0)
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem(parent = MainTabs::class, ordinal = 1)
    @Stack(name = "exploreTabStack", ...)
    sealed class ExploreTab : MainTabs() { ... }

    @TabItem(parent = MainTabs::class, ordinal = 2)
    @Destination(route = "main/profile")
    data object ProfileTab : MainTabs()

    @TabItem(parent = MainTabs::class, ordinal = 3)
    @Stack(name = "settingsTabStack", ...)
    sealed class SettingsTab : MainTabs() { ... }
}
```

**Complexity**: **Trivial** — mechanical annotation parameter updates.

---

## 5. Annotations Module

### 5.1 `TabAnnotations.kt` — Annotation Definitions

**File**: `quo-vadis-annotations/.../TabAnnotations.kt`

**Current state** (compiler-plugin branch):
```kotlin
annotation class Tabs(
    val name: String,
    val initialTab: KClass<*> = Unit::class,
    val items: Array<KClass<*>> = [],
)

annotation class TabItem   // marker, no params
```

**Must become** (main's version):
```kotlin
annotation class Tabs(
    val name: String,
)

annotation class TabItem(
    val parent: KClass<*>,
    val ordinal: Int,
)
```

**Note**: This file was listed as having merge conflicts. The resolution must adopt main's annotation signatures.

**Complexity**: **Trivial** — but this is the root cause of ALL downstream changes.

---

## Summary Table

| # | File | Module | Nature of Fix | Complexity |
|---|------|--------|---------------|------------|
| 1 | `TabAnnotations.kt` | annotations | Change `@Tabs`/`@TabItem` signatures | Trivial |
| 2 | `NavigationMetadata.kt` | compiler-plugin | Update `TabsMetadata`, `TabItemMetadata`, `TabItemType` | Trivial |
| 3 | `IrMetadataCollector.kt` | compiler-plugin | Rewrite `processTabs()` to child-to-parent discovery | **Significant** |
| 4 | `AnnotationExtractor.kt` | compiler-plugin | Add `intArgument()` helper | Trivial |
| 5 | `BaseConfigIrGenerator.kt` | compiler-plugin | Update tab registration code gen | Moderate |
| 6 | FIR Checkers (new) | compiler-plugin | Add ordinal/parent validation checker | Moderate |
| 7 | `TestSources.kt` | compiler-plugin/test | Rewrite all tab test snippets | Moderate |
| 8 | `IrCodegenTests.kt` | compiler-plugin/test | Update tab test assertions | Trivial–Moderate |
| 9 | `FirDiagnosticTests.kt` | compiler-plugin/test | Update + add tab diagnostic tests | Moderate |
| 10 | `TabExtractor.kt` | ksp | Complete rewrite to child-to-parent | **Significant** |
| 11 | `TabInfo.kt` | ksp/models | Update data models + enum | Trivial–Moderate |
| 12 | `MainTabs.kt` | composeApp/demo | Convert to new annotation pattern | Trivial |
| 13 | `ContainerBlockGenerator` (KSP) | ksp | Update enum refs + remove initialTab logic | Moderate |

---

## Recommended Fix Ordering

1. **`TabAnnotations.kt`** — foundation; everything depends on this
2. **`NavigationMetadata.kt`** + **`TabInfo.kt`** — data models used by everything downstream
3. **`IrMetadataCollector.processTabs()`** — the core compiler-plugin extraction rewrite
4. **`AnnotationExtractor.kt`** — add `intArgument` helper
5. **FIR Checkers** — add ordinal validation
6. **`BaseConfigIrGenerator.kt`** — update IR code generation
7. **`TabExtractor.kt`** (KSP) — adopt main's rewritten version
8. **`TestSources.kt`** — update all tab test sources
9. **`IrCodegenTests.kt`** + **`FirDiagnosticTests.kt`** — verify tests pass
10. **`MainTabs.kt`** (demo) — convert demo to new pattern
11. **Verify end-to-end** — run `./gradlew test` and `./gradlew :composeApp:assembleDebug`

---

## Risk Areas

1. **`IrMetadataCollector` multi-pass architecture**: The current 6-pass design assumes `@Tabs` lists its children. Reversing to child-to-parent requires either a new pre-pass to index `@TabItem` annotations or restructuring the pass ordering. The `classIndex` from Pass 1 can be extended to also collect `@TabItem` parent references.

2. **`registerTabsContainerFun` signature**: The core API's `registerTabsContainer()` function may have changed on main to accommodate ordinals / tab merging. Must verify the IR-level function resolution matches.

3. **`CompositeNavigationConfig.mergeTabNodes()`**: This main-only feature enables cross-module tab merging. The compiler plugin's `AggregatedConfigIrGenerator` must produce compatible `TabNode` structures, or aggregated configs won't merge correctly.

4. **Integer argument extraction in IR**: `IrMetadataCollector` currently has `getStringArgument`, `getBooleanArgument`, `getClassArgument`, `getEnumArgument`, `getClassArrayArgument` — but **no `getIntArgument`**. Must add this for reading `ordinal`.
