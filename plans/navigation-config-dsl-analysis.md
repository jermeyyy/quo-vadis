# Analysis Report: NavigationConfig System, Compiler Plugin, and `navigationConfig()` DSL Function

**Date**: 3 March 2026  
**Status**: Analysis Complete

---

## 1. Executive Summary

This report analyzes the current NavigationConfig system, the compiler plugin's FIR/IR generation pipeline, and the design space for a `navigationConfig()` DSL function that replaces manual `+` operator composition with auto-discovered, compiler-plugin-generated aggregation.

### Key Findings

1. **`NavigationConfig`** is an interface in `quo-vadis-core` with 6 sub-registries, a `buildNavNode()` factory, and a `plus()` composition operator.
2. **`CompositeNavigationConfig`** implements `plus()` with right-hand-wins priority semantics.
3. **The compiler plugin** already has a functional FIR + IR pipeline that generates `{Prefix}NavigationConfig` objects per module.
4. **`@NavigationRoot`** annotation exists in `quo-vadis-annotations` but has **NO compiler plugin processing logic yet** — the FIR/IR code has no reference to `NavigationRoot`.
5. **`GeneratedNavigationConfig`** marker interface exists in `quo-vadis-core` and is already used as the supertype in FIR-generated configs.
6. **The DI.kt file** shows both the current state (manual `+` chaining) and the desired state (a `navigationConfig()` function call), side by side.
7. **Comprehensive plans exist** in `plans/compiler-plugin/04-multi-module.md` with detailed tasks for implementing `@NavigationRoot` auto-discovery.

---

## 2. NavigationConfig Interface

**Location**: [quo-vadis-core/.../navigation/config/NavigationConfig.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/NavigationConfig.kt)

### Interface Shape

```kotlin
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkRegistry: DeepLinkRegistry
    val paneRoleRegistry: PaneRoleRegistry  // default: PaneRoleRegistry.Empty

    fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?

    operator fun plus(other: NavigationConfig): NavigationConfig

    companion object {
        val Empty: NavigationConfig  // identity element for plus
    }
}
```

### Sub-Registry Interfaces

| Registry | Key Methods | Purpose |
|----------|-------------|---------|
| `ScreenRegistry` | `@Composable Content(destination, sharedTransitionScope?, animatedVisibilityScope?)`, `hasContent(destination)` | Maps destinations to composable screens |
| `ScopeRegistry` | `isInScope(scopeKey, destination)`, `getScopeKey(destination)` | Scope membership lookups |
| `TransitionRegistry` | `getTransition(destinationClass): NavTransition?` | Transition animation lookup |
| `ContainerRegistry` | `getContainerInfo(destination)`, `@Composable TabsContainer(...)`, `@Composable PaneContainer(...)` | Container building + wrapper rendering |
| `DeepLinkRegistry` | `resolve(uri)`, `handle(uri, navigator)`, `canHandle(uri)`, etc. | Deep link resolution |
| `PaneRoleRegistry` | `getPaneRole(scopeKey, destination)`, `hasPaneRole(...)` | Pane role mapping |

---

## 3. CompositeNavigationConfig

**Location**: [quo-vadis-core/.../internal/config/CompositeNavigationConfig.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/internal/config/CompositeNavigationConfig.kt)

```kotlin
class CompositeNavigationConfig(
    private val primary: NavigationConfig,
    private val secondary: NavigationConfig
) : NavigationConfig { ... }
```

### Composition Semantics

- **Right-hand wins**: `secondary` (the right operand of `+`) is checked first, `primary` is fallback.
- **Screen dispatch**: tries `secondary.screenRegistry` first, falls back to `primary.screenRegistry`.
- **Container info**: tries `secondary` first, then `primary`.
- **Scope**: destination is in scope if **either** registry says so.
- **Transitions**: `secondary` takes priority.
- **Deep links**: tries `secondary` first, then `primary`.
- **Pane roles**: `secondary` takes priority.

### Usage Pattern (Current State)

