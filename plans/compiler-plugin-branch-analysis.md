# Compiler-Plugin Branch Analysis Report

**Date**: 15 March 2026  
**Branch**: `compiler-plugin` (12 commits since v0.3.5)  
**Scope**: Changes made on the compiler-plugin branch and their interaction with main's tab refactoring

---

## Executive Summary

The compiler-plugin branch adds a complete K2 compiler plugin backend for navigation code generation alongside the existing KSP processor. However, the branch operates on the **OLD tab annotation pattern** (`@Tabs(items=[...])` with `@TabItem` as a marker), while the main branch has refactored to the **NEW child-to-parent pattern** (`@Tabs(name)` with `@TabItem(parent, ordinal)`). This creates **hard conflicts** in the annotations module, the entire KSP tab processing pipeline, KSP models, and the demo app.

The compiler plugin module itself (`quo-vadis-compiler-plugin`) is largely independent new code, but its IR metadata collection also reads the OLD `@Tabs(items=[...])` annotation shape. Meanwhile, the core module additions (`NavigationConfigRegistry`, `navigationConfig<T>()`, `@NavigationRoot`, builder hooks) are additive and compatible with both tab patterns.

---

## 1. Tab Annotations (`TabAnnotations.kt`)

### Compiler-Plugin Branch (OLD Pattern)

```kotlin
// @Tabs: parent-lists-children
@Tabs(
    name: String,
    initialTab: KClass<*> = Unit::class,
    items: Array<KClass<*>> = []
)

// @TabItem: marker annotation, no parameters
@TabItem  // no parent, no ordinal
```

### Main Branch (NEW Pattern)

```kotlin
// @Tabs: pure declaration, only has name
@Tabs(name: String)

// @TabItem: child-to-parent with explicit ordering
@TabItem(
    parent: KClass<*>,
    ordinal: Int
)
```

### Analysis

**HARD CONFLICT**. These are fundamentally incompatible annotation signatures:

| Aspect | Compiler-Plugin | Main |
|--------|----------------|------|
| Tab discovery | Parent lists children in `items` array | Children declare parent via `@TabItem(parent=...)` |
| Tab ordering | Order of `items` array | `ordinal` parameter on `@TabItem` |
| Initial tab | `initialTab` parameter on `@Tabs` | Tab with `ordinal=0` is initial |
| Cross-module tabs | Not supported (items array is local) | Supported (children point to parent) |
| `@TabItem` role | Marker annotation (no parameters) | Discovery annotation (parent + ordinal) |
| Retention | `BINARY` | `BINARY` |

The compiler-plugin branch also demonstrates the nested-subclass pattern in the demo:
```kotlin
@Tabs(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ...])
sealed class MainTabs : NavDestination {
    @TabItem  // marker
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()
    // ...
}
```

Main uses top-level classes:
```kotlin
@Tabs(name = "mainTabs")
object MainTabs

@TabItem(parent = MainTabs::class, ordinal = 0)
@Stack(name = "homeStack", startDestination = HomeTab.Feed::class)
sealed class HomeTab : NavDestination { ... }
```

---

## 2. KSP Module Changes

### TabExtractor

**HARD CONFLICT**. Completely rewritten with different architecture:

| Aspect | Compiler-Plugin | Main |
|--------|----------------|------|
| Entry method | `extract(classDeclaration)` — processes single `@Tabs` class | `extractAll(resolver)` — scans all `@TabItem` classes |
| Tab discovery | Reads `items` array from `@Tabs` annotation | Scans for `@TabItem(parent=...)` and groups by parent |
| Legacy support | `extractFromSealedSubclasses()` fallback | Not present (old pattern removed) |
| Pattern detection | `isNewPattern` flag based on `items` array presence | Single pattern only |
| Method structure | `extract`, `extractFromItemsArray`, `extractFromSealedSubclasses`, `detectTabItemType`, `extractTabItemNewPattern` | `extractAll`, `extractAllTabItems`, `extractAllTabs`, `extractTabItem`, `detectTabItemType`, `extractTypeSpecificInfo` |

### TabInfo / TabItemInfo / TabItemType Models

**HARD CONFLICT**. Incompatible data models:

| Field/Type | Compiler-Plugin | Main |
|------------|----------------|------|
| `TabItemType` values | `FLAT_SCREEN`, `NESTED_STACK` | `DESTINATION`, `STACK`, `TABS` |
| `TabInfo.initialTabClass` | Present (`KSClassDeclaration?`) | Absent |
| `TabInfo.isNewPattern` | Present (`Boolean`) | Absent |
| `TabInfo.isCrossModule` | Absent | Present (`Boolean`) |
| `TabItemInfo.ordinal` | Absent | Present (`Int`) |
| Nested tabs support | Only via `NESTED_STACK` | First-class `TABS` type |

### ValidationEngine

