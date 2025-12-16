# DSL-Based Code Generation Refactoring Plan

> **Status**: Draft  
> **Created**: December 2024  
> **Author**: Architect Agent  

## Executive Summary

This document outlines a comprehensive refactoring plan to transform the current KSP-generated navigation code from verbose, imperative registries into an elegant DSL-based architecture. The new approach will provide:

- **One-liner integration** for simple use cases
- **Advanced configuration** for complex scenarios
- **Full backward compatibility** with existing navigation APIs
- **Type-safe, composable registry system**
- **Modular support** for multi-module projects

---

## Problem Statement

### Current Pain Points

1. **Verbosity**: Current generated code requires passing 5-6 separate registries to `NavigationHost`
2. **Scattered Configuration**: Registries are generated as separate singleton objects with no unified access
3. **Imperative Style**: Generated `build*NavNode()` functions use imperative construction instead of declarative DSL
4. **String-Based Keys**: Key construction like `"$key/tabKey/root"` is error-prone
5. **Limited Composability**: No easy way to combine registries from multiple modules
6. **Duplicate Code**: Common generation patterns repeated across 8 generators

### Current Integration Pattern (Problematic)

```kotlin
@Composable
fun App() {
    val navigator = remember {
        TreeNavigator(
            initialState = buildMainTabsNavNode(),
            scopeRegistry = GeneratedScopeRegistry,
            containerRegistry = GeneratedContainerRegistry,
            deepLinkHandler = GeneratedDeepLinkHandlerImpl
        )
    }
    
    NavigationHost(
        navigator = navigator,
        screenRegistry = GeneratedScreenRegistry,
        wrapperRegistry = GeneratedWrapperRegistry,
        scopeRegistry = GeneratedScopeRegistry,
        transitionRegistry = GeneratedTransitionRegistry,
        // ... more parameters
    )
}
```

---

## Proposed Solution

### Design Principles

1. **Hybrid DSL Model**: DSL for configuration, data objects for runtime lookups
2. **Full Registry Consolidation**: Single `GeneratedNavigationConfig` with modular composition
3. **`rememberNavigator()` Pattern**: Minimal boilerplate for common cases
4. **Backward Compatibility**: All existing APIs continue to work

### Target Integration Pattern

#### One-Liner (Simple Use Case)
```kotlin
@Composable
fun App() {
    QuoVadisNavigation(MainTabs::class)
}
```

#### Standard (Most Use Cases)
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class)
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig
    )
}
```

#### Advanced (Full Control)
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = MainTabs::class,
        config = GeneratedNavigationConfig,
        deepLinkHandler = CustomDeepLinkHandler
    )
    
    NavigationHost(
        navigator = navigator,
        config = GeneratedNavigationConfig + FeatureModuleConfig, // Composable!
        enablePredictiveBack = true,
        predictiveBackMode = PredictiveBackMode.FULL_CASCADE,
        windowSizeClass = currentWindowSizeClass()
    )
}
```

---

## Architecture Design

### 1. New Core Interfaces

#### NavigationConfig Interface

```kotlin
/**
 * Unified configuration for all navigation registries.
 * Provides both individual registry access and composition support.
 */
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val wrapperRegistry: WrapperRegistry
    val scopeRegistry: ScopeRegistry
    val transitionRegistry: TransitionRegistry
    val containerRegistry: ContainerRegistry
    val deepLinkHandler: DeepLinkHandler
    
    /**
     * Builds the initial NavNode for the given destination class.
     * @param destinationClass Root destination (container) class
     * @param key Optional custom key for the root node
     * @param parentKey Optional parent key for nested navigation
     */
    fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?
    
    /**
     * Combines this config with another, returning a composite config.
     * Later configs override earlier ones for conflicting entries.
     */
    operator fun plus(other: NavigationConfig): NavigationConfig
    
    companion object {
        val Empty: NavigationConfig = EmptyNavigationConfig
    }
}
```

#### NavigationConfigBuilder DSL