```kotlin
val appConfig = ComposeAppNavigationConfig +
    Feature1NavigationConfig +
    Feature2NavigationConfig
```

This creates a nested chain: `Composite(Composite(ComposeApp, Feature1), Feature2)`.

---

## 4. Current DI Setup (composeApp)

**Location**: [composeApp/.../demo/DI.kt](composeApp/src/commonMain/kotlin/com/jermey/navplayground/demo/DI.kt)

The file contains **two versions** of `NavigationModule` (the second appears to be a forward-looking placeholder):

### Current (lines 18–43) — Manual `+` Composition

```kotlin
@NavigationRoot
object AppNavigation

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
        val initialState = navigationConfig.buildNavNode(
            destinationClass = rootDestination, parentKey = null
        ) ?: error(...)
        return TreeNavigator(config = navigationConfig, initialState = initialState)
    }
}
```

### Desired (lines 47–66) — Auto-Discovered `navigationConfig()`

```kotlin
@Module
class NavigationModule {
    @Single
    fun navigator(): Navigator {
        val navigationConfig = navigationConfig() // or navigationConfig<AppNavigation>()
        val rootDestination = MainTabs::class
        val initialState = navigationConfig.buildNavNode(
            destinationClass = rootDestination, parentKey = null
        ) ?: error(...)
        return TreeNavigator(config = navigationConfig, initialState = initialState)
    }
}
```

**Key observations**:
- `@NavigationRoot` is already applied to `object AppNavigation` (line 15–16).
- The desired API is `navigationConfig()` — a generated top-level function that returns the aggregated `NavigationConfig`.
- An alternative API `navigationConfig<AppNavigation>()` is hinted at, giving explicit control over which root to use.
- The `NavigationConfig` parameter is removed from `navigator()` — it's self-contained.

---

## 5. Existing `navigationConfig { }` DSL in quo-vadis-core

**Location**: [quo-vadis-core/.../dsl/NavigationConfigBuilder.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/dsl/NavigationConfigBuilder.kt) (line 519)

```kotlin
fun navigationConfig(builder: NavigationConfigBuilder.() -> Unit): NavigationConfig {
    return NavigationConfigBuilder().apply(builder).build()
}
```

This is the **runtime DSL builder** function used internally by generated configs for their `baseConfig`:

```kotlin
private val baseConfig by lazy {
    navigationConfig {
        tabs<MainTabs>(scopeKey = "mainTabs") { ... }
        stack<HomeDestination>(scopeKey = "home", ...) { ... }
        scope("mainTabs", HomeTab::class, ExploreTab::class, ...)
        transition<DetailsDestination>(NavTransition.SlideVertical)
        deepLink("home/feed") { HomeDestination.Feed }
    }
}
```

**Important distinction**: The existing `navigationConfig { }` (with lambda) is the DSL builder. The new `navigationConfig()` (no lambda) would be a completely different function — a generated top-level function for auto-discovery aggregation.

### Naming Collision Risk

The existing `fun navigationConfig(builder: ...)` and the desired `fun navigationConfig(): NavigationConfig` have different signatures (one takes a lambda, one is parameterless), so they can coexist. However, import disambiguation may be needed if both are used in the same file.

---

## 6. Annotations in `quo-vadis-annotations`

**Location**: [quo-vadis-annotations/.../annotations/](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/)

