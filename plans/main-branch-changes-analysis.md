# Main Branch Changes Analysis (v0.3.5 → multimodule-nested-tabs)

**Date**: 15 March 2026  
**Scope**: Changes on `main` since v0.3.5 (the merge-base with `compiler-plugin` branch)  
**Focus**: Key conflict areas that must be reconciled during merge

---

## Executive Summary

The `main` branch underwent a **fundamental architectural redesign of the tab annotation system**, shifting from a **parent-lists-children** pattern to a **child-declares-parent** pattern. This is the single most impactful change affecting the merge, touching annotations, extractors, validators, generators, and core config composition. Additionally, `main` added cross-module tab node merging in `CompositeNavigationConfig`, ordinal-based tab ordering, nested-tabs support (tabs within tabs), and new validation rules.

---

## 1. Tab Annotation System — `@Tabs` and `@TabItem` Redesign

### File: `quo-vadis-annotations/.../TabAnnotations.kt`

| Aspect | compiler-plugin branch (v0.3.5) | main branch (multimodule-nested-tabs) |
|--------|--------------------------------|---------------------------------------|
| `@Tabs` params | `name: String`, `initialTab: KClass<*>`, `items: Array<KClass<*>>` | `name: String` **only** |
| `@TabItem` params | **Marker annotation** (no params) | `parent: KClass<*>`, `ordinal: Int` |
| `@Tabs` retention | `BINARY` | `BINARY` |
| `@TabItem` retention | `BINARY` | `BINARY` |
| Dependency direction | Parent lists children via `items = [Class1::class, Class2::class]` | Each child declares `@TabItem(parent = ParentTabs::class, ordinal = N)` |
| Initial tab | `initialTab: KClass<*>` on `@Tabs` or legacy `initialTabLegacy: String` | `ordinal = 0` is the initial tab |
| Tab ordering | Order of `items` array | Sorted by `ordinal` |

### Key Architectural Change: **Child-to-Parent Pattern**

**Before** (compiler-plugin branch):
```kotlin
@Tabs(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ExploreTab::class])
object MainTabs

@TabItem   // marker annotation only
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }
```

**After** (main branch):
```kotlin
@Tabs(name = "mainTabs")   // pure declaration — no children listed
object MainTabs

@TabItem(parent = MainTabs::class, ordinal = 0)   // child -> parent reference
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }
```

### Why This Matters
- **Cross-module discovery**: Children in other modules can reference a `@Tabs` parent from a compiled dependency without requiring the parent to enumerate them. The parent is resolved from the classpath via `Resolver.getClassDeclarationByName`.
- **No more `items` array**: The `items` parameter is completely removed.
- **No more `initialTab`/`initialTabLegacy`**: Tab ordering is solely via `ordinal`.
- **Legacy pattern removed**: The old nested-sealed-subclass pattern and related dual-pattern code is gone.

---

## 2. TabInfo / TabItemInfo Model Changes

### File: `quo-vadis-ksp/.../models/TabInfo.kt`

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| `TabItemType` enum values | `FLAT_SCREEN`, `NESTED_STACK` | `DESTINATION`, `STACK`, `TABS` |
| `TabInfo.initialTabClass` | `KSClassDeclaration?` | **Removed** |
| `TabInfo.isNewPattern` | `Boolean` | **Removed** |
| `TabInfo.isCrossModule` | Not present | `Boolean = false` |
| `TabItemInfo.ordinal` | Not present | `Int` |
| Nested-tabs support | No | Yes (`TABS` type = tab whose child is another `@Tabs` container) |

### TabItemType Mapping
- `FLAT_SCREEN` → renamed to `DESTINATION`
- `NESTED_STACK` → renamed to `STACK`
- New: `TABS` — a tab item that is itself a `@Tabs` container (nested tabs)

### TabInfo Constructor Comparison

**compiler-plugin branch:**
```kotlin
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val initialTabClass: KSClassDeclaration?,  // REMOVED on main
    val isNewPattern: Boolean,                  // REMOVED on main
    val tabs: List<TabItemInfo>,
)
```

**main branch:**
```kotlin
data class TabInfo(
    val classDeclaration: KSClassDeclaration,
    val name: String,
    val className: String,
    val packageName: String,
    val tabs: List<TabItemInfo>,
    val isCrossModule: Boolean = false,         // NEW on main
)
```

### TabItemInfo Constructor Comparison

