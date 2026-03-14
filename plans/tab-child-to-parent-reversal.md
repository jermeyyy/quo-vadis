# Tab Child-to-Parent Dependency Reversal

## Overview

Reverse the parent-child dependency in the tab annotation system. Currently `@Tabs` (parent) lists its children via an `items` array. This change makes `@TabItem` (child) point to its `@Tabs` parent via `parent: KClass<*>` and `ordinal: Int` parameters.

**Breaking change** — no backward compatibility with legacy or current `items`-array pattern.

## Target API

### Simple tabs (same module)
```kotlin
@Tabs(name = "mainTabs")
data object MainTabs : NavDestination

@TabItem(MainTabs::class, ordinal = 0)
@Destination(route = "main/home")
data object HomeTab : NavDestination

@TabItem(MainTabs::class, ordinal = 1)
@Stack(name = "exploreStack", startDestination = ExploreFeed::class)
sealed class ExploreDestination : NavDestination { ... }

@TabItem(MainTabs::class, ordinal = 2)
@Destination(route = "main/profile")
data object ProfileTab : NavDestination
```

### Cross-module tab (feature module)
```kotlin
// In feature1 module:
@TabItem(MainTabs::class, ordinal = 3)
@Stack(name = "result_demo", startDestination = ResultDemoDestination.Demo::class)
sealed class ResultDemoDestination : NavDestination { ... }
```

### Nested tabs
```kotlin
@TabItem(MainTabs::class, ordinal = 1)
@Tabs(name = "nestedTabs")
data object NestedTabs : NavDestination

@TabItem(NestedTabs::class, ordinal = 0)
@Destination(route = "nested/first")
data object FirstNestedTab : NavDestination
```

### Optional sealed class grouping
```kotlin
@Tabs(name = "mainTabs")
sealed class MainTabs : NavDestination {
    companion object : NavDestination

    @TabItem(MainTabs::class, ordinal = 0)
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()

    @TabItem(MainTabs::class, ordinal = 1)
    @Stack(name = "exploreStack", startDestination = ExploreTab.Feed::class)
    sealed class ExploreTab : MainTabs() {
        @Destination(route = "explore/feed")
        data object Feed : ExploreTab()
    }
}
```

---

## Phase 1: Annotations Module

### Task 1.1: Modify `@Tabs` annotation

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

**Current:**
```kotlin
annotation class Tabs(
    val name: String,
    val initialTab: KClass<*> = Unit::class,
    val items: Array<KClass<*>> = [],
)
```

**New:**
```kotlin
annotation class Tabs(
    val name: String,
)
```

**Changes:**
- Remove `initialTab` parameter (replaced by `ordinal = 0` convention)
- Remove `items` parameter (replaced by child-to-parent `@TabItem(parent)`)
- Remove `initialTabLegacy` (only in KDoc, not an actual param)
- Update KDoc to document new pattern: `@Tabs` is now a pure declaration, children discover it via `@TabItem(parent)`

**Dependencies:** None  
**Acceptance criteria:**
- `@Tabs` has only `name: String` parameter
- KDoc describes the new child-to-parent pattern
- Retention remains `BINARY`

### Task 1.2: Modify `@TabItem` annotation

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt`

**Current:**
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TabItem
```

**New:**
```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TabItem(
    val parent: KClass<*>,
    val ordinal: Int,
)
```

**Changes:**
- Add `parent: KClass<*>` — required reference to the `@Tabs`-annotated class this tab belongs to
- Add `ordinal: Int` — required, defines tab order; `0` = initial tab
- Update KDoc with new usage examples showing `parent` and `ordinal`
- Keep `BINARY` retention (needed for cross-module resolution)

**Dependencies:** None  
**Acceptance criteria:**
- `@TabItem` has `parent: KClass<*>` and `ordinal: Int` parameters
- Both are required (no defaults)
- KDoc shows cross-module and nested tab examples

### Task 1.3: Verify `@TabsContainer` unchanged

**File:** `quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabsContainer.kt`

**Changes:** None — `@TabsContainer(tabClass: KClass<*>)` remains as-is.

**Dependencies:** None  
**Acceptance criteria:** No changes to `TabsContainer.kt`

---

## Phase 2: KSP Models