| Annotation | File | Target | Retention | Key Parameters |
|-----------|------|--------|-----------|----------------|
| `@Stack` | Stack.kt | CLASS | SOURCE | `name: String`, `startDestination: KClass<*>` |
| `@Destination` | Destination.kt | CLASS | SOURCE | `route: String = ""` |
| `@Screen` | Screen.kt | FUNCTION | SOURCE | `destination: KClass<*>` |
| `@Argument` | Argument.kt | VALUE_PARAMETER | SOURCE | `key: String = ""`, `optional: Boolean = false` |
| `@Tabs` | TabAnnotations.kt | CLASS | SOURCE | `name: String`, `initialTab: KClass<*>`, `items: Array<KClass<*>>` |
| `@TabItem` | TabAnnotations.kt | CLASS | SOURCE | *(marker)* |
| `@Pane` | PaneAnnotations.kt | CLASS | SOURCE | `name: String`, `backBehavior: PaneBackBehavior` |
| `@PaneItem` | PaneAnnotations.kt | CLASS | SOURCE | `role: PaneRole`, `adaptStrategy: AdaptStrategy` |
| `@TabsContainer` | TabsContainer.kt | FUNCTION | RUNTIME | `tabClass: KClass<*>` |
| `@PaneContainer` | PaneContainer.kt | FUNCTION | RUNTIME | `paneClass: KClass<*>` |
| `@Transition` | Transition.kt | CLASS | RUNTIME | `type: TransitionType`, `customTransition: KClass<*>` |
| **`@NavigationRoot`** | **NavigationRoot.kt** | **CLASS** | **BINARY** | **`prefix: String = ""`** |

### NavigationRoot Annotation (Already Exists)

```kotlin
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class NavigationRoot(
    val prefix: String = "",
)
```

Notable: It has `BINARY` retention (correct for cross-module visibility) and a `prefix` parameter for naming the generated config.

---

## 7. Compiler Plugin Architecture

### Module Structure

| File | Purpose |
|------|---------|
| `QuoVadisCompilerPluginRegistrar.kt` | SPI entry point, registers FIR + IR extensions |
| `QuoVadisCommandLineProcessor.kt` | Parses `modulePrefix` from Gradle |
| `QuoVadisConfigurationKeys.kt` | Configuration key definitions |
| `QuoVadisGeneratedKey.kt` | Plugin origin key for marking synthetic declarations |

### 7.1 FIR Phase

#### Predicates (`QuoVadisPredicates.kt`)

Registers `DeclarationPredicate` for all 11 navigation annotations (Stack, Destination, Screen, Tabs, TabItem, Pane, PaneItem, TabsContainer, PaneContainer, Transition, Argument). **Note: No predicate for `@NavigationRoot` yet.**

#### Declaration Generation (`QuoVadisDeclarationGenerationExtension.kt`)

Generates **three synthetic declarations** per module in package `com.jermey.quo.vadis.generated`:

| Synthetic Class | ClassKind | Supertype |
|----------------|-----------|-----------|
| `{Prefix}NavigationConfig` | OBJECT | `GeneratedNavigationConfig` |
| `{Prefix}DeepLinkHandler` | OBJECT | `DeepLinkRegistry` |
| `{Prefix}ScreenRegistryImpl` | CLASS | `ScreenRegistry` |

Key implementation details:
- **Source-set deduplication**: Only generates in root source set (checks `session.moduleData.dependsOnDependencies.isEmpty()`) to prevent duplicate declarations across KMP source sets.
- **Config properties**: `screenRegistry`, `scopeRegistry`, `transitionRegistry`, `containerRegistry`, `deepLinkRegistry`, `paneRoleRegistry`, `roots`.
- **Config functions**: `buildNavNode(...)`, `plus(...)`.
- **Deep link functions**: `resolve(uri)`, `resolve(deepLink)`, `register(...)`, `registerAction(...)`, `handle(...)`, `createUri(...)`, `canHandle(...)`, `getRegisteredPatterns()`, `handleDeepLink(...)`.
- **ScreenRegistry functions**: `@Composable Content(...)`, `hasContent(...)`.

#### FIR Checkers

| Checker | Purpose |
|---------|---------|
| `ArgumentParityChecker` | Validates `@Argument` parameters match route placeholders |
| `StructuralChecker` | Validates structural constraints (sealed classes, etc.) |
| `RouteCollisionChecker` | Detects duplicate route strings |
| `TransitionCompatibilityChecker` | Validates transition/container compatibility |
| `ContainerRoleChecker` | Validates pane role assignments |
| `ScreenValidationChecker` | Validates `@Screen` function signatures |

**Note: No checker for `@NavigationRoot` constraints (single-root validation) yet.**

### 7.2 IR Phase

