# KSP Implementation Analysis Report

**Date**: 2 March 2026  
**Purpose**: Comprehensive analysis of the current KSP processor to guide K2 compiler plugin migration

---

## 1. All Annotations Defined in `quo-vadis-annotations`

### 1.1 `@Stack` — [Stack.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Stack.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `name` | `String` | (required) | Unique name for stack, used for keys |
  | `startDestination` | `KClass<*>` | (required) | KClass of initial destination |
- **Semantics**: Applied to `sealed class`/`sealed interface` containing `@Destination` subclasses. Maps to `StackNode`.

### 1.2 `@Destination` — [Destination.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Destination.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `route` | `String` | `""` | Deep link route pattern. Empty = not deep-linkable |
- **Semantics**: Applied to `data object` or `data class` subclasses within a `@Stack`. Maps to `ScreenNode`.

### 1.3 `@Screen` — [Screen.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Screen.kt)
- **Target**: `AnnotationTarget.FUNCTION`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `destination` | `KClass<*>` | (required) | The destination class this composable renders |
- **Semantics**: Applied to `@Composable` functions. KSP detects parameter types (destination instance, `Navigator`, `SharedTransitionScope?`, `AnimatedVisibilityScope?`).

### 1.4 `@Argument` — [Argument.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Argument.kt)
- **Target**: `AnnotationTarget.VALUE_PARAMETER`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `key` | `String` | `""` | Custom URL parameter key. Defaults to param name |
  | `optional` | `Boolean` | `false` | Whether param can be omitted in deep links |
- **Semantics**: Marks constructor parameter as a navigation argument. Used for deep link serialization.

### 1.5 `@Tabs` — [TabAnnotations.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `name` | `String` | (required) | Unique identifier for tab container |
  | `initialTab` | `KClass<*>` | `Unit::class` | Initial active tab. `Unit::class` = first |
  | `items` | `Array<KClass<*>>` | `[]` | Array of tab item classes |
- **Semantics**: Defines a tabbed navigation container. Applied to `object` or `class`. Maps to `TabNode`.

### 1.6 `@TabItem` — [TabAnnotations.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabAnnotations.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `SOURCE`
- **Parameters**: None (marker annotation)
- **Semantics**: Marks a `@Stack`-annotated class as a tab within a `@Tabs` container. Used alongside `@Stack`.

### 1.7 `@Pane` — [PaneAnnotations.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `name` | `String` | (required) | Unique identifier for pane container |
  | `backBehavior` | `PaneBackBehavior` | `PopUntilScaffoldValueChange` | Back navigation behavior |
- **Semantics**: Applied to `sealed class`/`sealed interface`. Maps to `PaneNode`.

### 1.8 `@PaneItem` — [PaneAnnotations.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneAnnotations.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `SOURCE`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `role` | `PaneRole` | (required) | Layout role: `PRIMARY`, `SECONDARY`, `EXTRA` |
  | `adaptStrategy` | `AdaptStrategy` | `HIDE` | Behavior when space is limited |
- **Semantics**: Applied to sealed subclasses within `@Pane` container.

### 1.9 `@TabsContainer` — [TabsContainer.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/TabsContainer.kt)
- **Target**: `AnnotationTarget.FUNCTION`
- **Retention**: `RUNTIME`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `tabClass` | `KClass<*>` | (required) | The `@Tabs` class this composable wraps |
- **Semantics**: Applied to `@Composable` functions with signature `(scope: TabsContainerScope, content: @Composable () -> Unit)`.

### 1.10 `@PaneContainer` — [PaneContainer.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/PaneContainer.kt)
- **Target**: `AnnotationTarget.FUNCTION`
- **Retention**: `RUNTIME`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `paneClass` | `KClass<*>` | (required) | The `@Pane` class this composable wraps |
- **Semantics**: Applied to `@Composable` functions with signature `(scope: PaneContainerScope, content: @Composable () -> Unit)`.