```kotlin
/**
 * DSL builder for constructing NavigationConfig instances.
 */
@NavigationConfigDsl
class NavigationConfigBuilder {
    // Screen registrations
    private val screens = mutableMapOf<KClass<out Destination>, ScreenEntry>()
    // Container builders
    private val containers = mutableMapOf<KClass<out Destination>, ContainerBuilder>()
    // Scope definitions
    private val scopes = mutableMapOf<String, MutableSet<KClass<out Destination>>>()
    // Transitions
    private val transitions = mutableMapOf<KClass<out Destination>, NavTransition>()
    // Wrappers
    private val tabWrappers = mutableMapOf<String, @Composable TabWrapperScope.() -> Unit>()
    private val paneWrappers = mutableMapOf<String, @Composable PaneWrapperScope.() -> Unit>()
    
    /**
     * Register a screen for a destination.
     */
    inline fun <reified D : Destination> screen(
        noinline content: @Composable ScreenScope.(D) -> Unit
    ) {
        screens[D::class] = ScreenEntry(D::class) { dest, scope ->
            content(scope, dest as D)
        }
    }
    
    /**
     * Register a stack container with DSL.
     */
    inline fun <reified D : Destination> stack(
        scopeKey: String? = null,
        noinline builder: StackBuilder.() -> Unit
    ) {
        containers[D::class] = ContainerBuilder.Stack(
            destinationClass = D::class,
            scopeKey = scopeKey ?: D::class.simpleName,
            builder = builder
        )
    }
    
    /**
     * Register a tab container with DSL.
     */
    inline fun <reified D : Destination> tabs(
        scopeKey: String? = null,
        wrapperKey: String? = null,
        noinline builder: TabsBuilder.() -> Unit
    ) {
        containers[D::class] = ContainerBuilder.Tabs(
            destinationClass = D::class,
            scopeKey = scopeKey ?: D::class.simpleName,
            wrapperKey = wrapperKey,
            builder = builder
        )
    }
    
    /**
     * Register a pane container with DSL.
     */
    inline fun <reified D : Destination> panes(
        scopeKey: String? = null,
        wrapperKey: String? = null,
        noinline builder: PanesBuilder.() -> Unit
    ) {
        containers[D::class] = ContainerBuilder.Panes(
            destinationClass = D::class,
            scopeKey = scopeKey ?: D::class.simpleName,
            wrapperKey = wrapperKey,
            builder = builder
        )
    }
    
    /**
     * Define scope membership.
     */
    fun scope(key: String, vararg destinations: KClass<out Destination>) {
        scopes.getOrPut(key) { mutableSetOf() }.addAll(destinations)
    }
    
    /**
     * Register transition for a destination.
     */
    inline fun <reified D : Destination> transition(transition: NavTransition) {
        transitions[D::class] = transition
    }
    
    /**
     * Register tab wrapper.
     */
    fun tabWrapper(key: String, wrapper: @Composable TabWrapperScope.() -> Unit) {
        tabWrappers[key] = wrapper
    }
    
    /**
     * Register pane wrapper.
     */
    fun paneWrapper(key: String, wrapper: @Composable PaneWrapperScope.() -> Unit) {
        paneWrappers[key] = wrapper
    }
    
    fun build(): NavigationConfig = DslNavigationConfig(
        screens = screens.toMap(),
        containers = containers.toMap(),
        scopes = scopes.mapValues { it.value.toSet() },
        transitions = transitions.toMap(),
        tabWrappers = tabWrappers.toMap(),
        paneWrappers = paneWrappers.toMap()
    )
}

/**
 * Top-level DSL function for building NavigationConfig.
 */
fun navigationConfig(builder: NavigationConfigBuilder.() -> Unit): NavigationConfig {
    return NavigationConfigBuilder().apply(builder).build()
}
```

### 2. Container Builder DSL

```kotlin
/**
 * DSL for building stack containers.
 */
@NavigationConfigDsl
class StackBuilder {
    internal val screens = mutableListOf<StackScreenEntry>()
    
    /**
     * Add a screen to the stack.
     */
    inline fun <reified D : Destination> screen(
        destination: D,
        key: String = destination::class.simpleName ?: "screen"
    ) {
        screens.add(StackScreenEntry(destination, key))
    }
}

/**
 * DSL for building tab containers.
 */
@NavigationConfigDsl  
class TabsBuilder {
    internal val tabs = mutableListOf<TabEntry>()
    var initialTab: Int = 0
    
    /**
     * Add a flat screen tab (no nested stack).
     */
    inline fun <reified D : Destination> tab(
        destination: D,
        title: String? = null,
        icon: Any? = null
    ) {
        tabs.add(TabEntry.FlatScreen(
            destination = destination,
            title = title,
            icon = icon
        ))
    }
    
    /**
     * Add a tab with nested stack navigation.
     */
    inline fun <reified D : Destination> nestedTab(
        destination: D,
        title: String? = null,
        icon: Any? = null,
        noinline builder: StackBuilder.() -> Unit
    ) {
        tabs.add(TabEntry.NestedStack(
            destination = destination,
            title = title,
            icon = icon,
            stackBuilder = builder
        ))
    }
    
    /**
     * Add a tab that references another container definition.
     */
    inline fun <reified D : Destination> containerTab(
        containerClass: KClass<out Destination>,
        title: String? = null,
        icon: Any? = null
    ) {
        tabs.add(TabEntry.ContainerReference(
            containerClass = containerClass,
            title = title,
            icon = icon
        ))
    }
}

/**
 * DSL for building pane containers.
 */
@NavigationConfigDsl
class PanesBuilder {
    internal val panes = mutableMapOf<PaneRole, PaneEntry>()
    var initialPane: PaneRole = PaneRole.Primary
    var backBehavior: PaneBackBehavior = PaneBackBehavior.PopOrSwitchPane
    
    /**
     * Configure primary pane.
     */
    fun primary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        panes[PaneRole.Primary] = PaneEntry(
            role = PaneRole.Primary,
            weight = weight,
            minWidth = minWidth,
            contentBuilder = builder
        )
    }
    
    /**
     * Configure secondary pane.
     */
    fun secondary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        panes[PaneRole.Secondary] = PaneEntry(
            role = PaneRole.Secondary,
            weight = weight,
            minWidth = minWidth,
            contentBuilder = builder
        )
    }
    
    /**
     * Configure tertiary pane.
     */
    fun tertiary(
        weight: Float = 1f,
        minWidth: Dp = 0.dp,
        builder: PaneContentBuilder.() -> Unit
    ) {
        panes[PaneRole.Tertiary] = PaneEntry(
            role = PaneRole.Tertiary,
            weight = weight,
            minWidth = minWidth,
            contentBuilder = builder
        )
    }
}

@NavigationConfigDsl
class PaneContentBuilder {
    internal var rootDestination: Destination? = null
    internal var alwaysVisible: Boolean = false
    
    /**
     * Set the root destination for this pane.
     */
    fun root(destination: Destination) {
        rootDestination = destination
    }
    
    /**
     * Mark this pane as always visible (doesn't collapse on narrow screens).
     */
    fun alwaysVisible() {
        alwaysVisible = true
    }
}
```