**compiler-plugin branch:**
```kotlin
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType = TabItemType.FLAT_SCREEN,
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
)
```

**main branch:**
```kotlin
data class TabItemInfo(
    val classDeclaration: KSClassDeclaration,
    val tabType: TabItemType,
    val ordinal: Int,                           // NEW on main
    val destinationInfo: DestinationInfo? = null,
    val stackInfo: StackInfo? = null,
)
```

---

## 3. TabExtractor Refactoring

### File: `quo-vadis-ksp/.../extractors/TabExtractor.kt`

The extractor was **completely rewritten** on main with a fundamentally different approach.

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| Discovery strategy | Scans `@Tabs`-annotated classes, reads `items` array OR sealed subclasses | Two-phase: 1) Scan all `@TabItem`, group by parent. 2) Scan `@Tabs`, match children. |
| `extractAll()` signature | `fun extractAll(resolver): List<TabInfo>` (uses `populateTabItemCache` + scans `@Tab`/`@Tabs`) | `fun extractAll(resolver): List<TabInfo>` (calls `extractAllTabItems` then `extractAllTabs`) |
| Internal methods | `extract`, `resolveInitialTabClass`, `extractFromItemsArray`, `detectTabItemType`, `extractTabItemNewPattern`, `extractTypeSpecificInfo`, `extractFromSealedSubclasses`, `extractTabItemLegacy`, `populateTabItemCache` | `extractAllTabItems`, `extractAllTabs`, `extractTabItem`, `detectTabItemType`, `extractTypeSpecificInfo` |
| Cross-module support | None | Resolves `@TabItem.parent` from compiled classpath dependencies |
| Legacy pattern | Full support with `extractFromSealedSubclasses` and `extractTabItemLegacy` | **Completely removed** |
| Tab ordering | Order in `items` array or sealed subclass order | Sorted by `ordinal` from `@TabItem` annotation |
| `@Tab` support | Yes (both `@Tab` and `@Tabs`) | Only `@Tabs` |
| `tabItemCache` | Manual cache for legacy pattern | Not needed; `@TabItem(parent=...)` provides direct reference |

### Key Methods on main:

1. **`extractAllTabItems(resolver)`**: Finds all `@TabItem`-annotated classes, reads `parent` KClass and `ordinal` Int from each annotation, groups by parent qualified name → `Map<String, List<Pair<KSClassDeclaration, Int>>>`

2. **`extractAllTabs(resolver, tabItemsByParent)`**: Finds all `@Tabs` classes, then for each:
   - Matches children from `tabItemsByParent` map
   - If a `@TabItem.parent` references a class not in current sources (orphaned parent), attempts **classpath resolution** via `resolver.getClassDeclarationByName`
   - Skips `@Tabs` with no children (they may have children in downstream modules)
   - Sets `isCrossModule = true` when `classDecl.containingFile == null`

3. **`detectTabItemType(classDecl)`**: Now detects 3 types: `STACK` (has `@Stack`), `DESTINATION` (has `@Destination`), `TABS` (has `@Tabs`)

---

## 4. ValidationEngine Changes

### File: `quo-vadis-ksp/.../validation/ValidationEngine.kt`

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| `validate()` signature | Takes `resolver: Resolver` as last param | No `resolver` param |
| Tab-specific validations | `validateTabInitialTabs`, `validateTabItemAnnotations`, `validateNestedStackTabs`, `validateFlatScreenTabs` | `validateOrdinalZeroExists`, `validateOrdinalCollisions`, `validateOrdinalContinuity`, `validateTabItemAnnotations`, `validateDestinationTabs`, `validateCircularTabNesting`, `validateTabNestingDepth` |
| `validateRootGraphClass` | Present (added for compiler-plugin) | **Not present** |

### New Validation Rules on main:

1. **`validateOrdinalZeroExists`**: Every non-cross-module `@Tabs` must have at least one `@TabItem` with `ordinal = 0` (initial tab).

2. **`validateOrdinalCollisions`**: No two `@TabItem` entries in the same `@Tabs` can share the same `ordinal`.

3. **`validateOrdinalContinuity`**: Ordinals should be contiguous starting from 0 (warning, not error).

4. **`validateTabItemAnnotations`**: Each `@TabItem` must have exactly one of `@Stack`, `@Destination`, or `@Tabs`. Having 0 or 2+ is an error. (Expanded from the compiler-plugin version which only checked `@Stack` and `@Destination`.)