### 1.11 `@Transition` — [Transition.kt](quo-vadis-annotations/src/commonMain/kotlin/com/jermey/quo/vadis/annotations/Transition.kt)
- **Target**: `AnnotationTarget.CLASS`
- **Retention**: `RUNTIME`
- **Parameters**:
  | Parameter | Type | Default | Description |
  |-----------|------|---------|-------------|
  | `type` | `TransitionType` | `SlideHorizontal` | Transition type enum |
  | `customTransition` | `KClass<*>` | `Unit::class` | Custom NavTransition class (for `Custom` type) |

### Annotation Enums

| Enum | Values | Location |
|------|--------|----------|
| `TransitionType` | `SlideHorizontal`, `SlideVertical`, `Fade`, `None`, `Custom` | Transition.kt |
| `PaneBackBehavior` | `PopUntilScaffoldValueChange`, `PopUntilContentChange`, `PopLatest` | PaneAnnotations.kt |
| `PaneRole` | `PRIMARY`, `SECONDARY`, `EXTRA` | PaneAnnotations.kt |
| `AdaptStrategy` | `HIDE`, `COLLAPSE`, `OVERLAY`, `REFLOW` | PaneAnnotations.kt |

---

## 2. What the KSP Processor Generates

The KSP processor produces **two files** per module:

### 2.1 `{ModulePrefix}NavigationConfig.kt`

**Location**: `com.jermey.quo.vadis.generated` package  
**Type**: `object` implementing `NavigationConfig`

#### Generated Structure:

```kotlin
object ComposeAppNavigationConfig : NavigationConfig {

    // Lazy-initialized DSL config for non-composable registries
    private val baseConfig by lazy {
        navigationConfig {
            // CONTAINERS section
            tabs<MainTabs>(scopeKey = "mainTabs") { ... }
            stack<HomeDestination>(scopeKey = "home", startDestination = ...) { ... }
            panes<CatalogPane>(scopeKey = "catalog", backBehavior = ...) { ... }

            // SCOPES section
            scope("mainTabs", HomeTab::class, ExploreTab::class, ...)
            scope("home", HomeDestination.Feed::class, HomeDestination.Detail::class, ...)

            // TRANSITIONS section
            transition<DetailsDestination>(NavTransition.SlideVertical)

            // DEEP LINKS section
            deepLink("home/feed") { HomeDestination.Feed }
            deepLink("home/detail/{id}") { params -> HomeDestination.Detail(params["id"]!!) }
        }
    }

    // Screen registry with when-based dispatch (NOT in DSL)
    override val screenRegistry: ScreenRegistry = object : ScreenRegistry {
        @Composable
        override fun Content(
            destination: NavDestination,
            sharedTransitionScope: SharedTransitionScope?,
            animatedVisibilityScope: AnimatedVisibilityScope?
        ) {
            when (destination) {
                is HomeDestination.Feed -> FeedScreen(...)
                is HomeDestination.Detail -> DetailScreen(destination = destination, ...)
                else -> error("No screen registered for destination: $destination")
            }
        }
        override fun hasContent(destination: NavDestination): Boolean {
            return when (destination) {
                is HomeDestination.Feed, is HomeDestination.Detail -> true
                else -> false
            }
        }
    }

    // Container registry with when-based wrapper dispatch + delegation for building
    override val containerRegistry: ContainerRegistry = object : ContainerRegistry {
        private val tabsContainerKeys = setOf("mainTabs")
        private val paneContainerKeys = setOf("catalog")

        override fun getContainerInfo(destination: NavDestination): ContainerInfo? =
            baseConfig.containerRegistry.getContainerInfo(destination)

        @Composable
        override fun TabsContainer(tabNodeKey: String, scope: TabsContainerScope, content: @Composable () -> Unit) {
            when (tabNodeKey) {
                "mainTabs" -> MainTabsWrapper(scope = scope, content = content)
                else -> content()
            }
        }

        @Composable
        override fun PaneContainer(paneNodeKey: String, scope: PaneContainerScope, content: @Composable () -> Unit) {
            when (paneNodeKey) {
                "catalog" -> CatalogPaneContainer(scope = scope, content = content)
                else -> content()
            }
        }

        override fun hasTabsContainer(tabNodeKey: String) = tabNodeKey in tabsContainerKeys
        override fun hasPaneContainer(paneNodeKey: String) = paneNodeKey in paneContainerKeys
    }

    // Delegated to baseConfig
    override val scopeRegistry: ScopeRegistry = baseConfig.scopeRegistry
    override val transitionRegistry: TransitionRegistry = baseConfig.transitionRegistry

    // Deep link registry references generated handler
    override val deepLinkRegistry: DeepLinkRegistry = ComposeAppDeepLinkHandler

    // Pane role registry with when-based dispatch
    override val paneRoleRegistry: PaneRoleRegistry = object : PaneRoleRegistry { ... }

    override fun buildNavNode(destinationClass: KClass<out NavDestination>, key: String?, parentKey: String?): NavNode? =
        baseConfig.buildNavNode(destinationClass, key, parentKey)

    override operator fun plus(other: NavigationConfig): NavigationConfig =
        CompositeNavigationConfig(this, other)

    val roots: Set<KClass<out NavDestination>> = setOf(MainTabs::class, ...)
}
```