### Task 2.1: Update `TabItemType` enum

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`

**Current values:** `FLAT_SCREEN`, `NESTED_STACK`, `CONTAINER_REFERENCE`

**New values:** `DESTINATION`, `STACK`, `TABS`

**Changes:**
- `FLAT_SCREEN` → `DESTINATION` (tab is a `@Destination`-annotated screen)
- `NESTED_STACK` → `STACK` (tab is a `@Stack`-annotated navigation stack)
- `CONTAINER_REFERENCE` → removed; split into `STACK` and `TABS`
- New `TABS` value for `@TabItem` + `@Tabs` (nested tabs)
- Update KDoc for each enum value

**Rationale:** The old types mixed locality (NESTED_STACK = local, CONTAINER_REFERENCE = external) with structure. The new types are purely structural — locality no longer matters since discovery is annotation-based.

**Dependencies:** None  
**Acceptance criteria:**
- Enum has exactly 3 values: `DESTINATION`, `STACK`, `TABS`
- KDoc describes each pattern clearly

### Task 2.2: Update `TabItemInfo` data class

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`

**Current:**
```kotlin
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType = TabItemType.FLAT_SCREEN,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
)
```

**New:**
```kotlin
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType,
    val ordinal: Int,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
)
```

**Changes:**
- Add `ordinal: Int` field (from `@TabItem(ordinal = ...)`)
- Remove default for `tabType` (must be explicitly set)
- Update KDoc to document the three patterns: DESTINATION, STACK, TABS

**Dependencies:** Task 2.1  
**Acceptance criteria:**
- `ordinal` field present, no default
- `tabType` has no default
- KDoc updated

### Task 2.3: Update `TabInfo` data class

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/models/TabInfo.kt`

**Current:**
```kotlin
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val initialTabClass: KSClassDeclaration?,
    val isNewPattern: Boolean,
    val tabs: List<TabItemInfo>,
)
```

**New:**
```kotlin
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val tabs: List<TabItemInfo>,
)
```

**Changes:**
- Remove `initialTabClass` — initial tab is now the item with `ordinal = 0`
- Remove `isNewPattern` — only one pattern exists now
- `tabs` list is sorted by `ordinal` (enforced during extraction)
- Update KDoc

**Dependencies:** Task 2.2  
**Acceptance criteria:**
- Only 5 fields remain
- KDoc describes ordinal-based ordering
- No mention of legacy or new pattern distinction

---

## Phase 3: KSP Extractor (Major Rewrite)

### Task 3.1: Rewrite `TabExtractor` — new discovery flow

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/extractors/TabExtractor.kt`

**Current flow (~480 lines):**
1. `extract()` → reads `items` array from `@Tabs`, branches new vs legacy
2. `populateTabItemCache()` → caches nested `@TabItem` classes by parent for legacy
3. `extractFromItemsArray()` → iterates items array, extracts each
4. `extractFromSealedSubclasses()` → legacy sealed subclass discovery
5. `detectTabItemType()` → checks locality + annotations to determine type
6. `extractTabItemNewPattern()` → builds TabItemInfo for items-array pattern
7. `extractTabItemLegacy()` → builds TabItemInfo for legacy pattern
8. `extractTypeSpecificInfo()` → fills destinationInfo/stackInfo based on type
9. `resolveInitialTabClass()` → resolves `initialTab` KClass

**New flow:**
1. `extractAllTabItems(resolver)` → find all `@TabItem`-annotated classes, read `parent` and `ordinal` from each, return grouped by parent qualified name
2. `extractAllTabs(resolver, tabItemsByParent)` → find all `@Tabs`-annotated classes, for each: look up its children from `tabItemsByParent`, sort by ordinal, build `TabInfo`
3. `detectTabItemType(classDeclaration)` → simplified; no `tabContainerClass` param needed:
   - Has `@Destination` → `DESTINATION`
   - Has `@Tabs` → `TABS`
   - Has `@Stack` → `STACK`
   - None → error (validated later)
4. `extractTabItem(classDeclaration, ordinal)` → builds `TabItemInfo` with type detection and type-specific info extraction
5. `extractTypeSpecificInfo()` → keep as-is, update for new enum names

**Methods to remove:**
- `populateTabItemCache()` — no longer needed (no items array to cross-reference)
- `tabItemCache` property — removed
- `extractFromItemsArray()` — replaced by annotation-based discovery
- `extractFromSealedSubclasses()` — legacy pattern removed
- `extractTabItemLegacy()` — legacy pattern removed
- `resolveInitialTabClass()` — replaced by ordinal=0 convention
- `extractTabItemNewPattern()` — replaced by `extractTabItem()`