5. **`validateDestinationTabs`**: `DESTINATION`-type tabs must be data objects with `@Destination` and a route.

6. **`validateCircularTabNesting`**: Builds a directed graph of `TABS`-type tab items pointing to other `@Tabs` and detects cycles via DFS.

7. **`validateTabNestingDepth`**: Warns when tab nesting exceeds 3 levels (only runs if no cycles detected).

### Removed Validations:

- `validateTabInitialTabs` (validating `initialTab` references) — no longer needed since ordinals replaced `initialTab`.
- `validateNestedStackTabs` / `validateFlatScreenTabs` — replaced by `validateTabItemAnnotations` and `validateDestinationTabs` with broader checks.
- `validateRootGraphClass` — this was a compiler-plugin-specific addition not present on main.
- `validate()` no longer takes `resolver` parameter.

### validateEmptyContainers Changes:

On main, `validateEmptyContainers` for tabs:
- Uses a **warning** (not error) for empty tabs, with a message acknowledging that children may be in downstream modules
- Suggests adding `@TabItem(parent = ClassName::class)` in the fix hint
- On compiler-plugin, it was a hard error

---

## 5. ContainerBlockGenerator Changes

### File: `quo-vadis-ksp/.../generators/dsl/ContainerBlockGenerator.kt`

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| `generateTabsBlock` | Handles `initialTab` index, uses `findInitialTabIndex` | No `initialTab`; tabs sorted by ordinal, index 0 is initial |
| `generateTabEntry` | Takes `(tabItem, index)`, uses `NESTED_STACK`/`FLAT_SCREEN` enum | Takes `(tabItem)` only, uses `STACK`/`DESTINATION`/`TABS` enum |
| `findInitialTabIndex` | Present (resolves initialTab class to index) | **Removed** (not needed with ordinals) |
| Nested-tabs handling | Not supported | `TABS` case generates `containerTab<Type>()` |

### Main Branch `generateTabEntry`:
```kotlin
when (tabItem.tabType) {
    TabItemType.STACK, TabItemType.TABS -> CodeBlock.of("containerTab<%T>()\n", tabClassName)
    TabItemType.DESTINATION -> {
        if (isObject) CodeBlock.of("tab(%T)\n", tabClassName)
        else CodeBlock.of("containerTab<%T>()\n", tabClassName)
    }
}
```

### Compiler-Plugin Branch `generateTabEntry`:
```kotlin
when (tabItem.tabType) {
    TabItemType.NESTED_STACK -> CodeBlock.of("containerTab<%T>()\n", tabClassName)
    TabItemType.FLAT_SCREEN -> {
        if (isObject) CodeBlock.of("tab(%T)\n", tabClassName)
        else CodeBlock.of("containerTab<%T>()\n", tabClassName)
    }
}
```

---

## 6. CompositeNavigationConfig Changes

### Files: `quo-vadis-core/.../config/CompositeNavigationConfig.kt` (public) and `...internal/config/CompositeNavigationConfig.kt` (internal)

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| `setNodeResolver` | Not present | New method — propagates resolver to primary+secondary |
| `NavigationConfig.setNodeResolver` | Not present on interface | New `@InternalQuoVadisApi` default method on interface |
| `init` block | Not present | Calls `setNodeResolver(::buildNavNode)` on both primary and secondary |
| `buildNavNode` | Simple fallback: `secondary ?: primary` | **Merges TabNodes**: if both return `TabNode`, calls `mergeTabNodes` |
| `mergeTabNodes` | Not present | New: merges stacks from two TabNodes, deduplicates by route, re-keys |
| `rekeyStack` | Not present | New: re-keys a stack with a new key prefix |
| `rekeySubtree` | Not present | New: recursively re-keys all children in a subtree |

### Cross-Module Tab Merging

The most significant addition is `mergeTabNodes()`, which enables tabs from different modules to be merged at config composition time:

```kotlin
private fun mergeTabNodes(primary: TabNode, secondary: TabNode): TabNode {
    val primaryRoutes = primary.tabMetadata.map { it.route }.toSet()
    val newIndices = secondary.tabMetadata.indices
        .filter { i -> secondary.tabMetadata[i].route !in primaryRoutes }
    val additionalStacks = newIndices.mapIndexed { offset, secondaryIndex ->
        val newTabIndex = primary.stacks.size + offset
        rekeyStack(secondary.stacks[secondaryIndex], "$tabNodeKey/tab$newTabIndex", tabNodeKey)
    }
    return TabNode(
        key = primary.key, parentKey = primary.parentKey,
        stacks = primary.stacks + additionalStacks,
        tabMetadata = primary.tabMetadata + additionalMetadata,
        ...
    )
}
```