**Key design decisions**:
- Screens and wrappers use **when-based dispatch** (not DSL lambdas) to avoid Compose compiler lambda casting issues
- DSL config is **lazy-initialized** and only contains non-composable registrations
- `containerRegistry` is a **merged** object: building from baseConfig, rendering from when-dispatch

### 2.2 `{ModulePrefix}DeepLinkHandler.kt`

**Location**: `com.jermey.quo.vadis.generated` package  
**Type**: `object` implementing `DeepLinkRegistry`

#### Generated Structure:

```kotlin
object ComposeAppDeepLinkHandler : DeepLinkRegistry {
    private val routes = listOf(
        ComposeAppRoutePattern("home/feed", emptyList()) { HomeDestination.Feed },
        ComposeAppRoutePattern("home/detail/{id}", listOf("id")) { params ->
            HomeDestination.Detail(id = params["id"]!!)
        }
    )
    private val patternStrings = routes.map { it.pattern }

    fun handleDeepLink(uri: String): DeepLinkResult { ... }
    override fun resolve(uri: String): NavDestination? { ... }
    override fun resolve(deepLink: DeepLink): NavDestination? { ... }
    override fun register(pattern: String, factory: ...) { /* no-op */ }
    override fun registerAction(pattern: String, action: ...) { /* no-op */ }
    override fun handle(uri: String, navigator: Navigator): Boolean { ... }
    override fun createUri(destination: NavDestination, scheme: String): String? { ... }
    override fun canHandle(uri: String): Boolean { ... }
    override fun getRegisteredPatterns(): List<String> = patternStrings
    private fun extractPath(uri: String): String { ... }
}

// Also generates a private helper class:
private data class ComposeAppRoutePattern(
    val pattern: String,
    val paramNames: List<String>,
    val createDestination: (Map<String, String>) -> NavDestination
) {
    private val regex: Regex = buildRegex()
    private fun buildRegex(): Regex { ... }
    fun match(path: String): Map<String, String>? { ... }
}
```

**Deep link type serialization support**:
- `String`: direct
- `Int/Long/Float/Double`: `.toXxx()` / `?.toXxxOrNull()` for optionals
- `Boolean`: `.toBoolean()` / `?.toBooleanStrictOrNull()`
- `Enum<T>`: `enumValueOf<T>()`
- `JSON/Serializable`: fallback to String (logged warning)

---

## 3. `NavigationConfig` Interface Contract

**Location**: [quo-vadis-core/.../NavigationConfig.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/config/NavigationConfig.kt)

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