**Methods to keep (with modifications):**
- `extractTypeSpecificInfo()` — update enum references (`FLAT_SCREEN` → `DESTINATION`, `NESTED_STACK`/`CONTAINER_REFERENCE` → `STACK`)
- `detectTabItemType()` — simplify by removing `tabContainerClass` parameter

**Public API change:**
- `extractAll(resolver)` remains the main entry point but internally uses new flow
- `populateTabItemCache(resolver)` — remove or keep as no-op for backward compat in processor (prefer removing and updating processor)
- `extract(classDeclaration)` — no longer used as primary extraction; individual @Tabs extraction done via `extractAllTabs`

**Dependencies:** Phase 2 (models)  
**Acceptance criteria:**
- Discovery works: `@TabItem`s in any module are found and grouped by `parent`
- `@Tabs` without any `@TabItem` children are reported (empty container validation deferred to Phase 4)
- Tab items sorted by ordinal in resulting `TabInfo.tabs`
- No references to `items` array, legacy pattern, or sealed subclass discovery
- Cross-module `@TabItem` references work (KSP resolves `parent` KClass across modules)
- Estimated reduction from ~480 lines to ~200 lines

---

## Phase 4: KSP Validation

### Task 4.1: Add `validateOrdinalZeroExists`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

**New method:**
```kotlin
private fun validateOrdinalZeroExists(tabs: List<TabInfo>) {
    tabs.forEach { tab ->
        val hasZero = tab.tabs.any { it.ordinal == 0 }
        if (!hasZero) {
            reportError(
                tab.classDeclaration,
                "@Tabs '${tab.className}' has no @TabItem with ordinal = 0 (initial tab)",
                "Add ordinal = 0 to one of the @TabItem annotations targeting this @Tabs"
            )
        }
    }
}
```

**Dependencies:** Phase 2, Phase 3  
**Acceptance criteria:** KSP error emitted when no `ordinal = 0` exists for a `@Tabs`

### Task 4.2: Add `validateOrdinalCollisions`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

**New method:**
```kotlin
private fun validateOrdinalCollisions(tabs: List<TabInfo>) {
    tabs.forEach { tab ->
        val ordinals = tab.tabs.groupBy { it.ordinal }
        ordinals.filter { it.value.size > 1 }.forEach { (ordinal, items) ->
            items.forEach { item ->
                reportError(
                    item.classDeclaration,
                    "Duplicate ordinal $ordinal for @Tabs '${tab.className}'",
                    "Each @TabItem targeting '${tab.className}' must have a unique ordinal"
                )
            }
        }
    }
}
```

**Dependencies:** Phase 2, Phase 3  
**Acceptance criteria:** KSP error for duplicate ordinals within the same `@Tabs` parent

### Task 4.3: Add `validateOrdinalContinuity`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

**New method:**
```kotlin
private fun validateOrdinalContinuity(tabs: List<TabInfo>) {
    tabs.forEach { tab ->
        val ordinals = tab.tabs.map { it.ordinal }.sorted()
        val expected = (0 until ordinals.size).toList()
        if (ordinals != expected) {
            reportError(
                tab.classDeclaration,
                "@Tabs '${tab.className}' has ordinal gaps: found $ordinals, expected $expected",
                "Ordinals must be consecutive starting from 0"
            )
        }
    }
}
```

**Dependencies:** Phase 2, Phase 3  
**Acceptance criteria:** KSP error when ordinals have gaps (e.g., 0, 2, 3 without 1)

### Task 4.4: Add `validateTabItemParentReference`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

**Changes:** This validation is best performed in the extractor during discovery (Phase 3) since the extractor reads the `parent` KClass and checks if it has `@Tabs`. However, any orphaned `@TabItem` (parent has no `@Tabs`) should be reported.

**New method:**
```kotlin
private fun validateTabItemParentReference(tabs: List<TabInfo>, resolver: Resolver) {
    // This is called with the successfully matched tabs.
    // Additionally, check for @TabItem annotations whose parent doesn't have @Tabs.
    // This data needs to be passed from the extractor (orphaned tab items).
}
```