### 3. Generated Code Structure

The KSP processor will generate a single `GeneratedNavigationConfig.kt` file:

```kotlin
// Generated by Quo Vadis KSP processor
package com.example.app.navigation.generated

import com.jermey.quo.vadis.core.navigation.*
import com.example.app.destinations.*

/**
 * Auto-generated navigation configuration.
 * 
 * Usage:
 * ```
 * // One-liner
 * QuoVadisNavigation(MainTabs::class)
 * 
 * // Standard
 * val navigator = rememberQuoVadisNavigator(MainTabs::class)
 * NavigationHost(navigator, config = GeneratedNavigationConfig)
 * 
 * // Advanced
 * NavigationHost(
 *     navigator = navigator,
 *     config = GeneratedNavigationConfig + FeatureModuleConfig
 * )
 * ```
 */
object GeneratedNavigationConfig : NavigationConfig {
    
    // ===========================================
    // DSL-STYLE CONFIGURATION
    // ===========================================
    
    private val config = navigationConfig {
        // ─────────────────────────────────────
        // SCREEN REGISTRATIONS
        // ─────────────────────────────────────
        
        screen<HomeDestination.Feed> { dest ->
            FeedScreen(navigator = navigator)
        }
        
        screen<HomeDestination.Detail> { dest ->
            DetailScreen(destination = dest, navigator = navigator)
        }
        
        screen<ProfileDestination> { dest ->
            ProfileScreen(userId = dest.userId, navigator = navigator)
        }
        
        // ─────────────────────────────────────
        // CONTAINER DEFINITIONS
        // ─────────────────────────────────────
        
        tabs<MainTabs>(scopeKey = "MainTabs", wrapperKey = "mainTabsWrapper") {
            initialTab = 0
            
            // Flat screen tab
            tab(
                destination = MainTabs.HomeTab,
                title = "Home",
                icon = Icons.Home
            )
            
            // Nested stack tab
            nestedTab(
                destination = MainTabs.ExploreTab,
                title = "Explore",
                icon = Icons.Explore
            ) {
                screen(ExploreDestination.List)
            }
            
            // Container reference tab
            containerTab<MainTabs.ProfileTab>(
                containerClass = ProfileStack::class,
                title = "Profile",
                icon = Icons.Person
            )
        }
        
        stack<ProfileStack>(scopeKey = "ProfileStack") {
            screen(ProfileDestination.Main)
        }
        
        panes<MasterDetailDestination>(
            scopeKey = "MasterDetail",
            wrapperKey = "masterDetailWrapper"
        ) {
            initialPane = PaneRole.Primary
            backBehavior = PaneBackBehavior.PopOrSwitchPane
            
            primary(weight = 0.4f, minWidth = 240.dp) {
                root(MasterDetailDestination.List)
            }
            
            secondary(weight = 0.6f, minWidth = 320.dp) {
                root(MasterDetailDestination.Detail)
            }
        }
        
        // ─────────────────────────────────────
        // SCOPE DEFINITIONS
        // ─────────────────────────────────────
        
        scope("MainTabs",
            MainTabs.HomeTab::class,
            MainTabs.ExploreTab::class,
            MainTabs.ProfileTab::class
        )
        
        scope("ProfileStack",
            ProfileDestination.Main::class,
            ProfileDestination.Settings::class
        )
        
        // ─────────────────────────────────────
        // TRANSITIONS
        // ─────────────────────────────────────
        
        transition<HomeDestination.Detail>(NavTransitions.SharedElement)
        transition<ProfileDestination>(NavTransitions.Slide)
        
        // ─────────────────────────────────────
        // WRAPPERS
        // ─────────────────────────────────────
        
        tabWrapper("mainTabsWrapper") {
            CustomTabBar(
                tabs = tabs,
                selectedIndex = activeTabIndex,
                onTabSelected = { switchToTab(it) }
            ) {
                content()
            }
        }
        
        paneWrapper("masterDetailWrapper") {
            CustomPaneLayout(
                primaryContent = { PrimaryPaneContent() },
                secondaryContent = { SecondaryPaneContent() }
            )
        }
    }
    
    // ===========================================
    // NAVIGATION CONFIG IMPLEMENTATION
    // ===========================================
    
    override val screenRegistry: ScreenRegistry = config.screenRegistry
    override val wrapperRegistry: WrapperRegistry = config.wrapperRegistry
    override val scopeRegistry: ScopeRegistry = config.scopeRegistry
    override val transitionRegistry: TransitionRegistry = config.transitionRegistry
    override val containerRegistry: ContainerRegistry = config.containerRegistry
    override val deepLinkHandler: DeepLinkHandler = config.deepLinkHandler
    
    override fun buildNavNode(
        destinationClass: KClass<out Destination>,
        key: String?,
        parentKey: String?
    ): NavNode? = config.buildNavNode(destinationClass, key, parentKey)
    
    override fun plus(other: NavigationConfig): NavigationConfig =
        CompositeNavigationConfig(this, other)
    
    // ===========================================
    // CONVENIENCE EXTENSIONS
    // ===========================================
    
    /**
     * Root destinations available for navigation.
     */
    val roots: Set<KClass<out Destination>> = setOf(
        MainTabs::class,
        ProfileStack::class,
        MasterDetailDestination::class
    )
}
```