#### Two-Pass Strategy

**Pass 1 — `StubMaterializationTransformer`**: Traverses `IrModuleFragment` to locate FIR-generated synthetic classes by matching expected names (`{Prefix}NavigationConfig`, `{Prefix}DeepLinkHandler`, `{Prefix}ScreenRegistryImpl`). Collects them into `SynthesizedDeclarations`.

**Pass 2 — `BodySynthesisTransformer`**: Visits each synthetic class and dispatches to the appropriate body generator:

| Class | Generator |
|-------|-----------|
| `{Prefix}NavigationConfig` | `NavigationConfigIrGenerator` |
| `{Prefix}DeepLinkHandler` | `DeepLinkHandlerIrGenerator` |
| `{Prefix}ScreenRegistryImpl` | `ScreenRegistryIrGenerator` |

#### NavigationConfigIrGenerator

Generates bodies for the config object's properties and functions:

| Method | What It Generates |
|--------|-------------------|
| `generateBaseConfigDelegatedProperty()` | Lazy `baseConfig` using the runtime DSL `navigationConfig { ... }` |
| `generateScreenRegistryProperty()` | Property returning a `ScreenRegistryImpl` instance |
| `generatePaneRoleRegistryProperty()` | Property delegating to `PaneRoleRegistryIrGenerator` |
| `generateDeepLinkRegistryProperty()` | Property returning the `DeepLinkHandler` singleton |
| `generateRootsProperty()` | `Set<KClass<out NavDestination>>` of root container classes |
| `generateBuildNavNodeBody()` | Delegates to `baseConfig.buildNavNode(...)` |
| `generatePlusBody()` | Returns `CompositeNavigationConfig(this, other)` |

#### DSL Sub-Generators

| Generator | Purpose |
|-----------|---------|
| `StackDslIrGenerator` | Generates `stack<T>(...)` DSL calls |
| `TabsDslIrGenerator` | Generates `tabs<T>(...)` DSL calls |
| `PanesDslIrGenerator` | Generates `panes<T>(...)` DSL calls |
| `TransitionDslIrGenerator` | Generates `transition<T>(...)` DSL calls |
| `ScopeDslIrGenerator` | Generates `scope(...)` DSL calls |

#### Supporting IR Infrastructure

| Class | Purpose |
|-------|---------|
| `IrMetadataCollector` | Collects `NavigationMetadata` from annotations in the current module |
| `SymbolResolver` | Resolves `IrClassSymbol`/`IrFunctionSymbol` from `IrPluginContext` |
| `NavigationMetadata` | Data class holding all extracted stack/destination/tab/pane/screen/transition/container metadata |
| `BaseConfigIrGenerator` | Generates the lazy `_baseConfig` backing field with DSL builder calls |

---

## 8. KSP Code Generation (Current, Being Replaced)

### Pipeline

```
Collection (7 Extractors) → Validation (ValidationEngine) → Generation (2 Generators)
```

### Generated Outputs (per module)

1. **`{Prefix}NavigationConfig.kt`** — `object : NavigationConfig` with:
   - Lazy `baseConfig` using runtime DSL
   - `when`-based `ScreenRegistry` dispatch
   - Composite `ContainerRegistry` (DSL for building, `when` for rendering)
   - Delegated `ScopeRegistry`, `TransitionRegistry` to `baseConfig`
   - `PaneRoleRegistry` with `when`-based dispatch
   - `roots` property

2. **`{Prefix}DeepLinkHandler.kt`** — `object : DeepLinkRegistry` with regex-based route matching

---

## 9. `GeneratedNavigationConfig` Marker Interface

**Location**: [quo-vadis-core/.../config/GeneratedNavigationConfig.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/GeneratedNavigationConfig.kt)

```kotlin
interface GeneratedNavigationConfig : NavigationConfig
```

Already exists and is already used as the FIR supertype for compiler-plugin-generated configs (visible in `QuoVadisDeclarationGenerationExtension` line ~296: `superType(generatedNavigationConfigId.createConeType(session))`).