This is critical for the multi-module story: feature module A defines some tabs, feature module B defines other tabs referencing the same `@Tabs` parent. When configs are composed with `+`, the TabNodes get merged.

### `setNodeResolver` Pattern

Main introduces a cross-config node resolution pattern:
- `NavigationConfig` interface gains `setNodeResolver(...)` with a default no-op
- `CompositeNavigationConfig.init` wires `::buildNavNode` as the resolver for both configs
- This enables individual module configs to resolve destinations from other modules when building container nodes

---

## 7. NavigationConfig Interface Changes

### File: `quo-vadis-core/.../config/NavigationConfig.kt`

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| `setNodeResolver` | Not present | New `@InternalQuoVadisApi` method with default no-op |
| `navigationConfig<T>()` | Present (for compiler plugin) | Not present (compiler-plugin-only addition) |
| `GeneratedNavigationConfig` | Present (companion interface) | Not present (compiler-plugin-only addition) |

The `setNodeResolver` is the only new API on main that isn't already on compiler-plugin.

---

## 8. QuoVadisSymbolProcessor Changes

### File: `quo-vadis-ksp/.../QuoVadisSymbolProcessor.kt`

| Aspect | compiler-plugin branch | main branch |
|--------|----------------------|-------------|
| `validate()` call | Passes `resolver` param | No `resolver` param |
| `collectAllSymbols` | Calls `tabExtractor.populateTabItemCache(resolver)` before `collectTabs` | Calls `collectTabs(resolver)` directly (TabExtractor.extractAll handles internal discovery) |
| `@NavigationRoot` processing | Step 8 in collectAllSymbols | **Not present** (compiler-plugin-only feature) |
| `collectTabs` | Collects both `@Tab` and `@Tabs` | Collects only `@Tabs` |

---

## 9. TreeNavigator Changes

The TreeNavigator on main does not appear to have fundamental architectural differences specific to the multimodule-nested-tabs feature. The main changes are upstream in `CompositeNavigationConfig` and the tab system.

---

## Summary of Merge Conflict Resolution Strategy

### Must Adopt from Main (breaking APIs):
1. **`@Tabs` annotation** — remove `initialTab` and `items` params, keep only `name`
2. **`@TabItem` annotation** — add `parent: KClass<*>` and `ordinal: Int` params
3. **`TabItemType` enum** — rename values: `FLAT_SCREEN`→`DESTINATION`, `NESTED_STACK`→`STACK`, add `TABS`
4. **`TabInfo` model** — remove `initialTabClass` and `isNewPattern`, add `isCrossModule`
5. **`TabItemInfo` model** — add `ordinal: Int`
6. **`TabExtractor`** — complete rewrite to child-discovers-parent pattern with cross-module support
7. **`ValidationEngine`** — adopt ordinal validations, circular nesting detection, remove `resolver` param from `validate()`, remove legacy tab validations
8. **`ContainerBlockGenerator`** — update to new enum values, remove `initialTab` logic
9. **`CompositeNavigationConfig`** — add `mergeTabNodes`, `rekeyStack`, `rekeySubtree`, `setNodeResolver`, init block
10. **`NavigationConfig`** — add `setNodeResolver` interface method

### Must Preserve from compiler-plugin Branch:
1. **`navigationConfig<T>()`** stub function
2. **`GeneratedNavigationConfig`** marker interface (or `@GeneratedConfig` annotation per interchangeability plan)
3. **`@NavigationRoot`** processing in QuoVadisSymbolProcessor
4. **`validateRootGraphClass`** in ValidationEngine
5. **Compiler plugin module** (`quo-vadis-compiler-plugin/`) and its infrastructure
6. **Gradle plugin** backend selection logic

### Key Risk Areas:
1. The compiler plugin's FIR/IR tab handling must be updated to match the new child-to-parent `@TabItem(parent, ordinal)` pattern
2. `NavigationConfigCallTransformer` and aggregated config generation must account for `setNodeResolver` and `mergeTabNodes`
3. Tab container building in both KSP and compiler-plugin backends must produce compatible `TabNode` structures for cross-module merging to work
4. Demo app (`composeApp`) must be updated to use the new `@TabItem(parent=..., ordinal=...)` syntax
