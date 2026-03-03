# Implementation Plan: `navigationConfig<T>()` DSL Function

**Date**: 3 March 2026  
**Status**: Ready for Implementation  
**Branch**: `compiler-plugin`

---

## Overview

Replace manual `NavigationConfig` composition (`Config1 + Config2 + Config3`) with a compiler-plugin-powered reified inline function `navigationConfig<T>()` that auto-discovers and aggregates all `GeneratedNavigationConfig` implementations from the dependency graph.

### Target API

```kotlin
@NavigationRoot
object AppNavigation

@Module
class NavigationModule {
    @Single
    fun navigator(): Navigator {
        val navigationConfig = navigationConfig<AppNavigation>()
        val rootDestination = MainTabs::class
        val initialState = navigationConfig.buildNavNode(
            destinationClass = rootDestination,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
                    "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                    "or manually registered in the NavigationConfig."
        )
        return TreeNavigator(
            config = navigationConfig,
            initialState = initialState
        )
    }
}
```

### Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| API surface | `navigationConfig<T>()` reified inline function | Explicitly links to `@NavigationRoot` class, supports multiple roots in theory |
| Generated object visibility | Internal only — function is sole entry point | Cleaner API, no generated objects in user namespace |
| Bridging mechanism | IR call-site transformation | Zero runtime overhead, compiler rewrites call to direct object reference |
| Fallback without plugin | Compile-time error diagnostic | `navigationConfig<T>()` where T is not `@NavigationRoot` produces compiler error |
| Function location | `quo-vadis-core` | Single function definition, reused across all consumer modules |
| Scope | Core path + Phase 3 prerequisite gaps | Includes panes, transitions, deep links as prerequisites |

---

## Requirements

- `navigationConfig<AppNavigation>()` returns a fully-aggregated `NavigationConfig`
- Compiler plugin auto-discovers all `GeneratedNavigationConfig` implementors from classpath
- No manual imports or `+` chaining needed
- The reified type parameter must be annotated with `@NavigationRoot`, enforced at compile time
- Stack, tab, pane navigation, transitions, and deep links all work through the aggregated config
- Demo app (`composeApp`) updated to use the new API

---

## Technical Approach

### How IR Call-Site Transformation Works

```
Source code:                    After IR transformation:
─────────────                   ───────────────────────
val config =                    val config =
  navigationConfig              AppNavigation__AggregatedConfig
    <AppNavigation>()             .INSTANCE
```

1. `quo-vadis-core` declares: `inline fun <reified T> navigationConfig(): NavigationConfig`
2. The stub body contains: `error("Quo Vadis compiler plugin not applied")`
3. The compiler plugin's IR pass:
   a. Finds all call sites of `navigationConfig()` in the current module
   b. Extracts the reified type argument `T`
   c. Validates `T` has `@NavigationRoot` annotation (emits error if not)
   d. Replaces the entire call expression with a reference to the internally-generated aggregated config object
4. The aggregated config object is generated per `@NavigationRoot` (internal visibility, not exposed to user)

### Aggregated Config Generation

The compiler plugin generates an `internal object` for each `@NavigationRoot`:

```kotlin
// Generated (internal, in com.jermey.quo.vadis.generated)
internal object AppNavigation__AggregatedConfig : NavigationConfig {
    private val delegate: NavigationConfig by lazy {
        ComposeAppNavigationConfig +     // own module
            Feature1NavigationConfig +   // discovered from classpath
            Feature2NavigationConfig     // discovered from classpath
    }

    override val screenRegistry get() = delegate.screenRegistry
    override val scopeRegistry get() = delegate.scopeRegistry
    // ... all NavigationConfig members delegate to `delegate`
}
```

Discovery uses `IrPluginContext` to find all classes implementing `GeneratedNavigationConfig` from the classpath, sorted lexicographically by FQN for determinism.

---

## Tasks

### Prerequisite: Phase 3 IR Gaps

These must be completed before the aggregated config is useful end-to-end.

#### Task P1: BaseConfigIrGenerator — Add Pane Registration