Compiler-plugin branch has `ValidationEngine.validate()` with standard checks. Main branch added:
- `ValidationEngineOrdinalTest.kt` — validates ordinal=0 exists, no duplicate ordinals, no gaps
- `ValidationEngineContainerReferenceTest.kt` — validates cross-module container references

These test files exist because main's new tab pattern requires ordinal validation. They would be **inapplicable** to the compiler-plugin branch's model (no ordinals).

### ContainerBlockGenerator

Uses `TabItemInfo` and `TabItemType` from the compiler-plugin branch models. Iterates `tab.tabs` without ordinal sorting (relies on `items` array order). Main's version sorts by `ordinal`.

### NavigationConfigGenerator

References `TabItemInfo.tabType` using `FLAT_SCREEN`/`NESTED_STACK` values. Also uses `tab.tabs.any { tabItem -> tabItem.stackInfo?.classDeclaration... }` to find stacks. This code is structurally tied to the compiler-plugin branch's model.

### QuoVadisSymbolProcessor

Added capabilities that are **partially independent** of the tab refactoring:
- `@NavigationRoot` processing (`collectNavigationRoot`)
- Aggregated config generation (`generateAggregatedConfig`)
- `ClasspathConfigDiscovery` and `AggregatedConfigGenerator` — new classes for multi-module config discovery
- These additions do NOT conflict with main's tab refactoring and are **forward-compatible improvements**

However, the tab collection path (`collectTabs`, `extractTabInfo`) is tied to the old `TabExtractor.extract()` API.

---

## 3. Core Module Changes

### New Files (Additive, No Conflict)

| File | Purpose |
|------|---------|
| `NavigationConfigRegistry.kt` | Internal registry for `navigationConfig<T>()` backing. Mutable `KClass→NavigationConfig` map. |
| `NavigationConfigRegistryTest.kt` | Tests for registry register/get/overwrite behavior |
| `NavigationConfigResolutionTest.kt` | Tests for `navigationConfig<T>()` resolution and error messages |

### NavigationConfig.kt Changes

**Compatible**. Added at the end of the file:
- `navigationConfig<reified T>()` inline function — calls `NavigationConfigRegistry.get(T::class)`, or errors
- KDoc referencing `@NavigationRoot` annotation

The `NavigationConfig` interface itself is unchanged — same properties, same `plus` operator, same `buildNavNode` signature.

### CompositeNavigationConfig.kt

**Potential conflict**. The compiler-plugin branch has a straightforward composite that delegates to primary/secondary for each registry. Main's version (visible via Serena) adds:
- `mergeTabNodes()` — merges tab nodes from multiple configs (for cross-module tab support)
- `rekeyStack()` / `rekeySubtree()` — re-keys nodes after merging

These methods exist on main to support the new `@TabItem(parent=...)` cross-module tab discovery pattern. They are absent from the compiler-plugin branch.

### NavigationConfigBuilder.kt

**Compatible** additions. All new methods are `@InternalQuoVadisApi`:
- `registerTabsContainer(...)` — registers tab container with scope, tab classes, instances
- `registerStackContainer(...)` — registers stack container
- `registerScope(...)` — registers scope membership
- `registerTransition(...)` — registers transition
- `registerPaneContainer(...)` — registers pane container with roles

These are IR generation targets for the compiler plugin. They call into the existing builder infrastructure. They do NOT conflict with any main branch changes.

### Other Core Changes (from audit)

- **TabRenderer**: Changed wrapper lookup priority to `scopeKey ?: wrapperKey ?: nodeKey`. This is a **runtime behavior change** not directly related to tab annotations, but potentially conflicting if main changed TabRenderer too.
- **PaneOperations/BackOperations**: Back-navigation behavior changes for panes. Also a runtime behavior change, independent of tab refactoring.
- **ScreenRegistry.Empty**: Additive.
- **PaneRoleRegistry KClass overload**: Additive.

---

## 4. Gradle Plugin

### Current State

The Gradle plugin on the compiler-plugin branch is **STILL the old KSP-only version**:
- `QuoVadisExtension` has only `modulePrefix` and `useLocalKsp`
- `QuoVadisPlugin.apply()` requires KSP plugin, configures KSP dependencies
- **No `backend` property**, no `useCompilerPlugin`, no `KotlinCompilerPluginSupportPlugin`

The interchangeability plan (`compiler-plugin-ksp-interchangeability-plan.md`) was documented but **NOT implemented** in the Gradle plugin. The compiler plugin module is registered in `settings.gradle.kts` but is not wired through the Gradle plugin.

### Implication

To use the compiler plugin, it must be manually added to the compilation classpath. The documented backend switching (`quoVadis.backend = ksp | compiler`) does not exist yet.

---

## 5. Demo App (`composeApp`)

### Current State