### 4. Convenience Composables

```kotlin
/**
 * Remember and create a Navigator with generated configuration.
 */
@Composable
fun rememberQuoVadisNavigator(
    rootDestination: KClass<out Destination>,
    config: NavigationConfig = GeneratedNavigationConfig,
    key: String? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    
    return remember(rootDestination, config, key) {
        val initialState = config.buildNavNode(
            destinationClass = rootDestination,
            key = key,
            parentKey = null
        ) ?: error("No container registered for $rootDestination")
        
        TreeNavigator(
            initialState = initialState,
            scopeRegistry = config.scopeRegistry,
            containerRegistry = config.containerRegistry,
            deepLinkHandler = config.deepLinkHandler,
            coroutineScope = coroutineScope
        )
    }
}

/**
 * One-liner navigation setup with all defaults.
 */
@Composable
fun QuoVadisNavigation(
    rootDestination: KClass<out Destination>,
    modifier: Modifier = Modifier,
    config: NavigationConfig = GeneratedNavigationConfig,
    enablePredictiveBack: Boolean = true,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    windowSizeClass: WindowSizeClass? = null
) {
    val navigator = rememberQuoVadisNavigator(rootDestination, config)
    
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        screenRegistry = config.screenRegistry,
        wrapperRegistry = config.wrapperRegistry,
        transitionRegistry = config.transitionRegistry,
        scopeRegistry = config.scopeRegistry,
        enablePredictiveBack = enablePredictiveBack,
        predictiveBackMode = predictiveBackMode,
        windowSizeClass = windowSizeClass
    )
}

/**
 * NavigationHost overload accepting NavigationConfig.
 */
@Composable
fun NavigationHost(
    navigator: Navigator,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    predictiveBackMode: PredictiveBackMode = PredictiveBackMode.ROOT_ONLY,
    windowSizeClass: WindowSizeClass? = null
) {
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        screenRegistry = config.screenRegistry,
        wrapperRegistry = config.wrapperRegistry,
        transitionRegistry = config.transitionRegistry,
        scopeRegistry = config.scopeRegistry,
        enablePredictiveBack = enablePredictiveBack,
        predictiveBackMode = predictiveBackMode,
        windowSizeClass = windowSizeClass
    )
}
```

---

## Implementation Plan

### Phase 1: Core DSL Infrastructure (Week 1-2)

#### Task 1.1: Create NavigationConfig Interface
**Location**: `quo-vadis-core/src/commonMain/.../navigation/`