- **Description**: Extend `buildInitializerExpression()` to process `metadata.panes` and emit pane builder calls, following the existing tab/stack pattern.
- **File**: `quo-vadis-compiler-plugin/.../ir/generators/BaseConfigIrGenerator.kt`
- **Details**: Iterate `metadata.panes`, call the equivalent of `panes<T>(scopeKey, ...) { ... }` on the `NavigationConfigBuilder` in IR.
- **Acceptance Criteria**:
  - [ ] `@Pane`-annotated classes produce pane registration in the `_baseConfig` builder
  - [ ] `@PaneItem` roles are passed to the builder
  - [ ] Pane-based adaptive layouts render correctly in demo app

#### Task P2: BaseConfigIrGenerator — Add Transition Registration

- **Description**: Extend `buildInitializerExpression()` to process `metadata.transitions` and emit `transition<T>(NavTransition.X)` calls.
- **File**: `quo-vadis-compiler-plugin/.../ir/generators/BaseConfigIrGenerator.kt`
- **Details**: Iterate `metadata.transitions`, for each emit a `transition<DestinationType>(transitionInstance)` builder call.
- **Acceptance Criteria**:
  - [ ] `@Transition`-annotated destinations get custom transitions applied
  - [ ] `transitionRegistry` returns correct `NavTransition` for annotated destinations
  - [ ] Default transitions still apply for non-annotated destinations

#### Task P3: PaneRoleRegistryIrGenerator — Real Dispatch

- **Description**: Replace the `PaneRoleRegistry.Empty` stub with a when-based dispatch that maps destinations to their pane roles using metadata.
- **File**: `quo-vadis-compiler-plugin/.../ir/generators/PaneRoleRegistryIrGenerator.kt`
- **Details**: Generate `when (destination) { is X -> PaneRole.Primary; is Y -> PaneRole.Detail; else -> null }` dispatch for `getPaneRole()` methods.
- **Acceptance Criteria**:
  - [ ] `getPaneRole(scopeKey, destination)` returns correct role for `@PaneItem` destinations
  - [ ] Returns null for destinations without pane roles
  - [ ] Both `NavDestination` and `KClass<out NavDestination>` overloads work

#### Task P4: DeepLinkHandlerIrGenerator — Implement Core Methods

- **Description**: Generate real method bodies for the DeepLinkHandler object. 
- **File**: `quo-vadis-compiler-plugin/.../ir/generators/DeepLinkHandlerIrGenerator.kt`
- **Methods to implement**:
  1. `resolve(uri: String): NavDestination?` — Pattern-match URI against registered routes, extract parameters, call destination factories
  2. `canHandle(uri: String): Boolean` — Check if any registered pattern matches
  3. `handle(uri: String, navigator: Navigator): Boolean` — Resolve + navigate
  4. `getRegisteredPatterns(): List<String>` — Return all route patterns
  5. `createUri(destination: NavDestination, scheme: String): String?` — Reverse routing
  6. `resolve(deepLink: DeepLink): NavDestination?` — Resolve from DeepLink object
  7. `register(...)` / `registerAction(...)` — Store dynamic registrations
- **Acceptance Criteria**:
  - [ ] URI deep links resolve to correct destinations
  - [ ] Route parameters are extracted and passed to destination constructors
  - [ ] `canHandle()` returns true for registered patterns
  - [ ] `getRegisteredPatterns()` returns all declared routes

---

### Phase 4: Auto-Discovery & `navigationConfig<T>()`

#### Task 1: Add `@NavigationRoot` FIR Predicate

- **Description**: Register `@NavigationRoot` in `QuoVadisPredicates.kt` so the FIR checker and declaration generator can detect it.
- **File**: `quo-vadis-compiler-plugin/.../fir/QuoVadisPredicates.kt`
- **Details**:
  - Add `HAS_NAVIGATION_ROOT` predicate matching `@NavigationRoot` annotation
  - Register in the predicate factory
- **Acceptance Criteria**:
  - [ ] `@NavigationRoot`-annotated classes are detected by FIR extensions
  - [ ] No false positives from other annotations

#### Task 2: FIR Checker — Single `@NavigationRoot` Validation