**Purpose**: Enables precise classpath scanning — only compiler-plugin-generated configs implement this, excluding user-defined manual configs and KSP-generated configs (backward compat).

---

## 10. Current State vs Desired State

### Current State

```kotlin
// composeApp/.../DI.kt
@NavigationRoot
object AppNavigation

@Module
class NavigationModule {
    @Single
    fun navigationConfig(): NavigationConfig =
        ComposeAppNavigationConfig +    // explicit import
            Feature1NavigationConfig +  // explicit import
            Feature2NavigationConfig    // explicit import

    @Single
    fun navigator(navigationConfig: NavigationConfig): Navigator { ... }
}
```

**Problems**:
- Must explicitly import every feature module's generated config
- Must manually chain with `+` operator
- Adding/removing a module requires updating DI code
- Compile error if any import is missing; silent breakage if one is forgotten

### Desired State

```kotlin
// composeApp/.../DI.kt
@NavigationRoot
object AppNavigation

@Module
class NavigationModule {
    @Single
    fun navigator(): Navigator {
        val navigationConfig = navigationConfig() // auto-discovers all
        ...
        return TreeNavigator(config = navigationConfig, initialState = initialState)
    }
}
```

**Benefits**:
- Zero imports from feature modules
- Zero manual chaining
- Adding a feature module = adding its Gradle dependency
- Compiler plugin auto-discovers `GeneratedNavigationConfig` implementors from classpath

---

## 11. Design Space for `navigationConfig()` DSL Function

### Option A: Compiler-Plugin-Generated Top-Level Function

The compiler plugin generates a top-level function in `com.jermey.quo.vadis.generated`:

```kotlin
// Generated by compiler plugin when @NavigationRoot is found
fun navigationConfig(): NavigationConfig = AppNavigationNavigationConfig
```

Where `AppNavigationNavigationConfig` is the generated aggregated config object.

**Pros**: Simple, looks like a regular function call, discoverable.  
**Cons**: Name collision with existing `navigationConfig { }` DSL builder in `quo-vadis-core.dsl`.

### Option B: Generated as a Property on the Root Object

```kotlin
// AppNavigation.navigationConfig is auto-generated
val config = AppNavigation.navigationConfig
```

**Pros**: Explicit, no naming collision.  
**Cons**: Requires referencing the root object.

### Option C: Direct Use of the Generated Config Object (Current Plan)

```kotlin
val config = MyAppNavigationConfig  // generated aggregated singleton
```

This is what the `plans/compiler-plugin/04-multi-module.md` plan describes. The `{Prefix}NavigationConfig` IS the aggregated config.

**Pros**: Consistent with current per-module pattern, no new API surface needed.  
**Cons**: Still requires an import, though only one.

### Option D: Generated `navigationConfig()` with Reified Type Parameter

```kotlin
val config = navigationConfig<AppNavigation>()
```