| File | Description |
|------|-------------|
| `NavigationConfig.kt` | Interface definition |
| `EmptyNavigationConfig.kt` | Default empty implementation |
| `CompositeNavigationConfig.kt` | Composition support |

**Acceptance Criteria**:
- [ ] `NavigationConfig` interface with all registry properties
- [ ] `plus` operator for composition
- [ ] `buildNavNode()` method for initial state creation
- [ ] Unit tests for composition logic

#### Task 1.2: Create DSL Builder Infrastructure
**Location**: `quo-vadis-core/src/commonMain/.../navigation/dsl/`

| File | Description |
|------|-------------|
| `NavigationConfigBuilder.kt` | Main DSL builder |
| `StackBuilder.kt` | Stack container DSL |
| `TabsBuilder.kt` | Tabs container DSL |
| `PanesBuilder.kt` | Panes container DSL |
| `NavigationConfigDsl.kt` | DSL marker annotation |

**Acceptance Criteria**:
- [ ] All builder classes implemented with type-safe DSL
- [ ] DSL marker annotation to prevent scope leakage
- [ ] Proper data classes for intermediate structures
- [ ] Unit tests for all builder configurations

#### Task 1.3: Create DslNavigationConfig Implementation
**Location**: `quo-vadis-core/src/commonMain/.../navigation/`

| File | Description |
|------|-------------|
| `DslNavigationConfig.kt` | Runtime implementation |
| `DslScreenRegistry.kt` | DSL-based screen registry |
| `DslContainerRegistry.kt` | DSL-based container registry |
| `DslScopeRegistry.kt` | DSL-based scope registry |

**Acceptance Criteria**:
- [ ] Converts DSL configuration to runtime registries
- [ ] Implements `buildNavNode()` from container definitions
- [ ] All registry interfaces properly implemented
- [ ] Integration tests with existing navigation APIs

### Phase 2: Convenience Composables (Week 2)

#### Task 2.1: Create rememberQuoVadisNavigator
**Location**: `quo-vadis-core/src/commonMain/.../compose/`

| File | Description |
|------|-------------|
| `QuoVadisComposables.kt` | Convenience composables |

**Acceptance Criteria**:
- [ ] `rememberQuoVadisNavigator()` properly memoized
- [ ] Handles coroutine scope correctly
- [ ] Integrates with NavigationConfig
- [ ] Unit tests for navigator creation

#### Task 2.2: Create QuoVadisNavigation One-Liner
**Location**: `quo-vadis-core/src/commonMain/.../compose/`

**Acceptance Criteria**:
- [ ] `QuoVadisNavigation()` combines navigator + host
- [ ] All NavigationHost parameters forwarded
- [ ] Platform-specific defaults handled
- [ ] Demo app updated with one-liner usage

#### Task 2.3: Add NavigationHost Config Overload
**Location**: `quo-vadis-core/src/commonMain/.../compose/NavigationHost.kt`

**Acceptance Criteria**:
- [ ] New overload accepting `NavigationConfig`
- [ ] Backward compatible with existing signatures
- [ ] KDoc documentation added
- [ ] Existing usages continue to work

### Phase 3: KSP Generator Refactoring (Week 3-4)

#### Task 3.1: Create New Generator Base Classes
**Location**: `quo-vadis-ksp/src/main/.../generator/`

| File | Description |
|------|-------------|
| `DslCodeGenerator.kt` | Base class for DSL generation |
| `CodeBlockBuilders.kt` | Shared code block utilities |
| `StringTemplates.kt` | Reusable string templates |

**Acceptance Criteria**:
- [ ] Eliminate duplicate code across generators
- [ ] Type-safe code generation helpers
- [ ] Unit tests for code block generation

#### Task 3.2: Create NavigationConfigGenerator
**Location**: `quo-vadis-ksp/src/main/.../generator/`

| File | Description |
|------|-------------|
| `NavigationConfigGenerator.kt` | Main DSL config generator |
| `ScreenBlockGenerator.kt` | Screen registration blocks |
| `ContainerBlockGenerator.kt` | Container DSL blocks |
| `ScopeBlockGenerator.kt` | Scope definition blocks |

**Acceptance Criteria**:
- [ ] Generates single `GeneratedNavigationConfig.kt`
- [ ] All current annotation types supported
- [ ] Generated code compiles and runs
- [ ] Output matches DSL structure defined above

#### Task 3.3: Refactor Existing Generators
**Location**: `quo-vadis-ksp/src/main/.../generator/`

| Generator | Action |
|-----------|--------|
| `ScreenRegistryGenerator` | Convert to generate DSL blocks |
| `ContainerRegistryGenerator` | Convert to generate DSL blocks |
| `ScopeRegistryGenerator` | Convert to generate DSL blocks |
| `NavNodeBuilderGenerator` | Deprecate, keep for compatibility |
| `WrapperRegistryGenerator` | Convert to generate DSL blocks |
| `TransitionRegistryGenerator` | Convert to generate DSL blocks |
| `DeepLinkHandlerGenerator` | Integrate into config generator |