- **Description**: Emit a compilation error if more than one `@NavigationRoot` exists in the same module.
- **Files**:
  - `quo-vadis-compiler-plugin/.../fir/QuoVadisDiagnostics.kt` — Add `MULTIPLE_NAVIGATION_ROOTS` error
  - `quo-vadis-compiler-plugin/.../fir/QuoVadisDeclarationChecker.kt` — Add validation logic
- **Diagnostic message**: `"Multiple @NavigationRoot annotations found in this module. Only one @NavigationRoot is allowed per compilation unit."`
- **Acceptance Criteria**:
  - [ ] Two `@NavigationRoot` classes in same module → compile error listing both locations
  - [ ] Single `@NavigationRoot` → no error
  - [ ] Zero `@NavigationRoot` → no error

#### Task 3: FIR Checker — `navigationConfig<T>()` Type Validation

- **Description**: Emit a compilation error when `navigationConfig<T>()` is called with a type `T` that is not annotated with `@NavigationRoot`.
- **Files**:
  - `quo-vadis-compiler-plugin/.../fir/QuoVadisDiagnostics.kt` — Add `NAVIGATION_ROOT_REQUIRED` error
  - New FIR checker for call expression validation
- **Diagnostic message**: `"Type argument T of navigationConfig<T>() must be annotated with @NavigationRoot"`
- **Acceptance Criteria**:
  - [ ] `navigationConfig<AppNavigation>()` where `AppNavigation` has `@NavigationRoot` → no error
  - [ ] `navigationConfig<SomeRandomClass>()` → compile error
  - [ ] Error message clearly states the requirement

#### Task 4: `navigationConfig<T>()` Stub Function in quo-vadis-core