| Registry | Key Methods | Location |
|----------|-------------|----------|
| `ScreenRegistry` | `@Composable Content(destination, sharedTransitionScope?, animatedVisibilityScope?)`, `hasContent(destination)` | [ScreenRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ScreenRegistry.kt) |
| `ScopeRegistry` | `isInScope(scopeKey, destination)`, `getScopeKey(destination)` | [ScopeRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ScopeRegistry.kt) |
| `TransitionRegistry` | `getTransition(destinationClass): NavTransition?` | [TransitionRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/TransitionRegistry.kt) |
| `ContainerRegistry` | `getContainerInfo(destination)`, `@Composable TabsContainer(key, scope, content)`, `@Composable PaneContainer(key, scope, content)`, `hasTabsContainer(key)`, `hasPaneContainer(key)` | [ContainerRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/ContainerRegistry.kt) |
| `DeepLinkRegistry` | `resolve(uri)`, `resolve(deepLink)`, `register(pattern, factory)`, `registerAction(pattern, action)`, `handle(uri, navigator)`, `createUri(destination, scheme)`, `canHandle(uri)`, `getRegisteredPatterns()` | [DeepLinkRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/DeepLinkRegistry.kt) |
| `PaneRoleRegistry` | `getPaneRole(scopeKey, destination)`, `getPaneRole(scopeKey, destinationClass)`, `hasPaneRole(scopeKey, destination)` | [PaneRoleRegistry.kt](quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/registry/PaneRoleRegistry.kt) |

### `ContainerInfo` Sealed Interface

```kotlin
sealed interface ContainerInfo {
    val scopeKey: ScopeKey
    val containerClass: KClass<out NavDestination>

    data class TabContainer(
        val builder: (key: NodeKey, parentKey: NodeKey?, initialTabIndex: Int) -> TabNode,
        val initialTabIndex: Int,
        override val scopeKey: ScopeKey,
        override val containerClass: KClass<out NavDestination>
    ) : ContainerInfo

    data class PaneContainer(
        val builder: (key: NodeKey, parentKey: NodeKey?) -> PaneNode,
        val initialPane: PaneRole,
        override val scopeKey: ScopeKey,
        override val containerClass: KClass<out NavDestination>
    ) : ContainerInfo
}
```

---

## 4. Deep Link Handling

There is no standalone `DeepLinkHandler` interface. Deep linking is handled through:

1. **`DeepLinkRegistry` interface** — the contract the generated `{ModulePrefix}DeepLinkHandler` implements
2. **Generated `{ModulePrefix}DeepLinkHandler` object** — KSP generates route pattern matching with regex
3. **`DeepLink` data class** — core utility for parsing URIs (used by `resolve(deepLink)`)
4. **`DeepLinkResult`** — sealed class with `Matched(destination)` and `NotMatched`

The generated handler also provides a convenience method `handleDeepLink(uri): DeepLinkResult` on top of the interface.

---

## 5. Gradle Plugin

**Location**: [QuoVadisPlugin.kt](quo-vadis-gradle-plugin/src/main/kotlin/com/jermey/quo/vadis/gradle/QuoVadisPlugin.kt)

### What it does:
1. Creates `QuoVadisExtension` with `quoVadis { }` DSL block
2. Validates that both KSP and Kotlin Multiplatform plugins are applied
3. Adds `kspCommonMainMetadata` dependency (either local project or Maven artifact)
4. Passes `quoVadis.modulePrefix` as KSP argument
5. Registers `build/generated/ksp/metadata/commonMain/kotlin` as source directory
6. Sets up task dependencies: all non-KSP/non-Test compilation tasks depend on `kspCommonMainKotlinMetadata`

### Configuration Options (`QuoVadisExtension`):

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `modulePrefix` | `Property<String>` | `project.name.toCamelCase()` | Prefix for generated class names |
| `useLocalKsp` | `Property<Boolean>` | `false` | Use project dependency instead of Maven |

### KSP Options:

| Option | Default | Description |
|--------|---------|-------------|
| `quoVadis.package` | `com.jermey.quo.vadis.generated` | Target package |
| `quoVadis.modulePrefix` | From extension | Class name prefix |
| `quoVadis.strictValidation` | `true` | Abort on validation errors |

---

## 6. Multi-Module Config Merging (`+` Operator)

The `plus` operator is implemented via `CompositeNavigationConfig`:

```kotlin
// In generated code:
override operator fun plus(other: NavigationConfig): NavigationConfig =
    CompositeNavigationConfig(this, other)
```