**Acceptance Criteria**:
- [ ] All generators produce DSL-style code
- [ ] Single unified output file
- [ ] Backward compatible with annotation syntax
- [ ] All existing tests pass

#### Task 3.4: Update KSP Processor Orchestration
**Location**: `quo-vadis-ksp/src/main/.../QuoVadisSymbolProcessor.kt`

**Acceptance Criteria**:
- [ ] Single file generation instead of multiple
- [ ] Proper dependency ordering
- [ ] Incremental processing maintained
- [ ] Error messages improved

### Phase 4: Migration & Deprecation (Week 4-5)

#### Task 4.1: Add Deprecation Warnings
**Location**: Various

| Item | Action |
|------|--------|
| Old `build*NavNode()` functions | Add `@Deprecated` with migration path |
| Individual registry objects | Add `@Deprecated` pointing to unified config |
| Direct registry parameters | Document preference for config parameter |

**Acceptance Criteria**:
- [ ] All old APIs deprecated with clear messages
- [ ] Migration guide in deprecation messages
- [ ] No breaking changes (all old code still works)

#### Task 4.2: Update Demo Application
**Location**: `composeApp/src/commonMain/`

**Acceptance Criteria**:
- [ ] Demo uses new one-liner where appropriate
- [ ] Advanced configuration demonstrated
- [ ] Multi-module composition demonstrated
- [ ] All demo features continue working

#### Task 4.3: Create Migration Guide
**Location**: `docs/`

| File | Description |
|------|-------------|
| `MIGRATION_DSL.md` | Step-by-step migration guide |

**Acceptance Criteria**:
- [ ] Before/after code examples
- [ ] Common migration scenarios covered
- [ ] Troubleshooting section
- [ ] Performance considerations documented

### Phase 5: Documentation & Testing (Week 5)

#### Task 5.1: Update API Documentation
**Location**: `quo-vadis-core/docs/`, `docs/site/`

**Acceptance Criteria**:
- [ ] KDoc for all new public APIs
- [ ] Website documentation updated
- [ ] Code samples updated
- [ ] API reference reflects new patterns

#### Task 5.2: Comprehensive Testing
**Location**: `quo-vadis-core/src/commonTest/`, `quo-vadis-ksp/src/test/`

| Test Category | Coverage |
|---------------|----------|
| DSL Builder Unit Tests | All builder permutations |
| NavigationConfig Unit Tests | Composition, lookup, building |
| KSP Output Tests | Generated code validation |
| Integration Tests | End-to-end navigation flows |
| Migration Tests | Old patterns still work |

**Acceptance Criteria**:
- [ ] 90%+ code coverage on new code
- [ ] All edge cases tested
- [ ] Performance regression tests
- [ ] Multi-module scenario tests

---

## Generated Code Examples

### Before (Current)

```kotlin
// 6+ generated files

// GeneratedScreenRegistry.kt
object GeneratedScreenRegistry : ScreenRegistry {
    @Composable
    override fun Content(destination: Destination, navigator: Navigator, ...) {
        when (destination) {
            is HomeDestination.Feed -> FeedScreen(navigator)
            is HomeDestination.Detail -> DetailScreen(destination, navigator)
            // ... 50+ cases
            else -> error("No screen registered")
        }
    }
}

// GeneratedScopeRegistry.kt
object GeneratedScopeRegistry : ScopeRegistry {
    private val scopeMap = mapOf(
        "MainTabs" to setOf(MainTabs.HomeTab::class, MainTabs.ExploreTab::class, ...),
        // ... more scopes
    )
    // implementation
}

// GeneratedContainerRegistry.kt
object GeneratedContainerRegistry : ContainerRegistry {
    override fun getContainerInfo(destination: Destination): ContainerInfo? {
        return when (destination) {
            is MainTabs.HomeTab -> ContainerInfo.TabContainer(::buildMainTabsNavNode, 0, "MainTabs")
            // ... more cases
            else -> null
        }
    }
}

// MainTabsNavNodeBuilder.kt
fun buildMainTabsNavNode(
    key: String = "MainTabs",
    parentKey: String? = null,
    initialTabIndex: Int = 0
): TabNode {
    return TabNode(
        key = key,
        parentKey = parentKey,
        stacks = listOf(
            StackNode(
                key = "$key/home",
                parentKey = key,
                children = listOf(
                    ScreenNode(
                        key = "$key/home/root",
                        parentKey = "$key/home",
                        destination = MainTabs.HomeTab
                    )
                )
            ),
            // ... more tabs
        ),
        activeStackIndex = initialTabIndex
    )
}

// ... more generated files
```

