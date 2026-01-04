# Quo Vadis Navigation Architecture

This document describes the architectural design of the Quo Vadis navigation library, covering the logic layer, rendering layer, and the navigation node types.

## Table of Contents

- [Overview](#overview)
- [Architecture Diagram](#architecture-diagram)
- [Package Structure](#package-structure)
- [Logic Layer](#logic-layer)
  - [Navigator Interface](#navigator-interface)
  - [TreeNavigator Implementation](#treenavigator-implementation)
  - [TreeMutator](#treemutator)
- [Rendering Layer](#rendering-layer)
  - [NavigationHost](#navigationhost)
  - [NavNodeRenderer](#navnoderenderer)
  - [Specialized Renderers](#specialized-renderers)
- [NavNode Types](#navnode-types)
  - [NavNode (Base Interface)](#navnode-base-interface)
  - [ScreenNode](#screennode)
  - [StackNode](#stacknode)
  - [TabNode](#tabnode)
  - [PaneNode](#panenode)
- [Registry System](#registry-system)
  - [ScreenRegistry](#screenregistry)
  - [ContainerRegistry](#containerregistry)
  - [TransitionRegistry](#transitionregistry)
  - [ScopeRegistry](#scoperegistry)
  - [DeepLinkRegistry](#deeplinkregistry)
  - [Composite Registries](#composite-registries)
- [DSL Configuration](#dsl-configuration)
  - [NavigationConfigBuilder](#navigationconfigbuilder)
  - [Screen Configuration](#screen-configuration)
  - [Container Configuration](#container-configuration)
  - [Transition Configuration](#transition-configuration)
  - [Scope Configuration](#scope-configuration)
  - [Configuration Usage](#configuration-usage)
  - [DSL Benefits](#dsl-benefits)
- [Life Cycle Management](#life-cycle-management)
- [FlowMVI Integration](#flowmvi-integration)
- [Layer Interaction](#layer-interaction)
- [Summary](#summary)

---

## Overview

Quo Vadis uses a **tree-based navigation architecture** where:

1. **Logic Layer** manages navigation state as an immutable tree of `NavNode` objects
2. **Rendering Layer** recursively renders the tree to Compose UI with animations  
3. **Registry System** provides extensibility for screens, containers, and transitions
4. **DSL Configuration** offers a clean API for setting up navigation

This separation ensures:
- **Clean state management**: All navigation logic is isolated in the logic layer
- **Flexible rendering**: UI adapts to different screen sizes and platforms
- **Testability**: Logic can be unit tested without UI dependencies
- **Extensibility**: Registry system allows custom screens, containers, and transitions
- **Ease of use**: DSL provides a declarative configuration API

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Application                                      │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                           RENDERING LAYER                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        NavigationHost                                    │ │
│  │  - Entry point for navigation UI                                        │ │
│  │  - Sets up SharedTransitionLayout                                       │ │
│  │  - Handles predictive back gestures                                     │ │
│  │  - Provides CompositionLocals (NavRenderScope)                          │ │
│  └───────────────────────────────────┬─────────────────────────────────────┘ │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        NavNodeRenderer                                   │ │
│  │  - Recursive dispatcher                                                  │ │
│  │  - Routes to specialized renderers based on node type                   │ │
│  └───────────────────────────────────┬─────────────────────────────────────┘ │
│                                      │                                        │
│            ┌─────────────────────────┼─────────────────────────┐              │
│            ▼                         ▼                         ▼              │
│  ┌─────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐     │
│  │  ScreenRenderer │   │    StackRenderer    │   │    TabRenderer      │     │
│  │  - Leaf content │   │  - Animated stack   │   │  - Tab wrapper      │     │
│  │  - Registry     │   │    transitions      │   │  - Tab switching    │     │
│  │    lookup       │   │  - Predictive back  │   │    animations       │     │
│  └─────────────────┘   └─────────────────────┘   └─────────────────────┘     │
│                                                                               │
│            ┌─────────────────────────┐                                        │
│            ▼                                                                  │
│  ┌─────────────────────┐                                                     │
│  │    PaneRenderer     │                                                     │
│  │  - Adaptive layout  │                                                     │
│  │  - Multi-pane /     │                                                     │
│  │    single-pane      │                                                     │
│  └─────────────────────┘                                                     │
└───────────────────────────────────┬─────────────────────────────────────────┘
                                    │
                                    │ observes StateFlow<NavNode>
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                             LOGIC LAYER                                       │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          Navigator                                       │ │
│  │  (Interface)                                                             │ │
│  │  - state: StateFlow<NavNode>                                            │ │
│  │  - navigate(), navigateBack()                                           │ │
│  │  - transitionState, currentDestination                                  │ │
│  │  └───────────────────────────────────┬─────────────────────────────────────┘ │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        TreeNavigator                                     │ │
│  │  (Implementation)                                                        │ │
│  │  - MutableStateFlow<NavNode> for state management                       │ │
│  │  - Container-aware navigation                                           │ │
│  │  - Scope-aware navigation                                               │ │
│  │  - Lifecycle event dispatch                                             │ │
│  │  └───────────────────────────────────┬─────────────────────────────────────┘ │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          TreeMutator                                     │ │
│  │  (Static Operations)                                                     │ │
│  │  - push(), pop(), popTo()                                               │ │
│  │  - switchTab(), switchActiveTab()                                       │ │
│  │  - navigateToPane(), switchActivePane()                                 │ │
│  │  - replaceNode(), removeNode()                                          │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            NAV NODE TREE                                      │
│                                                                               │
│    StackNode (root)                                                          │
│    └── TabNode (MainTabs)                                                    │
│        ├── StackNode (HomeTab)                                               │
│        │   ├── ScreenNode (Home)                                             │
│        │   └── ScreenNode (Detail)                                           │
│        └── StackNode (ProfileTab)                                            │
│            └── ScreenNode (Profile)                                          │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## Package Structure

The Quo Vadis library is organized into four main packages:

### `navigation` - Core Navigation Logic
- **Navigator interface and TreeNavigator implementation**
- **NavNode hierarchy** (ScreenNode, StackNode, TabNode, PaneNode)
- **Navigation operations** (push, pop, tab switching, pane navigation)
- **Destination and deep link handling**
- **Transition and result management**
- **Scope-aware and container-aware navigation**

### `compose` - Compose UI Rendering
- **HierarchicalNavigationHost** - Main entry point for UI rendering
- **Specialized renderers** (Screen, Stack, Tab, Pane) with animations
- **Animation and transition system** including predictive back gestures
- **Shared element transition support**
- **CompositionLocals and rendering scopes**

### `registry` - Extensibility System
- **ScreenRegistry** - Maps destinations to composable content
- **ContainerRegistry** - Provides custom tab/pane wrappers
- **TransitionRegistry** - Custom transition animations
- **ScopeRegistry** - Scope-aware navigation rules
- **DeepLinkRegistry** - Deep link handling
- **Composite registries** for modular composition

### `dsl` - Configuration DSL
- **NavigationConfigBuilder** - Main configuration entry point
- **StackBuilder, TabsBuilder, PanesBuilder** - Container configuration
- **Registry integration** - DSL for registering screens, containers, transitions
- **Type-safe configuration** with builder pattern
- **Animation and scope configuration**

---

## Logic Layer

The logic layer is responsible for managing navigation state as an immutable tree structure.

### Navigator Interface

`Navigator` is the central navigation controller interface that defines the contract for all navigation operations.

**Location**: `quo-vadis-core/.../core/navigation/navigator/Navigator.kt`

#### Key Properties

| Property | Type | Description |
|----------|------|-------------|
| `state` | `StateFlow<NavNode>` | Current navigation state as immutable tree |
| `currentDestination` | `StateFlow<NavDestination?>` | Active destination (derived) |
| `previousDestination` | `StateFlow<NavDestination?>` | Previous destination (derived) |
| `canNavigateBack` | `StateFlow<Boolean>` | Whether back navigation is possible |
| `config` | `NavigationConfig` | Complete navigation configuration |

#### Navigation Operations

```kotlin
// Basic navigation
fun navigate(destination: NavDestination, transition: NavigationTransition? = null)
fun navigateBack(): Boolean

// Stack manipulation
fun navigateAndClearTo(destination: NavDestination, clearRoute: String?, inclusive: Boolean)
fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition?)
fun navigateAndClearAll(destination: NavDestination)

// Deep linking
fun handleDeepLink(uri: String): Boolean
fun handleDeepLink(deepLink: DeepLink)
```

#### Configuration Integration

The Navigator now integrates directly with `NavigationConfig`, eliminating the need to pass configuration separately to NavigationHost:

```kotlin
val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = buildInitialState()
)

// Config is read from navigator
NavigationHost(navigator)
```

### TreeNavigator Implementation

`TreeNavigator` is the concrete implementation of `Navigator` using a tree-based state model.

**Location**: `quo-vadis-core/.../core/navigation/navigator/TreeNavigator.kt`

#### Key Features

1. **Immutable State Management**
   - Uses `MutableStateFlow<NavNode>` internally
   - Exposes as `StateFlow<NavNode>` to consumers
   - All mutations create new tree instances

2. **Container-Aware Navigation**
   - Checks if destination needs a container (tabs/panes)
   - Creates container structure when navigating to containerized destinations
   - Uses `NavigationConfig` to build appropriate nodes

3. **Scope-Aware Navigation**
   - Uses scope information from `NavigationConfig` to determine destination scopes
   - Out-of-scope destinations push to parent stack
   - Preserves predictive back gestures for container navigation

4. **Configuration Integration**
   - Stores complete `NavigationConfig` for rendering layer access
   - Eliminates config duplication between Navigator and NavigationHost

### TreeMutator

`TreeMutator` has been refactored into operation-specific modules:

**Locations**: 
- `quo-vadis-core/.../core/navigation/tree/operations/PushOperations.kt`
- `quo-vadis-core/.../core/navigation/tree/operations/PopOperations.kt`
- `quo-vadis-core/.../core/navigation/tree/operations/TabOperations.kt`
- `quo-vadis-core/.../core/navigation/tree/operations/PaneOperations.kt`
- `quo-vadis-core/.../core/navigation/tree/operations/BackOperations.kt`
- `quo-vadis-core/.../core/navigation/tree/operations/TreeNodeOperations.kt`

#### Design Philosophy

- **Pure Functions**: All operations take a tree and return a new tree
- **Immutability**: Original tree is never modified
- **Structural Sharing**: Unchanged subtrees are reused
- **Modular Organization**: Operations are organized by type (push, pop, tab, pane, back)

#### Key Operations

**Push Operations**
```kotlin
// Push with scope awareness
fun push(
    root: NavNode,
    destination: NavDestination,
    scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    keyGenerator: () -> String
): NavNode

// Push to specific stack
fun pushToStack(
    root: NavNode,
    stackKey: String,
    destination: NavDestination,
    keyGenerator: () -> String
): NavNode
```

**Pop Operations**
```kotlin
fun pop(root: NavNode, behavior: PopBehavior = PopBehavior.RemoveEmptyStacks): PopResult
fun popTo(root: NavNode, predicate: (NavNode) -> Boolean, inclusive: Boolean = false): NavNode
fun popToRoute(root: NavNode, route: String, inclusive: Boolean = false): NavNode
```

**Tab Operations**
```kotlin
fun switchTab(root: NavNode, nodeKey: String, tabIndex: Int): NavNode
fun switchActiveTab(root: NavNode, tabIndex: Int): NavNode
```

**Pane Operations**
```kotlin
fun navigateToPane(
    root: NavNode,
    nodeKey: String,
    role: PaneRole,
    destination: NavDestination,
    switchFocus: Boolean = true,
    keyGenerator: () -> String
): NavNode

fun switchActivePane(root: NavNode, nodeKey: String, role: PaneRole): NavNode
fun popPane(root: NavNode, nodeKey: String, role: PaneRole): NavNode?
```

---

## Rendering Layer

The rendering layer converts the navigation tree to Compose UI using hierarchical rendering.

### NavigationHost

`NavigationHost` is the main entry point for rendering navigation content, now featuring **hierarchical rendering**.

**Location**: `quo-vadis-core/.../core/compose/NavigationHost.kt`

#### Key Responsibilities

1. **State Collection**
   - Collects `navigator.state` as Compose state
   - Tracks previous state for animation pairing

2. **Infrastructure Setup**
   - Creates `SaveableStateHolder` for state preservation
   - Creates `ComposableCache` for lifecycle management
   - Creates `AnimationCoordinator` for transition resolution
   - Creates `PredictiveBackController` for gesture handling
   - Creates `BackAnimationController` for back animations

3. **Hierarchical Rendering**
   - Preserves parent-child relationships in the navigation tree
   - Enables scoped animations for each container type
   - Supports predictive back gestures across entire subtrees
   - Simplifies state management with container-scoped responsibility

4. **Configuration Integration**
   - Automatically reads configuration from `Navigator.config`
   - Eliminates need to pass config twice (once to Navigator, once to NavigationHost)

#### Usage Patterns

**Simple Usage** (Recommended):
```kotlin
@Composable
fun App() {
    val navigator = rememberQuoVadisNavigator(MainTabs::class, GeneratedNavigationConfig)
    
    // Config is now implicit - read from navigator
    NavigationHost(navigator)
}
```

**Advanced Usage with Custom Registries**:
```kotlin
NavigationHost(
    navigator = navigator,
    screenRegistry = MyGeneratedScreenRegistry,
    containerRegistry = MyGeneratedContainerRegistry,
    transitionRegistry = MyGeneratedTransitionRegistry,
    scopeRegistry = MyGeneratedScopeRegistry,
    enablePredictiveBack = true,
    windowSizeClass = currentWindowSizeClass()
)
```

---

### NavNodeRenderer

`NavNodeRenderer` is the core recursive renderer that dispatches to specialized renderers based on node type.

**Location**: `quo-vadis-core/.../core/compose/render/NavNodeRenderer.kt`

#### Dispatch Logic

```kotlin
@Composable
internal fun NavNodeRenderer(
    node: NavNode,
    previousNode: NavNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    when (node) {
        is ScreenNode -> ScreenRenderer(node, scope, modifier)
        is StackNode -> StackRenderer(node, previousNode as? StackNode, scope, modifier)
        is TabNode -> TabRenderer(node, previousNode as? TabNode, scope, modifier)
        is PaneNode -> PaneRenderer(node, previousNode as? PaneNode, scope, modifier)
    }
}
```

#### Hierarchical Design

1. **Hierarchical Rendering**: Preserves parent-child relationships
2. **Animation Pairing**: Uses `previousNode` for transition detection
3. **Recursive Traversal**: Each renderer may call `NavNodeRenderer` for children
4. **Container Scoping**: Each container manages its own animations and state

---

### Specialized Renderers

#### HierarchicalScreenRenderer

Renders leaf `ScreenNode` content via the screen registry.

**Features**:
- Uses `ComposableCache` for state preservation
- Provides `LocalScreenNode` and `LocalAnimatedVisibilityScope`
- Invokes `ScreenRegistry.Content()` with destination
- Supports shared element transitions

#### HierarchicalStackRenderer

Renders `StackNode` with animated stack transitions.

**Features**:
- Detects navigation direction (forward/back) by comparing stack sizes
- Uses `AnimatedNavContent` for transitions with proper animation pairing
- Enables predictive back for root stacks
- Recursively renders active child via `NavNodeRenderer`

#### HierarchicalTabRenderer

Renders `TabNode` with wrapper and tab switching animations.

**Features**:
- Creates `TabsContainerScope` for wrapper composable
- Caches entire TabNode (wrapper + content) as a unit
- Uses `AnimatedNavContent` for tab switching animations
- Invokes `ContainerRegistry.TabsContainer()` for custom wrappers

#### HierarchicalPaneRenderer

Renders `PaneNode` with adaptive multi-pane layouts.

**Features**:
- Detects window size for expanded/compact mode
- **Expanded mode**: Multiple panes side-by-side via `MultiPaneRenderer`
- **Compact mode**: Single pane with animations via `SinglePaneRenderer`
- Creates `PaneContainerScope` for wrapper composable
- Caches entire PaneNode for smooth layout transitions
- Configurable back behavior based on display mode

---

## NavNode Types

The navigation tree consists of four node types forming a sealed hierarchy.

### NavNode (Base Interface)

Base sealed interface for all navigation tree nodes.

```kotlin
@Serializable
sealed interface NavNode {
    val key: String        // Unique identifier
    val parentKey: String? // Parent node key (null for root)
}
```

#### Key Design Principle

> The NavNode tree represents **logical navigation state**, NOT visual layout state.

**What NavNode stores:**
- Which destinations exist in the navigation hierarchy
- Which pane/stack/tab is "active" (has navigation focus)
- Adaptation strategies (how panes SHOULD adapt when space is limited)

**What the Renderer determines:**
- Which panes are currently VISIBLE (based on WindowSizeClass)
- Layout arrangement (side-by-side, stacked, levitated)
- Animation states during transitions

---

### ScreenNode

Leaf node representing a single screen/destination.

**Location**: `quo-vadis-core/.../core/navigation/node/ScreenNode.kt`

```kotlin
@Serializable
@SerialName("screen")
data class ScreenNode(
    override val key: String,
    override val parentKey: String?,
    val destination: NavDestination
) : NavNode
```

| Property | Description |
|----------|-------------|
| `key` | Unique identifier for this screen instance |
| `parentKey` | Key of containing StackNode or PaneNode |
| `destination` | The destination data (route, arguments, transitions) |

**Characteristics**:
- Terminal node (cannot contain children)
- Always contained within a StackNode
- Holds reference to `NavDestination` for content rendering

---

### StackNode

Container node representing a linear navigation stack.

**Location**: `quo-vadis-core/.../core/navigation/node/StackNode.kt`

```kotlin
@Serializable
@SerialName("stack")
data class StackNode(
    override val key: String,
    override val parentKey: String?,
    val children: List<NavNode> = emptyList(),
    val scopeKey: String? = null
) : NavNode {
    val activeChild: NavNode?  // Last child (visible)
    val canGoBack: Boolean     // More than one entry
    val isEmpty: Boolean       // No children
    val size: Int              // Number of entries
}
```

| Property | Description |
|----------|-------------|
| `key` | Unique identifier for this stack |
| `parentKey` | Key of containing TabNode or PaneNode (null if root) |
| `children` | Ordered list of child nodes (last = active) |
| `scopeKey` | Identifier for scope-aware navigation |

**Behavior**:
- **Push**: Appends new node to `children`
- **Pop**: Removes last node from `children`
- **Scope-aware**: Out-of-scope destinations navigate to parent stack

---

### PaneNode

Container node for adaptive multi-pane layouts.

**Location**: `quo-vadis-core/.../core/navigation/node/PaneNode.kt`

```kotlin
@Serializable
@SerialName("pane")
data class PaneNode(
    override val key: String,
    override val parentKey: String?,
    val paneConfigurations: Map<PaneRole, PaneConfiguration>,
    val activePaneRole: PaneRole = PaneRole.Primary,
    val backBehavior: PaneBackBehavior = PaneBackBehavior.PopUntilScaffoldValueChange,
    val scopeKey: String? = null
) : NavNode {
    fun paneContent(role: PaneRole): NavNode?
    fun adaptStrategy(role: PaneRole): AdaptStrategy?
    val activePaneContent: NavNode?
    val paneCount: Int
    val configuredRoles: Set<PaneRole>
}
```

| Property | Description |
|----------|-------------|
| `key` | Unique identifier for this pane container |
| `parentKey` | Key of containing node (null if root) |
| `paneConfigurations` | Map of pane roles to their configurations |
| `activePaneRole` | Pane that currently has navigation focus |
| `backBehavior` | How back navigation should behave |
| `scopeKey` | Identifier for scope-aware navigation |

**Pane Roles**:
- `PaneRole.Primary` - Main content pane (required)
- `PaneRole.Supporting` - Detail/secondary content
- `PaneRole.Extra` - Additional content (rare)

**Adaptive Behavior**:
- **Compact mode**: Only `activePaneRole` is visible
- **Expanded mode**: Multiple panes side-by-side
- **Hierarchical rendering**: Each pane container manages its own layout

**Requirements**:
- Must have at least a Primary pane
- `activePaneRole` must exist in configurations

---

### TabNode

Container node for tabbed navigation with parallel stacks.

**Location**: `quo-vadis-core/.../core/navigation/node/TabNode.kt`

```kotlin
@Serializable
@SerialName("tab")
data class TabNode(
    override val key: String,
    override val parentKey: String?,
    val stacks: List<StackNode>,
    val activeStackIndex: Int = 0,
    val wrapperKey: String? = null,
    val tabMetadata: List<GeneratedTabMetadata> = emptyList(),
    val scopeKey: String? = null
) : NavNode {
    val activeStack: StackNode   // Currently selected tab's stack
    val tabCount: Int            // Number of tabs
}
```

| Property | Description |
|----------|-------------|
| `key` | Unique identifier for this tab container |
| `parentKey` | Key of containing node (null if root) |
| `stacks` | List of StackNodes, one per tab |
| `activeStackIndex` | Index of currently active tab (0-based) |
| `wrapperKey` | Key for `ContainerRegistry` wrapper lookup |
| `tabMetadata` | Metadata for each tab (label, icon, route) |
| `scopeKey` | Identifier for scope-aware navigation |

**Behavior**:
- **Switch Tab**: Updates `activeStackIndex`
- **Push**: Affects only the active stack
- **Pop**: Removes from active stack; may switch tabs if configured

**Requirements**:
- Must have at least one stack
- `activeStackIndex` must be valid index

---

## Registry System (Extensibility)

The registry system provides extensibility for screens, containers, transitions, and navigation behavior, now integrated through `NavigationConfig`.

### Core Registries

#### ScreenRegistry
Maps navigation destinations to composable screen content.

**Location**: `quo-vadis-core/.../core/registry/ScreenRegistry.kt`

```kotlin
interface ScreenRegistry {
    @Composable
    fun Content(
        destination: NavDestination,
        sharedTransitionScope: SharedTransitionScope? = null,
        animatedVisibilityScope: AnimatedVisibilityScope? = null
    )
}
```

**Usage**:
- Provides screen content for `ScreenRenderer`
- Supports shared element transitions
- Enables coordinator animations

#### ContainerRegistry
Provides custom wrappers for tab and pane containers.

**Location**: `quo-vadis-core/.../core/registry/ContainerRegistry.kt`

```kotlin
interface ContainerRegistry {
    @Composable
    fun TabsContainer(
        node: TabNode,
        content: @Composable () -> Unit
    )
    
    fun PaneContainer(
        node: PaneNode,
        content: @Composable () -> Unit
    )
}
```

**Features**:
- Custom tab bar implementations
- Custom pane layouts
- Container-scoped state management

#### NavigationConfig Integration

All registries are now accessed through `NavigationConfig`:

```kotlin
val config = navigationConfig {
    screens {
        screen<HomeScreen> { dest, sharedScope, animScope ->
            HomeScreenContent(dest, sharedScope, animScope)
        }
    }
    
    containers {
        tabsContainer("main") { content ->
            MyCustomTabBar(content)
        }
    }
}

val navigator = TreeNavigator(config = config)
```

This provides better organization and eliminates the need to manually wire registries together.

---

## Navigation Configuration

The navigation configuration system provides a unified way to define all aspects of navigation behavior, replacing the previous registry-specific approach.

### NavigationConfig Interface

**Location**: `quo-vadis-core/.../core/navigation/config/NavigationConfig.kt`

The `NavigationConfig` interface consolidates all navigation-related configuration:

```kotlin
interface NavigationConfig {
    val screenRegistry: ScreenRegistry
    val containerRegistry: ContainerRegistry
    val transitionRegistry: TransitionRegistry
    val scopeRegistry: ScopeRegistry
    val deepLinkRegistry: DeepLinkRegistry
}
```

This eliminates the need to manage multiple registries separately and provides a single configuration point.

---

## DSL Configuration

The DSL provides a declarative, type-safe API for configuring navigation, now with better integration and type safety.

### NavigationConfigBuilder

**Location**: `quo-vadis-core/.../core/dsl/NavigationConfigBuilder.kt`

Main entry point for navigation configuration:

```kotlin
class NavigationConfigBuilder {
    fun screens(block: ScreenRegistryBuilder.() -> Unit)
    fun containers(block: ContainerRegistryBuilder.() -> Unit)
    fun transitions(block: TransitionRegistryBuilder.() -> Unit)
    fun scopes(block: ScopeRegistryBuilder.() -> Unit)
    fun deepLinks(block: DeepLinkRegistryBuilder.() -> Unit)
}
```

### Screen Configuration

Defines a mapping from `NavDestination` to a composable.

**Location**: `quo-vadis-core/.../dsl/ScreenBuilder.kt`

```kotlin
class ScreenBuilder {
    fun screen(destination: NavDestination, content: () -> Unit)
}
```

### Container Configuration

Provides configuration for tabs and panes (order, initial configuration, etc.)

**Location**: `quo-vadis-core/.../dsl/ContainerBuilder.kt`

```kotlin
class TabsBuilder {
    fun tab(destination: NavDestination) // adds to the tabs list
}

class PanesBuilder {
    fun pane(role: PaneRole) // adds to the panes list
}
```

### Transition Configuration

Configures animated content scope and animation behavior.

**Location**: `quo-vadis-core/.../dsl/TransitionBuilder.kt`

```kotlin
class TransitionBuilder {
    fun <T> scope(block: AnimatedContentScope<T>.() -> Unit)
    fun <T> content(block: AnimatedContentScope<T>.() -> Unit)
}
```

### Scope Configuration

Defines scope boundaries and rules for navigation transitions.

**Location**: `quo-vadis-core/.../dsl/ScopeBuilder.kt`

```kotlin
class ScopeBuilder {
    fun <T> rules(role: PaneRole, block: PaneNavigationScopeBuilder<T>.() -> Unit)
}
```

### Configuration Usage

```kotlin
val config = navigationConfig {
    screens {
        screen<HomeScreen> { destination, sharedScope, animScope ->
            HomeScreenContent(destination, sharedScope, animScope)
        }
        screen<DetailScreen> { destination, sharedScope, animScope ->
            DetailScreenContent(destination, sharedScope, animScope)
        }
    }
    
    containers {
        stack<MainStack>("main") {
            screen<HomeScreen>()
            screen<DetailScreen>()
        }
        
        tabs<MainTabs>("tabs") {
            initialTab = 0
            tab(HomeTab, title = "Home", icon = Icons.Home)
            tab(ProfileTab, title = "Profile", icon = Icons.Profile)
        }
        
        panes<ListDetailPanes>("list-detail") {
            primary(weight = 0.4f) { root(ListScreen) }
            supporting(weight = 0.6f) { root(DetailPlaceholder) }
        }
    }
    
    transitions {
        transition<DetailScreen>(NavTransition.SlideHorizontal)
        transition<ModalScreen>(NavTransition.SlideVertical)
    }
    
    containers {
        tabsContainer("main-tabs") { content ->
            MyCustomTabBar(content)
        }
        
        paneContainer("list-detail") { content ->
            MultiPaneLayout(content)
        }
    }
}

// Use with Navigator (config is internal)
val navigator = TreeNavigator(config = config)

// Use with NavigationHost (config is read from navigator)
@Composable
fun App() {
    NavigationHost(navigator)
}
```

### DSL Benefits

1. **Type Safety**: Compile-time route validation and destination compatibility
2. **Integration**: Single configuration point through `NavigationConfig`
3. **Readability**: Declarative configuration syntax
4. **Composability**: Modular configuration blocks
5. **IDE Support**: Auto-completion and refactoring

---

## Hierarchical Rendering

The rendering layer now uses **hierarchical rendering** that preserves the navigation tree structure in Compose.

### Key Improvements

1. **Structure Preservation**: Parent-child relationships are maintained in Compose UI
2. **Scoped Animations**: Each container type manages its own animations
3. **Predictive Back**: Entire subtrees can animate during back gestures
4. **Simplified State**: Each renderer manages only its immediate concerns

### Rendering Architecture

```
NavNode Tree                     Compose UI Hierarchy
┌─ TabNode(MainTabs)             ┌─ TabsContainer
│  ├─ StackNode(HomeTab)    →    │  ├─ AnimatedContent (tab switch)
│  │  └─ ScreenNode(Home)        │  │  └─ HomeScreen
│  └─ StackNode(ProfileTab)     │  └─ AnimatedContent
│     └─ ScreenNode(Profile)      │     └─ ProfileScreen
```

### Benefits

- **Wrapper Preservation**: Tab bars and pane layouts are preserved during navigation
- **Animation Coordination**: Container animations are properly coordinated
- **Performance**: Efficient hierarchy with proper animation pairing
- **User Experience**: Smooth, intuitive navigation with proper visual feedback

---

## Life Cycle Management

The navigation graph is a powerful and intuitive model for state management during navigation.

- The tree is updated immutably (no mutable operations).
- Forward navigation creates a new "next" branch on the tree while preserving previous states.
- Back navigation reverts to a previous state from the backstack, if available.
- Predictive back animations require activating new states without committing them.
---

## FlowMVI Integration

With FlowMVI, the main activity can be a composition root and repository for stateful data processing.

### Characterization

1. `TreeNavigator.state` is a `StateFlow<NavNode>`.
2. `California NavigationConfigBuilder` provides an integral `store` for main activity to control.

### Implementation

- The `store` provides the MVI store implementation for the `TreeNavigator`.
- `FlowStore` has a `fakeUpdate` method for structure-preserving "predictive" back animations.

### Integration Example

**Location**: `quo-vadis-demos/.../demo/MainActivity.kt`

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val flowStore = rememberFlowStore(VoterCounterStore::class) { VoterCounterStoreImpl() }
            val navigator = withConfig(
                navConfig = generatedNavigationConfig,
                store = flowStore.store,
                stack = StandardStackFactory
            ) { navigator }
            NavigationHost(navigator)
        }
    }
}
```

## Layer Interaction

### Data Flow

```
User Action (tap, gesture)
         │
         ▼
┌─────────────────────────┐
│      Navigator          │
│  • navigate()           │
│  • navigateBack()       │
│  • updateState()        │
│  • config               │
└───────────┬─────────────┘
            │ TreeMutator operations
            ▼
┌─────────────────────────┐
│    NavNode Tree         │
│  • Immutable state      │
│  • Structural sharing   │
│  • Hierarchical structure│
└───────────┬─────────────┘
            │ StateFlow emission + NavigationConfig
            ▼
┌─────────────────────────┐
│   NavigationHost        │
│  • collectAsState()     │
│  • Config from Navigator│
│  • Predictive back        │
└───────────┬─────────────┘
            │ Hierarchical rendering
            ▼
┌─────────────────────────┐
│   NavNodeRenderer       │
│  • Type dispatch        │
│  • Specialized renders  │
│  • Animation pairing    │
│  • Hierarchy preserved  │
└───────────┬─────────────┘
            │
            ▼
      Compose UI Hierarchy
      (Parent-child preserved)
```

### Key Principles

1. **Unidirectional Data Flow**: State flows down, events flow up
2. **Single Source of Truth**: `Navigator.state` and `Navigator.config` 
3. **Immutable Updates**: Every navigation creates a new tree
4. **Separation of Concerns**: Logic layer doesn't know about UI
5. **Configuration Consolidation**: Single configuration through Navigator

---

## Summary

| Component | Layer | Responsibility | Location |
|-----------|-------|----------------|----------|
| `Navigator` | navigation | Navigation operations interface | `navigation/navigator/Navigator.kt` |
| `TreeNavigator` | navigation | Implementation with Config integration | `navigation/navigator/TreeNavigator.kt` |
| `TreeMutator` | navigation | Operations (refactored into modules) | `navigation/tree/operations/*.kt` |
| `ScreenNode` | navigation | Single screen destination | `navigation/node/ScreenNode.kt` |
| `StackNode` | navigation | Linear navigation stack | `navigation/node/StackNode.kt` |
| `TabNode` | navigation | Parallel tabbed navigation | `navigation/node/TabNode.kt` |
| `PaneNode` | navigation | Adaptive multi-pane container | `navigation/node/PaneNode.kt` |
| `NavigationHost` | compose | Entry point with Config integration | `compose/NavigationHost.kt` |
| `NavNodeRenderer` | compose | Type-based dispatch to specialized renderers | `compose/render/NavNodeRenderer.kt` |
| `ScreenRenderer` | compose | Leaf content rendering with shared elements | `compose/render/ScreenRenderer.kt` |
| `StackRenderer` | compose | Animated stack transitions | `compose/render/StackRenderer.kt` |
| `TabRenderer` | compose | Tab wrapper and switching | `compose/render/TabRenderer.kt` |
| `PaneRenderer` | compose | Adaptive multi-pane layouts | `compose/render/PaneRenderer.kt` |
| `ScreenRegistry` | registry | Maps destinations to composable content | `registry/ScreenRegistry.kt` |
| `ContainerRegistry` | registry | Custom tab/pane wrappers | `registry/ContainerRegistry.kt` |
| `TransitionRegistry` | registry | Custom transition animations | `registry/TransitionRegistry.kt` |
| `ScopeRegistry` | registry | Scope-aware navigation rules | `registry/ScopeRegistry.kt` |
| `DeepLinkRegistry` | registry | Deep link handling | `registry/DeepLinkRegistry.kt` |
| `NavigationConfig` | dsl | Unified configuration interface | `dsl/NavigationConfig.kt` |
| `NavigationConfigBuilder` | dsl | Main configuration entry point | `dsl/NavigationConfigBuilder.kt` |
| `StackBuilder` | dsl | Stack configuration DSL | `dsl/StackBuilder.kt` |
| `TabsBuilder` | dsl | Tab configuration DSL | `dsl/TabsBuilder.kt` |
| `PanesBuilder` | dsl | Pane configuration DSL | `dsl/PanesBuilder.kt` |

## Key Architectural Changes

1. **Configuration Consolidation**: All registries now managed through single `NavigationConfig`
2. **Navigator Integration**: `Navigator` now holds complete configuration, eliminating duplication
3. **Hierarchical Rendering**: Rendering layer preserves navigation tree structure in Compose UI
4. **Modular TreeMutator**: Navigation operations organized by type (push, pop, tab, pane, back)
5. **Enhanced Type Safety**: Better integration between DSL, registries, and navigation logic

This architecture provides a clean separation of concerns while maintaining tight integration between the configuration system and navigation runtime, enabling powerful features like hierarchical rendering, predictive back gestures, and adaptive multi-pane layouts.