**Alternative approach:** Have the extractor produce a list of "orphaned" `@TabItem`s (parent KClass exists but has no `@Tabs` annotation) and pass them to validation. Or validate inline in the extractor since it already has the KSP resolver context.

**Dependencies:** Phase 3  
**Acceptance criteria:** KSP error when `@TabItem(parent = X::class)` and `X` is not annotated with `@Tabs`

### Task 4.5: Update `validateContainerTypes` — relax sealed class requirement

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

**Current:** (`validateContainerTypes`, lines 445–482)
```kotlin
tabs.forEach { tab ->
    if (!tab.classDeclaration.isSealed()) {
        reportError(tab.classDeclaration, "@Tabs '${tab.className}' must be a sealed class", ...)
    }
}
```

**New:** Remove the sealed-class requirement for `@Tabs`. Any class type (object, class, sealed class, interface) is allowed as long as it implements `NavDestination`.

```kotlin
tabs.forEach { tab ->
    // @Tabs no longer requires sealed class — any type implementing NavDestination is valid
    // Optionally: validate that it implements NavDestination (if this is enforced elsewhere)
}
```

**Dependencies:** Phase 2  
**Acceptance criteria:**
- `@Tabs` on a `data object`, `object`, `class`, `sealed class`, or `interface` is accepted
- No "must be a sealed class" error for `@Tabs`
- `@Stack` sealed class requirement remains unchanged

### Task 4.6: Remove obsolete validations

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngine.kt`

**Remove:**
- `validateTabInitialTabs()` (lines 156–177) — replaced by `validateOrdinalZeroExists`
- `validateTabItemsHaveAnnotation()` (lines 652–671) — validated `items` array items have `@TabItem`; no items array anymore
- `validateNestedStackTabs()` (lines 577–605) — `NESTED_STACK` type no longer exists; `STACK` type validation is simpler

**Update:**
- `validateTabItemAnnotations()` (lines 517–575) — update to reference new `TabItemType` names but logic stays the same (check exactly one of @Stack/@Destination/@Tabs)
- `validateFlatScreenTabs()` (lines 607–650) — rename references from `FLAT_SCREEN` to `DESTINATION`
- `validateContainerReferences()` (lines 673–695) — remove (no longer a separate type; `STACK` and `TABS` types are self-evident)
- `validateEmptyContainers()` — update error message for tabs (no longer "items array")

**Update `validate()` orchestration method:**
```kotlin
// Remove these calls:
validateTabInitialTabs(tabs)
validateNestedStackTabs(tabs)
validateTabItemsHaveAnnotation(tabs)
validateContainerReferences(tabs)

// Add these calls:
validateOrdinalZeroExists(tabs)
validateOrdinalCollisions(tabs)
validateOrdinalContinuity(tabs)

// Keep these (with updates):
validateTabItemAnnotations(tabs)  // updated enum names
validateFlatScreenTabs(tabs)      // FLAT_SCREEN → DESTINATION
validateCircularTabNesting(tabs)  // keep as-is (uses CONTAINER_REFERENCE → now check for TABS type)
validateTabNestingDepth(tabs)     // keep as-is (update type check)
```

**Dependencies:** Tasks 4.1–4.5  
**Acceptance criteria:**
- No reference to `FLAT_SCREEN`, `NESTED_STACK`, `CONTAINER_REFERENCE` enum values
- No reference to `initialTabClass`, `isNewPattern`, `items` array
- `validateCircularTabNesting` updated to check `TabItemType.TABS` instead of `TabItemType.CONTAINER_REFERENCE`
- `validateTabNestingDepth` updated similarly
- All removed methods have no callers

---

## Phase 5: KSP Generation

### Task 5.1: Update `generateTabsBlock`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`

**Current:** (lines 106–133)
```kotlin
private fun generateTabsBlock(tab: TabInfo): CodeBlock {
    ...
    val initialTabIndex = findInitialTabIndex(tab)
    if (initialTabIndex != 0) {
        builder.addStatement("initialTab = %L", initialTabIndex)
    }
    tab.tabs.forEachIndexed { index, tabItem ->
        builder.add(generateTabEntry(tabItem, index))
    }
    ...
}
```

**New:**
```kotlin
private fun generateTabsBlock(tab: TabInfo): CodeBlock {
    ...
    // No initialTab statement needed — ordinal 0 is always the initial tab,
    // and tabs are already sorted by ordinal
    tab.tabs.forEach { tabItem ->
        builder.add(generateTabEntry(tabItem))
    }
    ...
}
```