### After (New DSL)

```kotlin
// Single generated file: GeneratedNavigationConfig.kt

object GeneratedNavigationConfig : NavigationConfig {
    
    private val config = navigationConfig {
        // ═══════════════════════════════════════════════
        // SCREENS
        // ═══════════════════════════════════════════════
        
        screen<HomeDestination.Feed> { FeedScreen(navigator) }
        screen<HomeDestination.Detail> { DetailScreen(destination = it, navigator) }
        screen<ProfileDestination> { ProfileScreen(userId = it.userId, navigator) }
        // ... more screens
        
        // ═══════════════════════════════════════════════
        // CONTAINERS
        // ═══════════════════════════════════════════════
        
        tabs<MainTabs>(scopeKey = "MainTabs") {
            initialTab = 0
            tab(MainTabs.HomeTab, title = "Home", icon = Icons.Home)
            nestedTab(MainTabs.ExploreTab, title = "Explore", icon = Icons.Explore) {
                screen(ExploreDestination.List)
            }
            containerTab<MainTabs.ProfileTab>(ProfileStack::class, "Profile", Icons.Person)
        }
        
        stack<ProfileStack>(scopeKey = "ProfileStack") {
            screen(ProfileDestination.Main)
        }
        
        panes<MasterDetailDestination>(scopeKey = "MasterDetail") {
            primary(weight = 0.4f) { root(MasterDetailDestination.List) }
            secondary(weight = 0.6f) { root(MasterDetailDestination.Detail) }
        }
        
        // ═══════════════════════════════════════════════
        // SCOPES (auto-inferred from containers, explicit override)
        // ═══════════════════════════════════════════════
        
        scope("MainTabs", MainTabs.HomeTab::class, MainTabs.ExploreTab::class, MainTabs.ProfileTab::class)
        
        // ═══════════════════════════════════════════════
        // TRANSITIONS
        // ═══════════════════════════════════════════════
        
        transition<HomeDestination.Detail>(NavTransitions.SharedElement)
        
        // ═══════════════════════════════════════════════
        // WRAPPERS
        // ═══════════════════════════════════════════════
        
        tabWrapper("mainTabsWrapper") {
            CustomTabBar(tabs, activeTabIndex, ::switchToTab) { content() }
        }
    }
    
    // Delegate implementation
    override val screenRegistry = config.screenRegistry
    override val wrapperRegistry = config.wrapperRegistry
    override val scopeRegistry = config.scopeRegistry
    override val transitionRegistry = config.transitionRegistry
    override val containerRegistry = config.containerRegistry
    override val deepLinkHandler = config.deepLinkHandler
    
    override fun buildNavNode(destinationClass: KClass<out Destination>, key: String?, parentKey: String?) = 
        config.buildNavNode(destinationClass, key, parentKey)
    
    override fun plus(other: NavigationConfig) = CompositeNavigationConfig(this, other)
}
```

---

## Multi-Module Support

### Module A: Feature Module

```kotlin
// feature-a/generated/FeatureANavigationConfig.kt
object FeatureANavigationConfig : NavigationConfig {
    private val config = navigationConfig {
        screen<FeatureADestination.List> { FeatureAListScreen(navigator) }
        screen<FeatureADestination.Detail> { FeatureADetailScreen(it, navigator) }
        
        stack<FeatureADestination>(scopeKey = "FeatureA") {
            screen(FeatureADestination.List)
        }
    }
    // ... implementation
}
```

### Module B: Feature Module

```kotlin
// feature-b/generated/FeatureBNavigationConfig.kt
object FeatureBNavigationConfig : NavigationConfig {
    private val config = navigationConfig {
        screen<FeatureBDestination.Main> { FeatureBMainScreen(navigator) }
        
        stack<FeatureBDestination>(scopeKey = "FeatureB") {
            screen(FeatureBDestination.Main)
        }
    }
    // ... implementation
}
```

### App Module: Composition

```kotlin
// app/AppNavigation.kt
@Composable
fun App() {
    val combinedConfig = GeneratedNavigationConfig + 
                         FeatureANavigationConfig + 
                         FeatureBNavigationConfig
    
    QuoVadisNavigation(
        rootDestination = MainTabs::class,
        config = combinedConfig
    )
}
```

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Breaking existing users | Low | High | Full backward compatibility, deprecation path |
| Performance regression | Medium | Medium | Benchmark before/after, lazy initialization |
| DSL complexity | Low | Low | Clear documentation, IDE support |
| Multi-module conflicts | Medium | Medium | Clear composition rules, conflict detection |
| Code generation bugs | Medium | High | Extensive testing, gradual rollout |

---

## Success Metrics