The demo app uses the **KSP/manual composition** pattern:
```kotlin
// DI.kt
@Single
fun navigationConfig(): NavigationConfig =
    ComposeAppNavigationConfig +
    Feature1NavigationConfig +
    Feature2NavigationConfig
```

It does NOT use `@NavigationRoot` or `navigationConfig<T>()`. The audit plan described a state where the demo was migrated to compiler-plugin aggregation, but the current branch state shows the manual KSP approach.

### Tab Pattern

`MainTabs.kt` uses the OLD pattern:
```kotlin
@Tabs(name = "mainTabs", initialTab = HomeTab::class, items = [HomeTab::class, ...])
sealed class MainTabs : NavDestination {
    @TabItem
    @Destination(route = "main/home")
    data object HomeTab : MainTabs()
    // ...
}
```

This is **incompatible** with main's new tab pattern.

### Build Configuration

`composeApp/build.gradle.kts` uses `plugins { alias(libs.plugins.ksp); alias(libs.plugins.quoVadis) }` — standard KSP-only configuration.

---

## 6. New Modules

### `quo-vadis-compiler-plugin`

A complete K2 compiler plugin with **36 source files** and **10 test files**:

**FIR layer** (frontend — IDE integration, diagnostics):
- `QuoVadisDeclarationGenerationExtension` — synthesizes config objects in FIR
- `AnnotationExtractor` — extracts annotation metadata during FIR
- 8 diagnostic checkers: route collision, argument parity, transition compatibility, container roles, screen validation, structural, NavigationRoot uniqueness, NavigationConfig type
- `QuoVadisFirExtensionRegistrar`, `QuoVadisPredicates`

**IR layer** (backend — code generation):
- `IrMetadataCollector` — collects all navigation metadata from IR tree
- 7 IR generators: BaseConfig, NavigationConfig, DeepLinkHandler, ScreenRegistry, ContainerRegistry, PaneRoleRegistry, AggregatedConfig
- `NavigationConfigCallTransformer` — rewrites `navigationConfig<T>()` call sites
- `MultiModuleDiscovery` — classpath scanning for cross-module configs
- `ContainerWrapperValidator` — validates wrapper bindings

**Common**:
- `NavigationMetadata` — shared metadata model used by IR
- `NormalizedContainerBindings` — resolves wrapper-to-container bindings
- `QuoVadisCommandLineProcessor`, `QuoVadisCompilerPluginRegistrar`

**Build**: Depends on `kotlin-compiler-embeddable` (compile-only). Test deps include `kotlin-compile-testing`, annotations, and core modules.

**Tab handling in compiler plugin**: The `IrMetadataCollector.processTabs()` reads the `@Tabs` annotation using positional arguments:
```kotlin
val name = tabsAnn.getStringArgument(0)      // name
val initialTab = tabsAnn.getClassArgument(1)  // initialTab
val itemClasses = tabsAnn.getClassArrayArgument(2)  // items array
```
This is **hardcoded to the OLD annotation shape** and will break with main's `@Tabs(name)` only.

### `quo-vadis-compiler-plugin-native`

Shadow module for Kotlin/Native targets:
- No source files of its own
- Syncs sources from `quo-vadis-compiler-plugin` via Gradle `Sync` task
- Uses `kotlin-compiler` (not embeddable) to resolve package structure differences
- Required because Native compiler plugins can't use the embedded compiler dependency

---

## Conflict Classification

### Hard Conflicts (require resolution before merge)

| Area | Files | Nature |
|------|-------|--------|
| Tab annotations | `TabAnnotations.kt` | Incompatible annotation signatures |
| KSP TabExtractor | `TabExtractor.kt` | Completely different architecture |
| KSP models | `TabInfo.kt` | Incompatible field sets, enum values |
| Compiler plugin IR | `IrMetadataCollector.kt`, `NavigationMetadata.kt` | Reads OLD annotation shape |
| Demo tabs | `MainTabs.kt` | Uses OLD pattern |
| KSP ContainerBlockGenerator | `ContainerBlockGenerator.kt` | Uses OLD `TabItemType` values |
| KSP NavigationConfigGenerator | `NavigationConfigGenerator.kt` | Uses OLD `TabItemInfo` model |
| CompositeNavigationConfig | `CompositeNavigationConfig.kt` | Main added `mergeTabNodes`/`rekeyStack` for cross-module tabs |

### Independent Improvements (can be kept as-is)