**Changes:**
- Remove `findInitialTabIndex` call and `initialTab` statement — the list is already ordinal-sorted, index 0 is always the initial tab
- Remove `index` parameter from `forEachIndexed` — not needed

**Dependencies:** Phase 2  
**Acceptance criteria:**
- No `initialTab = X` statement in generated code
- Tabs emitted in ordinal order (guaranteed by extractor sorting)

### Task 5.2: Remove `findInitialTabIndex`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`

**Current:** (lines 135–148) — Searches for `initialTabClass` match in tabs list.

**Change:** Delete entire method.

**Dependencies:** Task 5.1  
**Acceptance criteria:** Method removed, no callers

### Task 5.3: Update `generateTabEntry`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGenerator.kt`

**Current:** (lines 150–188) — Switches on `NESTED_STACK`, `CONTAINER_REFERENCE`, `FLAT_SCREEN`

**New:**
```kotlin
private fun generateTabEntry(tabItem: TabItemInfo): CodeBlock {
    val tabClassName = tabItem.classDeclaration.toClassName()
    val isObject = tabItem.classDeclaration.classKind == ClassKind.OBJECT

    return when (tabItem.tabType) {
        TabItemType.STACK -> {
            CodeBlock.of("containerTab<%T>()\n", tabClassName)
        }
        TabItemType.TABS -> {
            CodeBlock.of("containerTab<%T>()\n", tabClassName)
        }
        TabItemType.DESTINATION -> {
            if (isObject) {
                CodeBlock.of("tab(%T)\n", tabClassName)
            } else {
                CodeBlock.of("containerTab<%T>()\n", tabClassName)
            }
        }
    }
}
```

**Changes:**
- Remove `@Suppress("UNUSED_PARAMETER") index: Int` parameter
- Update enum branches: `NESTED_STACK` → `STACK`, `CONTAINER_REFERENCE` → split into `STACK` + `TABS`, `FLAT_SCREEN` → `DESTINATION`

**Dependencies:** Task 2.1  
**Acceptance criteria:**
- All three `TabItemType` values handled
- Generated DSL code unchanged functionally

---

## Phase 6: KSP Processor Orchestration

### Task 6.1: Update `collectAllSymbols` in `QuoVadisSymbolProcessor`

**File:** `quo-vadis-ksp/src/main/kotlin/com/jermey/quo/vadis/ksp/QuoVadisSymbolProcessor.kt`

**Current:** (lines 169–209)
```kotlin
private fun collectAllSymbols(resolver: Resolver) {
    collectStacks(resolver)
    tabExtractor.populateTabItemCache(resolver)  // ← remove
    collectTabs(resolver)
    ...
}
```

**New:**
```kotlin
private fun collectAllSymbols(resolver: Resolver) {
    collectStacks(resolver)
    collectTabs(resolver)  // TabExtractor.extractAll now handles everything internally
    ...
}
```

**Changes:**
- Remove `tabExtractor.populateTabItemCache(resolver)` call — the new `extractAll` does its own @TabItem discovery internally
- The `collectTabs` method likely calls `tabExtractor.extractAll(resolver)` which is being rewritten

**Dependencies:** Phase 3  
**Acceptance criteria:**
- `populateTabItemCache` call removed from processor
- Tab extraction works end-to-end through `extractAll`
- Multi-round processing still works (cross-module `@TabItem` in feature modules resolved)

### Task 6.2: Verify multi-round / cross-module handling

**Context:** In KSP multi-module builds, `@TabItem` in `feature1` module may be processed in a different round/module than `@Tabs` in `composeApp`. The `parent: KClass<*>` reference should resolve correctly because:
1. KSP resolves `KClass<*>` annotation arguments to `KSType` which includes fully qualified names
2. `getSymbolsWithAnnotation("...TabItem")` returns all `@TabItem`-annotated classes visible to the current compilation unit
3. The `@Tabs` class is imported (via dependency) so its qualified name is available for matching

**Potential issue:** If `feature1` is compiled before `composeApp`, the `@TabItem` in `feature1` references `MainTabs::class` which is in `composeApp`. In KSP, annotation class references are resolved to types, so this should work as long as `feature1` has a dependency on the module containing `MainTabs`.