`CompositeNavigationConfig` (internal class in quo-vadis-core) creates composite implementations of each registry:
- **Priority**: `other` (right-hand) is checked first, `this` (left-hand) is fallback
- **Screen dispatch**: tries `other.screenRegistry` first, falls back to `this.screenRegistry`
- **Container info**: tries `other` first, then `this`
- **Scope**: destination is in scope if either registry says so
- **Transitions**: `other` takes priority
- **Deep links**: tries `other` first, then `this`
- **Pane roles**: `other` takes priority

**Usage pattern**:
```kotlin
val appConfig = feature1Config + feature2Config + feature3Config
val navigator = rememberQuoVadisNavigator(MainTabs::class, appConfig)
```

---

## 7. Runtime Types Referenced by Generated Code

The generated code references these types from `quo-vadis-core`:

### Navigation Config & Registries
| Type | Package | Usage |
|------|---------|-------|
| `NavigationConfig` | `core.navigation.config` | Interface implemented by generated object |
| `ScreenRegistry` | `core.registry` | Screen content mapping |
| `ScopeRegistry` | `core.registry` | Scope membership |
| `TransitionRegistry` | `core.registry` | Transition lookup |
| `ContainerRegistry` | `core.registry` | Container building + wrapper rendering |
| `ContainerInfo` | `core.registry` | Container metadata (TabContainer, PaneContainer) |
| `DeepLinkRegistry` | `core.registry` | Deep link handling |
| `PaneRoleRegistry` | `core.registry` | Pane role mapping |
| `CompositeNavigationConfig` | `core.navigation.internal.config` | `plus` operator implementation |

### Navigation DSL
| Type | Package | Usage |
|------|---------|-------|
| `navigationConfig { }` | `core.dsl` | DSL builder function for baseConfig |

### NavNode Types
| Type | Package | Usage |
|------|---------|-------|
| `NavNode` | `core.navigation` | Return type of `buildNavNode` |
| `NavDestination` | `core.navigation` | Base type for all destinations |
| `ScopeKey` | `core.registry` (or similar) | Key type for scope lookups |
| `PaneRole` | `core.navigation.pane` | Runtime pane roles (Primary, Supporting, Extra) |
| `PaneBackBehavior` | `core.navigation.pane` | Back behavior enum |

### Deep Linking
| Type | Package | Usage |
|------|---------|-------|
| `DeepLink` | `core.deeplink` (or similar) | URI parsing |
| `DeepLinkResult` | `core.deeplink` | Matched/NotMatched result |
| `Navigator` | `core.navigation.navigator` | Used in `handle()` method |

### Transition
| Type | Package | Usage |
|------|---------|-------|
| `NavTransition` | `core.compose.transition` | Transition configuration |
| `NavigationTransition` | (referenced in ClassNames) | Navigation transition type |

### Compose
| Type | Package | Usage |
|------|---------|-------|
| `@Composable` | `androidx.compose.runtime` | Screen/wrapper annotations |
| `SharedTransitionScope` | `androidx.compose.animation` | Shared element transitions |
| `AnimatedVisibilityScope` | `androidx.compose.animation` | Coordinated animations |
| `TabsContainerScope` | `core` (scope classes) | Scope for tab wrappers |
| `PaneContainerScope` | `core` (scope classes) | Scope for pane wrappers |

---

## 8. How `@Screen` Composable Functions Are Wired to Destinations

The wiring happens in three steps:

### Step 1: KSP Extraction (`ScreenExtractor`)
KSP scans for `@Screen` annotations on functions and extracts:
- Function name (e.g., `FeedScreen`)
- Package name
- Target destination class (from `@Screen(destination = ...)`)
- Parameter detection:
  - `hasDestinationParam`: whether the function accepts the destination type
  - `hasSharedTransitionScope`: whether it accepts `SharedTransitionScope?`
  - `hasAnimatedVisibilityScope`: whether it accepts `AnimatedVisibilityScope?`

### Step 2: Code Generation
The `NavigationConfigGenerator` generates an anonymous `ScreenRegistry` object with a `when`-based dispatch:

```kotlin
@Composable
override fun Content(destination: NavDestination, ...) {
    when (destination) {
        is HomeDestination.Feed -> FeedScreen()  // no dest param
        is HomeDestination.Detail -> DetailScreen(
            destination = destination,  // smart cast from when-is
            sharedTransitionScope = sharedTransitionScope!!,
            animatedVisibilityScope = animatedVisibilityScope!!
        )
        else -> error("No screen registered for destination: $destination")
    }
}
```

### Step 3: Runtime Resolution
`NavigationHost` calls `config.screenRegistry.Content(currentDestination)` to render the screen. The `when` expression uses Kotlin smart casts from the `is` check.

**Key point**: The `Navigator` is **NOT** passed as a parameter in the generated dispatch. Screen functions receive the navigator through composition locals (provided by `NavigationHost`).

---

## 9. `ContainerRegistry` and Code Generation

### What ContainerRegistry Does

`ContainerRegistry` has two responsibilities:

#### 1. Container Building (`getContainerInfo`)
When navigating to a destination that belongs to a `@Tabs` or `@Pane` container, the registry provides:
- A builder function to construct the `TabNode`/`PaneNode` tree
- The initial tab index or pane role
- The scope key for scope-aware navigation

This is generated via the DSL (`tabs<T> { ... }`, `stack<T> { ... }`, `panes<T> { ... }`) in `baseConfig`.

#### 2. Wrapper Rendering (`TabsContainer`, `PaneContainer`)
Generated as `when`-based dispatch to user-defined `@TabsContainer`/`@PaneContainer` composable functions. Uses scope keys to match.

### How It Relates to Code Generation

The `ContainerBlockGenerator` (sub-generator) produces DSL blocks for:
- **Stacks**: `stack<HomeDestination>` with scopeKey and sorted destinations
- **Tabs**: `tabs<MainTabs>` with initialTabIndex, NESTED_STACK/FLAT_SCREEN tab items
- **Panes**: `panes<CatalogPane>` with PaneRole mappings, backBehavior, adaptStrategy

The `WrapperBlockGenerator` tracks wrapper function imports for the when-dispatch.

---

## KSP Processor Architecture Summary

### Processing Pipeline

```
┌─────────────────────────────────────────────────────────┐
│                QuoVadisSymbolProcessor                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  Phase 1: COLLECTION                                     │
│  ┌──────────────────┬───────────────────────────┐       │
│  │ StackExtractor    │ → StackInfo               │       │
│  │ TabExtractor      │ → TabInfo (TabItemInfo)   │       │
│  │ PaneExtractor     │ → PaneInfo (PaneItemInfo) │       │
│  │ ScreenExtractor   │ → ScreenInfo              │       │
│  │ ContainerExtractor│ → ContainerInfoModel      │       │
│  │ TransitionExtract.│ → TransitionInfo          │       │
│  │ DestinationExtract│ → DestinationInfo         │       │
│  └──────────────────┴───────────────────────────┘       │
│                                                          │
│  Phase 2: VALIDATION                                     │
│  ┌──────────────────────────────────────────────┐       │
│  │ ValidationEngine                              │       │
│  │  - Validates stacks, tabs, panes, screens    │       │
│  │  - Checks destinations and cross-references  │       │
│  └──────────────────────────────────────────────┘       │
│                                                          │
│  Phase 3: GENERATION                                     │
│  ┌─────────────────────┬────────────────────────┐       │
│  │ NavigationConfigGen.│ DeepLinkHandlerGen.     │       │
│  │  ├ ScreenBlockGen.  │  → {Prefix}DeepLink-   │       │
│  │  ├ ContainerBlockGen│    Handler.kt           │       │
│  │  ├ ScopeBlockGen.   │                        │       │
│  │  ├ TransitionBlock. │                        │       │
│  │  ├ WrapperBlockGen. │                        │       │
│  │  └ DeepLinkBlockGen.│                        │       │
│  │  → {Prefix}Naviga-  │                        │       │
│  │    tionConfig.kt     │                        │       │
│  └─────────────────────┴────────────────────────┘       │
└─────────────────────────────────────────────────────────┘
```