1. **Developer Experience**
   - Reduction from 6+ files to 1 generated file
   - One-liner setup possible for 80% of use cases
   - IDE autocomplete works throughout DSL

2. **Code Quality**
   - 50%+ reduction in generated code volume
   - Zero duplication in generators
   - 100% type safety maintained

3. **Backward Compatibility**
   - All existing annotation syntax works
   - All existing APIs functional
   - Clear migration path documented

4. **Performance**
   - No runtime overhead vs current approach
   - Build time impact < 5%

---

## Timeline Summary

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Phase 1: Core DSL Infrastructure | 2 weeks | NavigationConfig interface + DSL builders |
| Phase 2: Convenience Composables | 1 week | rememberQuoVadisNavigator + QuoVadisNavigation |
| Phase 3: KSP Generator Refactoring | 2 weeks | Single unified generator |
| Phase 4: Migration & Deprecation | 1 week | Deprecation warnings + migration guide |
| Phase 5: Documentation & Testing | 1 week | Complete documentation + tests |

**Total: 5-7 weeks**

---

## Open Questions

1. **Deep Link Integration**: Should the DSL also support declarative deep link definitions?
   ```kotlin
   screen<ProfileDestination> {
       deepLink("profile/{userId}")
       // ...
   }
   ```

2. **Animation DSL**: Should transitions be part of the container definition?
   ```kotlin
   tabs<MainTabs> {
       transition = NavTransitions.Fade
       tab(HomeTab) { ... }
   }
   ```

3. **Validation**: Should generated code include runtime validation for common mistakes?

4. **IDE Plugin**: Worth creating an IDE plugin for DSL visualization/navigation?

---

## Appendix A: Full DSL Grammar

```
navigationConfig := 'navigationConfig' '{' configBlock* '}'

configBlock := screenBlock | containerBlock | scopeBlock | transitionBlock | wrapperBlock

screenBlock := 'screen' '<' DestinationType '>' '{' composableContent '}'

containerBlock := tabsBlock | stackBlock | panesBlock

tabsBlock := 'tabs' '<' DestinationType '>' '(' containerParams ')' '{' tabsContent '}'
tabsContent := ('initialTab' '=' INT)? tabEntry*
tabEntry := 'tab' '(' tabParams ')' | 'nestedTab' '(' tabParams ')' '{' stackContent '}' | 'containerTab' '<' DestinationType '>' '(' containerClass ',' tabParams ')'

stackBlock := 'stack' '<' DestinationType '>' '(' containerParams ')' '{' stackContent '}'
stackContent := screenEntry*
screenEntry := 'screen' '(' Destination (',' key)? ')'

panesBlock := 'panes' '<' DestinationType '>' '(' containerParams ')' '{' panesContent '}'
panesContent := ('initialPane' '=' PaneRole)? ('backBehavior' '=' PaneBackBehavior)? paneEntry*
paneEntry := ('primary' | 'secondary' | 'tertiary') '(' paneParams ')' '{' paneContent '}'
paneContent := 'root' '(' Destination ')' | 'alwaysVisible' '(' ')'

scopeBlock := 'scope' '(' STRING ',' KClass* ')'

transitionBlock := 'transition' '<' DestinationType '>' '(' NavTransition ')'

wrapperBlock := 'tabWrapper' '(' STRING ')' '{' composableContent '}' | 'paneWrapper' '(' STRING ')' '{' composableContent '}'

containerParams := ('scopeKey' '=' STRING)? (',' 'wrapperKey' '=' STRING)?
tabParams := 'destination' '=' Destination (',' 'title' '=' STRING)? (',' 'icon' '=' Any)?
paneParams := ('weight' '=' FLOAT)? (',' 'minWidth' '=' Dp)?
```

---

## Appendix B: Compatibility Matrix

| Feature | Old API | New API | Compatibility |
|---------|---------|---------|---------------|
| Screen Registration | `@Screen` annotation | DSL `screen<T>` block | ✅ Both work |
| Tab Container | `@Tabs` + `@TabItem` | DSL `tabs<T>` block | ✅ Both work |
| Pane Container | `@Pane` + `@PaneItem` | DSL `panes<T>` block | ✅ Both work |
| Stack Container | `@Stack` annotation | DSL `stack<T>` block | ✅ Both work |
| Scope Definition | Annotation inference | DSL `scope()` + auto | ✅ Both work |
| Transitions | `@Transition` annotation | DSL `transition<T>` | ✅ Both work |
| Wrappers | `@TabWrapper`/`@PaneWrapper` | DSL wrapper blocks | ✅ Both work |
| Deep Links | `@Destination(route=)` | DSL extension (future) | ✅ Both work |
| NavigationHost | Multiple params | Single config param | ✅ Both work |
| Navigator Creation | Manual | `rememberQuoVadisNavigator` | ✅ Both work |