**Dependencies:** Phase 3  
**Acceptance criteria:**
- `feature1/ResultDemoDestination.kt` with `@TabItem(MainTabs::class, ordinal = 4)` is discovered and linked to `MainTabs`
- No regression in cross-module tab resolution

---

## Phase 7: Update Tests

### Task 7.1: Update `TabItemTypeTest`

**File:** `quo-vadis-ksp/src/test/kotlin/com/jermey/quo/vadis/ksp/models/TabItemTypeTest.kt`

**Changes:**
- Update assertions for new enum values: `DESTINATION`, `STACK`, `TABS`
- Update count: still 3 values
- Update ordering assertions

**Dependencies:** Task 2.1  
**Acceptance criteria:** All tests pass with new enum values

### Task 7.2: Update `ContainerBlockGeneratorTabEntryTest`

**File:** `quo-vadis-ksp/src/test/kotlin/com/jermey/quo/vadis/ksp/generators/dsl/ContainerBlockGeneratorTabEntryTest.kt`

**Changes:**
- Update all `TabItemType.FLAT_SCREEN` → `TabItemType.DESTINATION`
- Update all `TabItemType.NESTED_STACK` → `TabItemType.STACK`
- Update all `TabItemType.CONTAINER_REFERENCE` → `TabItemType.STACK` or `TabItemType.TABS` as appropriate
- Add `ordinal` parameter to all `TabItemInfo` constructors
- Update `tabInfo` helper — remove `initialTabClass` and `isNewPattern` fields
- Update test names to reflect new terminology
- Add test for `TABS` type entry generation (currently only tested as `CONTAINER_REFERENCE with Tabs`)

**Dependencies:** Phase 2, Phase 5  
**Acceptance criteria:** All tests pass; coverage for DESTINATION, STACK, and TABS tab entry generation

### Task 7.3: Update `ValidationEngineContainerReferenceTest`

**File:** `quo-vadis-ksp/src/test/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngineContainerReferenceTest.kt`

**Changes:**
- Update `tabInfo` helper — remove `initialTabClass` and `isNewPattern`
- Update `TabItemInfo` constructors to include `ordinal`
- Update all `TabItemType.FLAT_SCREEN` → `TabItemType.DESTINATION`
- Update all `TabItemType.CONTAINER_REFERENCE` → `TabItemType.STACK` or `TabItemType.TABS`
- Update test names and assertions for new validation rules
- Remove tests for `validateContainerReferences` (method removed)
- Update circular nesting tests to use `TabItemType.TABS`
- Update nesting depth tests similarly

**Dependencies:** Phase 2, Phase 4  
**Acceptance criteria:** All tests pass; circular nesting and depth tests work with new types

### Task 7.4: Add new ordinal validation tests

**File:** `quo-vadis-ksp/src/test/kotlin/com/jermey/quo/vadis/ksp/validation/ValidationEngineOrdinalTest.kt` (new file)

**Tests to add:**
- `ordinal 0 missing produces error`
- `ordinal 0 present passes`
- `duplicate ordinals produce error`
- `unique ordinals pass`
- `ordinal gap (0, 2) produces error`
- `consecutive ordinals (0, 1, 2) pass`
- `single tab item with ordinal 0 passes`
- `parent KClass without @Tabs produces error` (if validated here)

**Dependencies:** Phase 4  
**Acceptance criteria:** Comprehensive test coverage for all ordinal validation rules

### Task 7.5: Update any other test files

**Search for:** All test files referencing `TabItemType`, `TabInfo`, `TabItemInfo`, `FLAT_SCREEN`, `NESTED_STACK`, `CONTAINER_REFERENCE`, `initialTabClass`, `isNewPattern`

**Files to check via grep:**
```bash
grep -rl "FLAT_SCREEN\|NESTED_STACK\|CONTAINER_REFERENCE\|initialTabClass\|isNewPattern" quo-vadis-ksp/src/test/
```

**Dependencies:** Phase 2  
**Acceptance criteria:** No compilation errors in test sources; all tests pass

---

## Phase 8: Migrate Demo App

### Task 8.1: Migrate `MainTabs.kt`

**File:** `composeApp/src/commonMain/kotlin/.../destinations/MainTabs.kt`

**Current:** Sealed class with `@Tabs(name, initialTab, items)` and nested `@TabItem` members