### Files in KSP Module

| Directory | File | Role |
|-----------|------|------|
| `ksp/` | `QuoVadisSymbolProcessor.kt` | Main processor, orchestrates 3 phases |
| `ksp/` | `QuoVadisClassNames.kt` | Type-safe KotlinPoet ClassName references |
| `ksp/models/` | `StackInfo.kt` | Stack metadata model |
| `ksp/models/` | `DestinationInfo.kt` | Destination metadata model |
| `ksp/models/` | `ScreenInfo.kt` | Screen function metadata model |
| `ksp/models/` | `TabInfo.kt` | Tab container + TabItemInfo + TabItemType |
| `ksp/models/` | `PaneInfo.kt` | Pane container + PaneItemInfo + PaneRole/AdaptStrategy/BackBehavior enums |
| `ksp/models/` | `TransitionInfo.kt` | Transition metadata model |
| `ksp/models/` | `ContainerInfoModel.kt` | Wrapper function metadata + ContainerType enum |
| `ksp/models/` | `ParamInfo.kt` | Constructor parameter metadata + SerializerType enum |
| `ksp/extractors/` | `StackExtractor.kt` | Extracts @Stack annotations |
| `ksp/extractors/` | `DestinationExtractor.kt` | Extracts @Destination annotations |
| `ksp/extractors/` | `ScreenExtractor.kt` | Extracts @Screen annotations |
| `ksp/extractors/` | `TabExtractor.kt` | Extracts @Tabs/@TabItem annotations |
| `ksp/extractors/` | `PaneExtractor.kt` | Extracts @Pane/@PaneItem annotations |
| `ksp/extractors/` | `TransitionExtractor.kt` | Extracts @Transition annotations |
| `ksp/extractors/` | `ContainerExtractor.kt` | Extracts @TabsContainer/@PaneContainer |
| `ksp/generators/dsl/` | `NavigationConfigGenerator.kt` | Main config file generator |
| `ksp/generators/dsl/` | `ScreenBlockGenerator.kt` | Screen registry generation |
| `ksp/generators/dsl/` | `ContainerBlockGenerator.kt` | Container DSL blocks |
| `ksp/generators/dsl/` | `ScopeBlockGenerator.kt` | Scope registration |
| `ksp/generators/dsl/` | `TransitionBlockGenerator.kt` | Transition registration |
| `ksp/generators/dsl/` | `WrapperBlockGenerator.kt` | Wrapper function dispatch |
| `ksp/generators/dsl/` | `DeepLinkBlockGenerator.kt` | Deep link DSL blocks |
| `ksp/generators/` | `DeepLinkHandlerGenerator.kt` | Standalone deep link handler generator |
| `ksp/generators/base/` | `DslCodeGenerator.kt` | Base class for generators |
| `ksp/generators/base/` | `StringTemplates.kt` | KDoc and comment templates |
| `ksp/validation/` | `ValidationEngine.kt` | Annotation validation |

---

## Open Questions for Compiler Plugin Migration

1. **Compose lambda constraints**: The current KSP uses when-dispatch instead of DSL lambdas for composable registries due to Compose compiler issues. Will the K2 compiler plugin have similar constraints, or can it generate code that the Compose compiler processes correctly?

2. **KotlinPoet dependency**: The current generator uses KotlinPoet extensively. The compiler plugin will need a different code generation approach — likely using IR (Intermediate Representation) directly.

3. **Multi-round processing**: The KSP processor handles this with a `hasGenerated` flag. The compiler plugin architecture handles phases differently (FIR → IR).

4. **Incremental compilation**: KSP tracks `originatingFiles` for incremental compilation. How will the compiler plugin handle incremental builds?

5. **Runtime DSL dependency**: The generated config uses `navigationConfig { }` DSL from `quo-vadis-core`. The compiler plugin could either:
   - Generate code that still uses this DSL (easier migration)
   - Generate IR directly that bypasses the DSL (more native but larger change)

6. **Validation**: The `ValidationEngine` runs KSP resolution-level checks. These would need to be implemented as FIR checkers in the compiler plugin.