| Area | Files | Nature |
|------|-------|--------|
| Core `NavigationConfigRegistry` | `NavigationConfigRegistry.kt` | New additive API |
| Core `navigationConfig<T>()` | `NavigationConfig.kt` | New additive function |
| Annotations `@NavigationRoot` | `NavigationRoot.kt` | New additive annotation |
| Builder hooks | `NavigationConfigBuilder.kt` | `@InternalQuoVadisApi` methods for IR targets |
| KSP aggregation | `AggregatedConfigGenerator.kt`, `ClasspathConfigDiscovery.kt` | New KSP capability |
| KSP `@NavigationRoot` processing | `QuoVadisSymbolProcessor.kt` | New processing step |
| Compiler plugin module (all) | `quo-vadis-compiler-plugin/**` | Entirely new (but needs tab update) |
| Compiler plugin native | `quo-vadis-compiler-plugin-native/**` | Shadow module |
| Core additive APIs | `ScreenRegistry.Empty`, `PaneRoleRegistry` overload | Safe additions |

### Runtime behavior changes (independent of tab refactoring, but broader than compiler-plugin scope)

| Area | Files | Risk |
|------|-------|------|
| TabRenderer lookup priority | `TabRenderer.kt` | Medium — changed `scopeKey ?: wrapperKey` ordering |
| Pane back-navigation | `BackOperations.kt`, `PaneOperations.kt` | Medium — runtime contract change |

---

## Answers to Specific Questions

### Q1: What specific changes did compiler-plugin make to TabAnnotations.kt?

The compiler-plugin branch has:
- `@Tabs(name, initialTab, items)` — 3 parameters, parent-lists-children pattern
- `@TabItem` — marker annotation, no parameters
- `@Retention(BINARY)` (changed from `SOURCE`)

It assumes the **OLD** `@Tabs(items=...)` pattern. It does NOT have the NEW `@TabItem(parent, ordinal)` pattern.

### Q2: What changes to the core NavigationConfig interface were made?

The `NavigationConfig` interface itself is unchanged. The changes are:
1. Added `navigationConfig<reified T>()` top-level function (new, additive)
2. Added `NavigationConfigRegistry` object (new, additive, internal)
3. `NavigationConfigBuilder` gained `@InternalQuoVadisApi` registration methods (additive)
4. `CompositeNavigationConfig` is a clean composite delegator (no cross-module tab merging — main added that)

These are **compatible with main's changes** — they're additive and don't modify existing signatures.

### Q3: How does the KSP/compiler plugin interchangeability work?

**It's planned but NOT fully implemented.** The interchangeability plan defines:
- Gradle property `quoVadis.backend = ksp | compiler` switching
- `NavigationConfigRegistry` for KSP runtime lookup 
- `navigationConfig<T>()` that works with either backend
- `@GeneratedConfig` annotation for multi-module discovery

What's actually implemented:
- `NavigationConfigRegistry` and `navigationConfig<T>()` — YES
- KSP `@NavigationRoot` processing and aggregated config generation — YES
- Gradle plugin backend switching — **NO** (still KSP-only)
- `@GeneratedConfig` annotation — **NO** (still using `GeneratedNavigationConfig` marker interface concept)

### Q4: What changes to the demo app's tab destinations were made?

The demo uses `@Tabs(items=[...])` OLD pattern with tabs as nested sealed subclasses. `DI.kt` manually composes configs with `+`. No `@NavigationRoot` or `navigationConfig<T>()` usage.

### Q5: Which compiler-plugin changes to KSP files are independent vs which conflict?

**Independent (safe to keep):**
- `QuoVadisSymbolProcessor`: `@NavigationRoot` processing, aggregated config generation, classpath discovery
- `AggregatedConfigGenerator.kt`: New file
- `ClasspathConfigDiscovery.kt`: New file

**Conflicting (tied to old tab pattern):**
- `TabExtractor.kt`: Completely different architecture
- `TabInfo.kt`: Incompatible models
- `ContainerBlockGenerator.kt`: Uses old `TabItemType` values
- `NavigationConfigGenerator.kt`: Uses old `TabItemInfo` model
- `ValidationEngine.kt`: Validation logic may differ
- Any test files using `FLAT_SCREEN`/`NESTED_STACK` or `initialTabClass`/`isNewPattern`

---

## Merge Strategy Recommendations

1. **Tab annotations must use main's NEW pattern** — it's the better design (cross-module support, separation of concerns). The compiler-plugin branch's annotation shape must be updated.

2. **Compiler plugin IR metadata collection must be updated** — `IrMetadataCollector.processTabs()` needs to scan for `@TabItem(parent=...)` instead of reading `@Tabs(items=[...])`.

3. **KSP tab pipeline must adopt main's version** — The entire `TabExtractor`, `TabInfo`/`TabItemInfo` models, and downstream generators need main's architecture.

4. **Core additions are safe to merge** — `NavigationConfigRegistry`, `navigationConfig<T>()`, `@NavigationRoot`, builder hooks are all additive and compatible.

5. **CompositeNavigationConfig needs main's tab merging** — for cross-module tab support.

6. **Demo must be updated** to use main's tab declaration pattern.

7. **Compiler plugin module is mostly independent** but its metadata layer needs tab pattern updates.