**Pros**: Type-safe, works well when multiple roots exist (though that's disallowed).  
**Cons**: More complex to generate in the compiler plugin's FIR/IR.

### Recommended Approach

Based on the existing plan in `04-multi-module.md` and the DI.kt desired state, the most pragmatic approach is:

1. **Compiler plugin generates the aggregated `{Prefix}NavigationConfig` object** (as planned in Phase 4C tasks).
2. **Optionally generate a `navigationConfig()` top-level function** as sugar that returns the aggregated config. This can be a parameterless function in the generated package.
3. Name it carefully to avoid collision with the DSL builder — either:
   - Use the full package: `com.jermey.quo.vadis.generated.navigationConfig()`
   - Or rename to `quoVadisNavigationConfig()` or `aggregatedNavigationConfig()`
   - Or just use the config object directly: `MyAppNavigationConfig`

---

## 12. Gap Analysis: What's Missing in the Compiler Plugin

| Component | Status | What's Missing |
|-----------|--------|----------------|
| `@NavigationRoot` annotation | ✅ Exists | — |
| `GeneratedNavigationConfig` marker interface | ✅ Exists | — |
| FIR predicate for `@NavigationRoot` | ❌ Missing | No `NAVIGATION_ROOT_FQN` or `HAS_NAVIGATION_ROOT` in `QuoVadisPredicates` |
| FIR single-root validation checker | ❌ Missing | No checker ensuring only one `@NavigationRoot` per module |
| FIR root config synthesis | ❌ Missing | The FIR extension only generates per-module configs, not a root aggregated config |
| IR classpath scanning | ❌ Missing | No `MultiModuleDiscovery.kt` or classpath iteration logic |
| IR root config body generation | ❌ Missing | No aggregation logic chaining discovered configs via `+` |
| `navigationConfig()` generated function | ❌ Missing | No top-level function generation in FIR or IR |

### What IS Working

- Per-module `{Prefix}NavigationConfig` generation (FIR declarations + IR bodies)
- Per-module `{Prefix}DeepLinkHandler` generation
- Per-module `{Prefix}ScreenRegistryImpl` generation
- 6 FIR diagnostic checkers for validation
- All 11 annotation predicates (except `@NavigationRoot`)
- Metadata collection from annotations (`IrMetadataCollector`)
- DSL IR generation (stack, tabs, panes, scope, transition) via sub-generators

---

## 13. Key Types and Patterns Summary

### Core Runtime Types

| Type | Package | Role |
|------|---------|------|
| `NavigationConfig` | `core.navigation.config` | Main config interface (6 registries + builder + composition) |
| `GeneratedNavigationConfig` | `core.navigation.config` | Marker for compiler-generated configs |
| `CompositeNavigationConfig` | `core.navigation.internal.config` | `+` operator implementation (right-hand-wins) |
| `NavigationConfig.Empty` | `core.navigation.config` | Identity element for `+` |
| `navigationConfig { }` | `core.dsl` | Runtime DSL builder for programmatic config |

### Compiler Plugin Types

| Type | Role |
|------|------|
| `NavigationMetadata` | Aggregated metadata from annotation scanning |
| `SynthesizedDeclarations` | IR class references for the 3 generated classes |
| `QuoVadisPredicates` | FIR predicate definitions for annotation matching |
| `QuoVadisGeneratedKey` | Origin key linking synthetic declarations to the plugin |
| `SymbolResolver` | Resolves core library symbols for IR generation |

### Annotation → Generated Output Mapping

```
@Stack + @Destination    →  stack<T> DSL, scope registration, buildNavNode
@Tabs + @TabItem         →  tabs<T> DSL, scope registration
@Pane + @PaneItem        →  panes<T> DSL, PaneRoleRegistry entries
@Screen                  →  ScreenRegistry when-dispatch
@TabsContainer           →  ContainerRegistry.TabsContainer when-dispatch
@PaneContainer           →  ContainerRegistry.PaneContainer when-dispatch
@Transition              →  TransitionRegistry entry
@Argument                →  Deep link parameter extraction
@NavigationRoot          →  (PLANNED) Aggregated root config with auto-discovery
```

---

## 14. Open Questions

1. **`navigationConfig()` function naming**: Should the generated function be `navigationConfig()` (risk of collision with DSL builder) or something more explicit like `quoVadisConfig()` or just use the config object directly (`MyAppNavigationConfig`)?

2. **Classpath scanning strategy**: The plan discusses three strategies (metadata embedding, known-package scanning, supertype resolution). Which one to implement? Plan recommends metadata-driven (Strategy A) but acknowledges supertype resolution (Strategy C) is the most robust.

3. **FIR generation for root config**: Should the root config's FIR declaration be a new class with a different name than the per-module config (e.g., `AppAggregatedNavigationConfig` vs `ComposeAppNavigationConfig`), or should `@NavigationRoot` suppress per-module config generation and only produce the aggregated config?

4. **Dual NavigationModule in DI.kt**: The current DI.kt has two `NavigationModule` class definitions (a compile error). This needs to be resolved — the second one (lines 47–66) is the desired state placeholder that should replace the first.