**New:**
```kotlin
@Tabs(name = "mainTabs")
sealed class MainTabs : NavDestination {
    companion object : NavDestination

    @TabItem(MainTabs::class, ordinal = 0)
    @Destination(route = "main/home")
    @Transition(type = TransitionType.Fade)
    data object HomeTab : MainTabs()

    @TabItem(MainTabs::class, ordinal = 1)
    @Stack(name = "exploreTabStack", startDestination = ExploreTab.Feed::class)
    @Transition(type = TransitionType.Fade)
    sealed class ExploreTab : MainTabs() { ... }

    @TabItem(MainTabs::class, ordinal = 2)
    @Destination(route = "main/profile")
    @Transition(type = TransitionType.Fade)
    data object ProfileTab : MainTabs()

    @TabItem(MainTabs::class, ordinal = 3)
    @Stack(name = "settingsTabStack", startDestination = SettingsTab.Main::class)
    @Transition(type = TransitionType.Fade)
    sealed class SettingsTab : MainTabs() { ... }
}
```

And the cross-module `ResultDemoDestination` gets ordinal = 4 (see Task 8.3).

**Changes:**
- Remove `initialTab = HomeTab::class` from `@Tabs`
- Remove `items = [...]` from `@Tabs`
- Add `@TabItem(MainTabs::class, ordinal = N)` to each tab member
- HomeTab = 0, ExploreTab = 1, ProfileTab = 2, SettingsTab = 3

**Dependencies:** Phase 1  
**Acceptance criteria:**
- Compiles without errors after KSP changes
- Demo app tab behavior identical to current

### Task 8.2: Migrate `TabsDestination.kt` (DemoTabs)

**File:** `composeApp/src/commonMain/kotlin/.../destinations/TabsDestination.kt`

**Current:** `@Tabs(name = "demoTabs", initialTab = MusicTab::class, items = [MusicTab::class, MoviesTab::class, BooksTab::class])`

**New:**
```kotlin
@Tabs(name = "demoTabs")
sealed class DemoTabs : NavDestination {
    companion object : NavDestination

    @TabItem(DemoTabs::class, ordinal = 0)
    @Stack(name = "musicStack", startDestination = MusicTab.List::class)
    sealed class MusicTab : DemoTabs() { ... }

    @TabItem(DemoTabs::class, ordinal = 1)
    @Stack(name = "moviesStack", startDestination = MoviesTab.List::class)
    sealed class MoviesTab : DemoTabs() { ... }

    @TabItem(DemoTabs::class, ordinal = 2)
    @Stack(name = "booksStack", startDestination = BooksTab.List::class)
    sealed class BooksTab : DemoTabs() { ... }
}
```

**Changes:**
- Remove `initialTab` and `items` from `@Tabs`
- Add `@TabItem(DemoTabs::class, ordinal = N)` to each tab member

**Dependencies:** Phase 1  
**Acceptance criteria:** DemoTabs tab demo works identically

### Task 8.3: Migrate `feature1/ResultDemoDestination.kt`

**File:** `feature1/src/commonMain/kotlin/com/jermey/feature1/resultdemo/ResultDemoDestinations.kt`

**Current:**
```kotlin
@TabItem
@Stack(name = "result_demo", startDestination = ResultDemoDestination.Demo::class)
sealed class ResultDemoDestination : NavDestination { ... }
```

**New:**
```kotlin
@TabItem(MainTabs::class, ordinal = 4)
@Stack(name = "result_demo", startDestination = ResultDemoDestination.Demo::class)
sealed class ResultDemoDestination : NavDestination { ... }
```

**Changes:**
- Add `(MainTabs::class, ordinal = 4)` to `@TabItem`
- May need to add import for `MainTabs`

**Dependencies:** Phase 1, Task 8.1 (ordinal coordination)  
**Acceptance criteria:**
- Cross-module tab reference works
- `feature1` module compiles with reference to `MainTabs::class`
- ResultDemo tab appears as the 5th tab (ordinal 4)

### Task 8.4: Verify `TabsDemoWrapper.kt` unchanged

**File:** `composeApp/src/commonMain/kotlin/.../screens/tabs/TabsDemoWrapper.kt`

**Changes:** None expected — the `@TabsContainer(DemoTabs::class)` pattern and `scope.tabs` pattern-matching remain the same. The `scope.tabs` list order is still determined by ordinal (now explicitly, previously by items array order).