- **Description**: Add the reified inline function declaration in `quo-vadis-core` that serves as the user-facing API and will be transformed by the compiler plugin.
- **File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/NavigationConfig.kt` (or a new file `NavigationConfigDsl.kt` in the same package)
- **Implementation**:
  ```kotlin
  /**
   * Returns the auto-discovered [NavigationConfig] aggregating all
   * [GeneratedNavigationConfig] implementations from the dependency graph.
   *
   * The type parameter [T] must be a class annotated with [@NavigationRoot].
   * The Quo Vadis compiler plugin replaces this call with a direct reference
   * to the generated aggregated config at compile time.
   *
   * @throws IllegalStateException if the Quo Vadis compiler plugin is not applied
   */
  inline fun <reified T> navigationConfig(): NavigationConfig {
      error(
          "navigationConfig<${T::class.simpleName}>() requires the Quo Vadis compiler plugin. " +
          "Add 'id(\"io.github.jermeyyy.quo-vadis\")' to your plugins block."
      )
  }
  ```
- **Acceptance Criteria**:
  - [ ] Function compiles on all KMP targets
  - [ ] Without compiler plugin, calling it throws with descriptive error message
  - [ ] KDoc explains usage and requirements
  - [ ] No naming collision with existing `navigationConfig { builder }` DSL function (different signature — no lambda parameter)

#### Task 5: Change Annotation Retentions to BINARY

- **Description**: Update navigation annotation retentions from SOURCE to BINARY so they survive in `.klib`/`.jar` and are visible to the compiler plugin processing dependent modules.
- **Files to modify**:

  | File | Annotation | SOURCE → BINARY |
  |------|-----------|-----------------|
  | `quo-vadis-annotations/.../Stack.kt` | `@Stack` | ✅ |
  | `quo-vadis-annotations/.../Destination.kt` | `@Destination` | ✅ |
  | `quo-vadis-annotations/.../Screen.kt` | `@Screen` | ✅ |
  | `quo-vadis-annotations/.../TabAnnotations.kt` | `@Tabs`, `@TabItem` | ✅ |
  | `quo-vadis-annotations/.../PaneAnnotations.kt` | `@Pane`, `@PaneItem` | ✅ |

- **Unchanged** (already sufficient):
  - `@Argument` — SOURCE (only needed within same module)
  - `@TabsContainer` — RUNTIME (superset of BINARY)
  - `@PaneContainer` — RUNTIME
  - `@Transition` — RUNTIME
  - `@NavigationRoot` — already BINARY

- **Acceptance Criteria**:
  - [ ] All 7 annotations updated to `@Retention(AnnotationRetention.BINARY)`
  - [ ] Existing KSP processor still works (KSP can read BINARY annotations)
  - [ ] Feature module `.klib` output contains annotation metadata

#### Task 6: FIR — Synthesize Aggregated Config Declaration

- **Description**: When `@NavigationRoot` is detected in the current module, synthesize an internal aggregated config object declaration in FIR. This gives IDE visibility (autocomplete for the `navigationConfig<T>()` return type).
- **File**: `quo-vadis-compiler-plugin/.../fir/QuoVadisDeclarationGenerationExtension.kt`
- **Details**:
  - Detect `@NavigationRoot` on classes/objects in the current module
  - Extract prefix from annotation parameter or class name
  - Generate `internal object {Prefix}__AggregatedConfig : NavigationConfig` in `com.jermey.quo.vadis.generated`
  - The object has all `NavigationConfig` member declarations (properties + functions) — bodies deferred to IR
  - The object does NOT implement `GeneratedNavigationConfig` (it's a root aggregator, not a per-module config)
- **Acceptance Criteria**:
  - [ ] `{Prefix}__AggregatedConfig` object is synthesized in FIR when `@NavigationRoot` is present
  - [ ] All `NavigationConfig` interface members are declared
  - [ ] Object has internal visibility
  - [ ] IDE can resolve the type (no red squiggles on `navigationConfig<T>()` return usages)

#### Task 7: IR — Classpath Scanning for GeneratedNavigationConfig Implementors

- **Description**: Create `MultiModuleDiscovery` class that scans the classpath via `IrPluginContext` to find all `GeneratedNavigationConfig` implementations from dependency modules.
- **File**: `quo-vadis-compiler-plugin/.../ir/MultiModuleDiscovery.kt` (new)
- **Technical approach**:
  1. Use `IrPluginContext.referenceClass()` to resolve `GeneratedNavigationConfig` marker
  2. Iterate the module's dependency descriptors to find all classes implementing the marker
  3. Filter out `NavigationConfig.Empty`, `CompositeNavigationConfig`, `EmptyNavigationConfig`
  4. Filter out the aggregated config itself (to avoid circular reference)
  5. Sort found configs lexicographically by FQN for deterministic ordering
  6. Return sorted list of `IrClassSymbol` references
- **Open question**: If `IrPluginContext` cannot enumerate dependency classes by supertype, fall back to resource-based or known-package scanning (see existing plan OQ-4.1 and OQ-4.2)
- **Acceptance Criteria**:
  - [ ] Discovers `ComposeAppNavigationConfig`, `Feature1NavigationConfig`, `Feature2NavigationConfig` from test project
  - [ ] Excludes core library types (`Empty`, `Composite`)
  - [ ] Results sorted lexicographically by FQN
  - [ ] Works for `.klib` (K/Native, K/JS) and `.jar` (JVM) artifacts
  - [ ] Transitive dependencies discovered (app → feature1 → shared)

#### Task 8: IR — Generate Aggregated Config Body

- **Description**: Generate the IR body for the `{Prefix}__AggregatedConfig` object using discovered configs chained via `plus`.
- **Files**:
  - `quo-vadis-compiler-plugin/.../ir/QuoVadisIrGenerationExtension.kt` — Wire up root config generation
  - `quo-vadis-compiler-plugin/.../ir/BodySynthesisTransformer.kt` — Add dispatch for aggregated config
- **Generated IR equivalent**:
  ```kotlin
  internal object AppNavigation__AggregatedConfig : NavigationConfig {
      private val delegate: NavigationConfig by lazy {
          // Own module's config first (lowest priority)
          ComposeAppNavigationConfig +
              // Discovered feature configs (sorted by FQN)
              Feature1NavigationConfig +
              Feature2NavigationConfig
      }
  
      override val screenRegistry get() = delegate.screenRegistry
      override val scopeRegistry get() = delegate.scopeRegistry
      override val transitionRegistry get() = delegate.transitionRegistry
      override val containerRegistry get() = delegate.containerRegistry
      override val deepLinkRegistry get() = delegate.deepLinkRegistry
      override val paneRoleRegistry get() = delegate.paneRoleRegistry
      override val roots get() = delegate.roots
  
      override fun buildNavNode(...) = delegate.buildNavNode(...)
      override fun plus(other: NavigationConfig) = CompositeNavigationConfig(this, other)
  }
  ```
- **Key details**:
  - `lazy` initialization prevents resolution-order issues
  - Own module's config is first in chain (lowest priority, overridden by features)
  - Feature configs sorted lexicographically (last has highest priority via right-hand-wins)
  - `plus` remains functional for users adding manual configs on top
- **Acceptance Criteria**:
  - [ ] All `NavigationConfig` members delegate to the lazy composite
  - [ ] Own module config + all discovered feature configs are chained
  - [ ] `lazy` initialization used
  - [ ] Deterministic ordering
  - [ ] Compiles on all KMP targets

#### Task 9: IR — Call-Site Transformation for `navigationConfig<T>()`

- **Description**: Transform calls to `navigationConfig<T>()` in user code, replacing them with direct references to the generated aggregated config object.
- **Files**:
  - `quo-vadis-compiler-plugin/.../ir/QuoVadisIrGenerationExtension.kt` — Register the IR transformer
  - `quo-vadis-compiler-plugin/.../ir/NavigationConfigCallTransformer.kt` (new) — Call-site transformation logic
- **Technical approach**:
  1. Create an `IrElementTransformer` that visits `IrCall` nodes
  2. Match calls to `navigationConfig()` by FQN (`com.jermey.quo.vadis.core.navigation.config.navigationConfig`)
  3. Extract the reified type argument from the call
  4. Look up the corresponding `{Prefix}__AggregatedConfig` object (mapped from `@NavigationRoot`-annotated class)
  5. Replace the `IrCall` with `IrGetObjectValue` referencing the aggregated config
  6. If no matching `@NavigationRoot` is found, the FIR checker (Task 3) already emitted an error — no IR action needed
- **Acceptance Criteria**:
  - [ ] `navigationConfig<AppNavigation>()` is replaced with `AppNavigation__AggregatedConfig.INSTANCE`
  - [ ] The replacement is transparent — callers get a `NavigationConfig` reference
  - [ ] Works in all call contexts (local variable, function argument, property initializer)
  - [ ] No runtime error from the stub body (it's completely replaced)

#### Task 10: Update Demo App (composeApp)

- **Description**: Replace the manual config composition in `DI.kt` with the new `navigationConfig<AppNavigation>()` call.
- **File**: `composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DI.kt`
- **Changes**:
  ```kotlin
  // Before
  @Module
  class NavigationModule {
      @Single
      fun navigationConfig(): NavigationConfig =
          ComposeAppNavigationConfig +
              Feature1NavigationConfig +
              Feature2NavigationConfig

      @Single
      fun navigator(navigationConfig: NavigationConfig): Navigator {
          val rootDestination = MainTabs::class
          val initialState = navigationConfig.buildNavNode(...)
          return TreeNavigator(config = navigationConfig, initialState = initialState)
      }
  }

  // After
  @Module
  class NavigationModule {
      @Single
      fun navigator(): Navigator {
          val navigationConfig = navigationConfig<AppNavigation>()
          val rootDestination = MainTabs::class
          val initialState = navigationConfig.buildNavNode(
              destinationClass = rootDestination,
              parentKey = null
          ) ?: error(
              "No container registered for ${rootDestination.simpleName}. " +
                      "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                      "or manually registered in the NavigationConfig."
          )
          return TreeNavigator(
              config = navigationConfig,
              initialState = initialState
          )
      }
  }
  ```
- **Also remove**: Unused imports for `ComposeAppNavigationConfig`, `Feature1NavigationConfig`, `Feature2NavigationConfig`
- **Acceptance Criteria**:
  - [ ] `DI.kt` uses `navigationConfig<AppNavigation>()` exclusively
  - [ ] No manual `+` chaining
  - [ ] No imports from generated config objects
  - [ ] Demo app builds and runs on all targets (Desktop, Android, iOS, Web)
  - [ ] All navigation patterns work: tabs, stacks, panes, transitions, deep links

---

## Sequencing

```
Phase 3 Prerequisites (can be parallelized):
P1 (panes) ──┐
P2 (transitions) ──┤──→ Integration test: compiler plugin generates working configs
P3 (pane roles) ──┤
P4 (deep links) ──┘