**Acceptance criteria:** No changes needed; wrapper compiles and works

### Task 8.5: Search for any other `@Tabs` or `@TabItem` usages

**Search:**
```bash
grep -rl "@Tabs\|@TabItem" composeApp/ feature1/ feature2/
```

**Dependencies:** Phase 1  
**Acceptance criteria:** All usages migrated

---

## Phase 9: Documentation

### Task 9.1: Update `ANNOTATIONS.md`

**File:** `docs/ANNOTATIONS.md`

**Changes:**
- Update `@Tabs and @TabItem Annotations` section (line ~301)
  - `@Tabs` properties: only `name: String`
  - `@TabItem` properties: `parent: KClass<*>`, `ordinal: Int`
  - Remove references to `items` array, `initialTab`, legacy pattern
  - Update examples to show new pattern
  - Update `@TabItem Requirement` section — now required with parent + ordinal
  - Update cross-module tab reference section
  - Update `TabItemType` table: DESTINATION, STACK, TABS
  - Update validation table for new ordinal validations
  - Remove "must be sealed class" validation for @Tabs

**Dependencies:** All previous phases  
**Acceptance criteria:** Documentation accurately reflects new annotation API

### Task 9.2: Update `copilot-instructions.md` if needed

**File:** `.github/copilot-instructions.md`

**Changes:**
- Update `@Tabs` + `@TabItem` section under "Tabs and Panes"
- Update code examples showing item-array pattern → parent-reference pattern

**Dependencies:** All previous phases  
**Acceptance criteria:** Copilot instructions reflect current API

---

## Dependency Graph

```text
Phase 1 (Annotations) ──────────────────────────── stands alone
    │
    ├─── Phase 2 (Models) ──────────────────────── depends on Phase 1
    │        │
    │        ├─── Phase 3 (Extractor) ──────────── depends on Phase 2
    │        │        │
    │        │        ├─── Phase 4 (Validation) ── depends on Phase 2, 3
    │        │        │
    │        │        └─── Phase 6 (Processor) ─── depends on Phase 3
    │        │
    │        └─── Phase 5 (Generation) ─────────── depends on Phase 2
    │
    └─── Phase 8 (Demo App Migration) ──────────── depends on Phase 1
                                                    (compiles after all KSP phases)

Phase 7 (Tests) ─── depends on Phases 2, 4, 5

Phase 9 (Docs) ──── depends on all previous phases
```

**Recommended execution order:** 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9

Phases 4, 5, and 6 can be worked on in parallel after Phase 3, but Phase 7 (tests) should follow all of them to ensure test assertions match final code.

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| Cross-module `@TabItem` parent resolution fails in KSP | High | Low | KClass annotation args resolve to KSType with qualified names; verify in feature1 module |
| Generated DSL code changes break runtime behavior | High | Low | Generated code structure unchanged; only enum names differ internally |
| Missing migration of a `@Tabs`/`@TabItem` usage | Medium | Low | Grep-based search in Task 8.5 catches all usages |
| Multi-round KSP processing misses `@TabItem` from other modules | High | Medium | Test cross-module scenario; KSP `getSymbolsWithAnnotation` should return all visible symbols |
| Ordinal validation interferes with multi-module builds | Medium | Medium | In multi-module, the KSP processor may not see all `@TabItem`s in one round; may need deferred validation |

---

## Estimated Scope

| Phase | Files Changed | Lines Added/Removed (est.) |
|-------|---------------|---------------------------|
| Phase 1: Annotations | 1 | -80 / +40 (net -40, mostly KDoc) |
| Phase 2: Models | 1 | -50 / +30 (net -20) |
| Phase 3: Extractor | 1 | -350 / +200 (net -150, major rewrite) |
| Phase 4: Validation | 1 | -80 / +60 (net -20) |
| Phase 5: Generation | 1 | -20 / +15 (net -5) |
| Phase 6: Processor | 1 | -5 / +0 (net -5) |
| Phase 7: Tests | 4 | -60 / +120 (net +60, new ordinal tests) |
| Phase 8: Demo App | 3 | -10 / +15 (net +5) |
| Phase 9: Docs | 2 | -40 / +50 (net +10) |
| **Total** | **~15 files** | **net ~-165 lines** |