Phase 4 Core Path:
Task 1 (FIR predicate) ──→ Task 2 (single-root checker) ──→ Task 6 (FIR root synthesis)
       │                                                              │
Task 3 (type validation checker) ────────────────────────────→ Task 9 (call-site transform)
       │                                                              │
Task 4 (stub function) ──────────────────────────────────────→ Task 9 (call-site transform)
       │                                                              │
Task 5 (annotation retention) ──→ Task 7 (classpath scan) ──→ Task 8 (aggregated body)
                                                                      │
                                                               Task 9 (call-site transform)
                                                                      │
                                                               Task 10 (demo app update)
```

**Critical path**: Tasks 1 → 6 → 7 → 8 → 9 → 10  
**Parallel tracks**: Tasks 2, 3, 4, 5 can execute in parallel with Tasks 1, 6

---

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| `IrPluginContext` cannot enumerate dependency classes by supertype | High | Fall back to resource embedding (META-INF) or known-package scanning. Research during Task 7. |
| Reified inline function + IR transformation interaction with Kotlin compiler versions | Medium | Test against Kotlin 2.0+ versions. The `IrCall` visiting pattern is stable. |
| Naming collision with existing `navigationConfig { builder }` DSL | Low | Different signatures (no lambda vs lambda). If imports conflict, use FQN or rename. |
| Incremental compilation may cache stale aggregated config | Medium | Gradle task dependency ensures recompilation when feature modules change. Test explicitly. |
| Compose compiler interaction with generated `lazy` delegate | Low | Aggregated config has no `@Composable` code — composable dispatch lives in individual module configs. |

---

## Files Inventory

### New Files

| File | Purpose |
|------|---------|
| `quo-vadis-core/.../config/NavigationConfigDsl.kt` | `navigationConfig<T>()` reified inline function |
| `quo-vadis-compiler-plugin/.../ir/MultiModuleDiscovery.kt` | Classpath scanning for `GeneratedNavigationConfig` |
| `quo-vadis-compiler-plugin/.../ir/NavigationConfigCallTransformer.kt` | IR call-site transformation |

### Modified Files

| File | Change |
|------|--------|
| `quo-vadis-compiler-plugin/.../fir/QuoVadisPredicates.kt` | Add `HAS_NAVIGATION_ROOT` predicate |
| `quo-vadis-compiler-plugin/.../fir/QuoVadisDiagnostics.kt` | Add `MULTIPLE_NAVIGATION_ROOTS`, `NAVIGATION_ROOT_REQUIRED` errors |
| `quo-vadis-compiler-plugin/.../fir/QuoVadisDeclarationChecker.kt` | Single-root validation, `navigationConfig<T>()` type validation |
| `quo-vadis-compiler-plugin/.../fir/QuoVadisDeclarationGenerationExtension.kt` | `@NavigationRoot` detection, aggregated config synthesis |
| `quo-vadis-compiler-plugin/.../ir/QuoVadisIrGenerationExtension.kt` | Wire aggregated body generation + call transformer |
| `quo-vadis-compiler-plugin/.../ir/BodySynthesisTransformer.kt` | Dispatch for aggregated config type |
| `quo-vadis-compiler-plugin/.../ir/generators/BaseConfigIrGenerator.kt` | Add pane + transition registration |
| `quo-vadis-compiler-plugin/.../ir/generators/PaneRoleRegistryIrGenerator.kt` | Real when-based dispatch |
| `quo-vadis-compiler-plugin/.../ir/generators/DeepLinkHandlerIrGenerator.kt` | Full method implementations |
| `quo-vadis-annotations/.../Stack.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../Destination.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../Screen.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../TabAnnotations.kt` | Retention: SOURCE → BINARY |
| `quo-vadis-annotations/.../PaneAnnotations.kt` | Retention: SOURCE → BINARY |
| `composeApp/.../DI.kt` | Replace manual composition with `navigationConfig<AppNavigation>()` |
