# Quo Vadis Navigation Architecture

This document describes the architectural design of the Quo Vadis navigation library, covering the logic layer, rendering layer, and the navigation node types.

## Table of Contents

- [Overview](#overview)
- [Architecture Diagram](#architecture-diagram)
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
- [Layer Interaction](#layer-interaction)

---

## Overview

Quo Vadis uses a **tree-based navigation architecture** where:

1. **Logic Layer** manages navigation state as an immutable tree of `NavNode` objects
2. **Rendering Layer** recursively renders the tree to Compose UI with animations

This separation ensures:
- **Clean state management**: All navigation logic is isolated in the logic layer
- **Flexible rendering**: UI adapts to different screen sizes and platforms
- **Testability**: Logic can be unit tested without UI dependencies

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
│  └───────────────────────────────────┬─────────────────────────────────────┘ │
│                                      │                                        │
│                                      ▼                                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        TreeNavigator                                     │ │
│  │  (Implementation)                                                        │ │
│  │  - MutableStateFlow<NavNode> for state management                       │ │
│  │  - Container-aware navigation                                           │ │
│  │  - Scope-aware navigation                                               │ │
│  │  - Lifecycle event dispatch                                             │ │
│  └───────────────────────────────────┬─────────────────────────────────────┘ │
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

## Logic Layer

The logic layer is responsible for managing navigation state as an immutable tree structure.

### Navigator Interface

`Navigator` is the central navigation controller interface that defines the contract for all navigation operations.

**Location**: `quo-vadis-core/.../core/Navigator.kt`

#### Key Properties

| Property | Type | Description |
|----------|------|-------------|
| `state` | `StateFlow<NavNode>` | Current navigation state as immutable tree |
| `transitionState` | `StateFlow<TransitionState>` | Animation/transition state |
| `currentDestination` | `StateFlow<NavDestination?>` | Active destination (derived) |
| `previousDestination` | `StateFlow<NavDestination?>` | Previous destination (derived) |
| `canNavigateBack` | `StateFlow<Boolean>` | Whether back navigation is possible |
| `resultManager` | `NavigationResultManager` | Manages navigation result passing |
| `lifecycleManager` | `NavigationLifecycleManager` | Manages lifecycle callbacks |

#### Navigation Operations

```kotlin
// Basic navigation
fun navigate(destination: NavDestination, transition: NavigationTransition? = null)
fun navigateBack(): Boolean

// Stack manipulation
fun navigateAndClearTo(destination: NavDestination, clearRoute: String?, inclusive: Boolean)
fun navigateAndReplace(destination: NavDestination, transition: NavigationTransition?)
fun navigateAndClearAll(destination: NavDestination)

// Pane operations
fun isPaneAvailable(role: PaneRole): Boolean
fun paneContent(role: PaneRole): NavNode?
fun navigateBackInPane(role: PaneRole): Boolean

// Deep linking
fun handleDeepLink(deepLink: DeepLink)
fun getDeepLinkHandler(): DeepLinkHandler

// State manipulation (advanced)
fun updateState(newState: NavNode, transition: NavigationTransition?)
```

#### Predictive Back Support

```kotlin
fun startPredictiveBack()
fun updatePredictiveBack(progress: Float, touchX: Float, touchY: Float)
fun cancelPredictiveBack()
fun commitPredictiveBack()
fun completeTransition()
```

---

### TreeNavigator Implementation

`TreeNavigator` is the concrete implementation of `Navigator` using a tree-based state model.

**Location**: `quo-vadis-core/.../core/TreeNavigator.kt`

#### Constructor Parameters

```kotlin
class TreeNavigator(
    private val deepLinkHandler: DeepLinkHandler = DefaultDeepLinkHandler(),
    private val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    initialState: NavNode? = null,
    private val scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    private val containerRegistry: ContainerRegistry = ContainerRegistry.Empty
) : Navigator
```

| Parameter | Description |
|-----------|-------------|
| `deepLinkHandler` | Handler for deep link navigation |
| `coroutineScope` | Scope for derived state computations |
| `initialState` | Optional initial navigation state |
| `scopeRegistry` | Registry for scope-aware navigation |
| `containerRegistry` | Registry for container-aware navigation |

#### Key Features

1. **Immutable State Management**
   - Uses `MutableStateFlow<NavNode>` internally
   - Exposes as `StateFlow<NavNode>` to consumers
   - All mutations create new tree instances

2. **Container-Aware Navigation**
   - Checks if destination needs a container (tabs/panes)
   - Creates container structure when navigating to containerized destinations
   - Uses `ContainerRegistry` to build appropriate nodes

3. **Scope-Aware Navigation**
   - Uses `ScopeRegistry` to determine destination scopes
   - Out-of-scope destinations push to parent stack
   - Preserves predictive back gestures for container navigation

4. **Derived State Computation**
   - `currentDestination`, `previousDestination`, `canNavigateBack` are derived
   - Updated synchronously after state mutations
   - Backed by `MutableStateFlow` for test compatibility

5. **Lifecycle Event Dispatch**
   - Dispatches `onScreenExited` and `onScreenDestroyed` events
   - Cancels pending results for destroyed screens
   - Uses coroutine for async dispatch

---

### TreeMutator

`TreeMutator` is a singleton object containing pure functions for tree manipulation.

**Location**: `quo-vadis-core/.../core/TreeMutator.kt`

#### Design Philosophy

- **Pure Functions**: All operations take a tree and return a new tree
- **Immutability**: Original tree is never modified
- **Structural Sharing**: Unchanged subtrees are reused

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

**Back Navigation**
```kotlin
fun popWithTabBehavior(root: NavNode): BackResult
fun canHandleBackNavigation(root: NavNode): Boolean
```

#### Result Types

```kotlin
sealed class PopResult {
    data class Popped(val newState: NavNode) : PopResult()
    data object AlreadyEmpty : PopResult()
    data object AtRoot : PopResult()
}

sealed class BackResult {
    data class Handled(val newState: NavNode) : BackResult()
    data object DelegateToSystem : BackResult()
    data object CannotHandle : BackResult()
}
```

---

## Rendering Layer

The rendering layer converts the navigation tree to Compose UI.

### NavigationHost

`NavigationHost` is the main entry point for rendering navigation content.

**Location**: `quo-vadis-core/.../compose/NavigationHost.kt`

#### Usage

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = EmptyScreenRegistry,
    containerRegistry: ContainerRegistry = ContainerRegistry.Empty,
    transitionRegistry: TransitionRegistry = TransitionRegistry.Empty,
    scopeRegistry: ScopeRegistry = ScopeRegistry.Empty,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
)

// Or with unified config
@Composable
fun NavigationHost(
    navigator: Navigator,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
)
```

#### Responsibilities

1. **State Collection**
   - Collects `navigator.state` as Compose state
   - Tracks previous state for animation pairing

2. **Infrastructure Setup**
   - Creates `SaveableStateHolder` for state preservation
   - Creates `ComposableCache` for lifecycle management
   - Creates `AnimationCoordinator` for transition resolution
   - Creates `PredictiveBackController` for gesture handling
   - Creates `BackAnimationController` for back animations

3. **Predictive Back Integration**
   - Wraps content in `NavigateBackHandler`
   - Computes speculative pop state at gesture start
   - Updates animation progress during gesture
   - Commits or cancels navigation on gesture completion

4. **Shared Element Transitions**
   - Wraps content in `SharedTransitionLayout`
   - Provides `SharedTransitionScope` via `LocalNavRenderScope`

5. **CompositionLocal Setup**
   - Provides `LocalNavRenderScope` for render context
   - Provides `LocalBackHandlerRegistry` for custom back handlers
   - Provides `LocalBackAnimationController` for animations

---

### NavNodeRenderer

`NavNodeRenderer` is the core recursive renderer that dispatches to specialized renderers.

**Location**: `quo-vadis-core/.../compose/render/NavTreeRenderer.kt`

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

#### Design Principles

1. **Hierarchical Rendering**: Preserves parent-child relationships
2. **Animation Pairing**: Uses `previousNode` for transition detection
3. **Recursive Traversal**: Each renderer may call `NavNodeRenderer` for children

---

### Specialized Renderers

#### ScreenRenderer

Renders leaf `ScreenNode` content via the screen registry.

**Features**:
- Uses `ComposableCache` for state preservation
- Provides `LocalScreenNode` and `LocalAnimatedVisibilityScope`
- Invokes `ScreenRegistry.Content()` with destination

#### StackRenderer

Renders `StackNode` with animated transitions.

**Features**:
- Detects navigation direction (forward/back) by comparing stack sizes
- Uses `AnimatedNavContent` for transitions
- Enables predictive back for root stacks
- Recursively renders active child via `NavNodeRenderer`

#### TabRenderer

Renders `TabNode` with wrapper and tab switching.

**Features**:
- Creates `TabsContainerScope` for wrapper composable
- Caches entire TabNode (wrapper + content) as a unit
- Uses `AnimatedNavContent` for tab switching animations
- Invokes `ContainerRegistry.TabsContainer()` for custom wrappers

#### PaneRenderer

Renders `PaneNode` with adaptive layout.

**Features**:
- Detects window size for expanded/compact mode
- **Expanded mode**: Multiple panes side-by-side via `MultiPaneRenderer`
- **Compact mode**: Single pane with animations via `SinglePaneRenderer`
- Creates `PaneContainerScope` for wrapper composable
- Caches entire PaneNode for smooth layout transitions

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

**Location**: `quo-vadis-core/.../core/ScreenNode.kt`

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

**Location**: `quo-vadis-core/.../core/StackNode.kt`

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

### TabNode

Container node for tabbed navigation with parallel stacks.

**Location**: `quo-vadis-core/.../core/TabNode.kt`

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

### PaneNode

Container node for adaptive multi-pane layouts.

**Location**: `quo-vadis-core/.../core/PaneNode.kt`

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
- **Compact screens**: Only `activePaneRole` is visible
- **Medium screens**: Primary visible, others can levitate
- **Expanded screens**: Multiple panes side-by-side

**Requirements**:
- Must have at least a Primary pane
- `activePaneRole` must exist in configurations

---

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
└───────────┬─────────────┘
            │ TreeMutator operations
            ▼
┌─────────────────────────┐
│    NavNode Tree         │
│  • Immutable state      │
│  • Structural sharing   │
└───────────┬─────────────┘
            │ StateFlow emission
            ▼
┌─────────────────────────┐
│   NavigationHost        │
│  • collectAsState()     │
│  • Animation setup      │
│  • Predictive back      │
└───────────┬─────────────┘
            │ Recursive rendering
            ▼
┌─────────────────────────┐
│   NavNodeRenderer       │
│  • Type dispatch        │
│  • Specialized renders  │
│  • AnimatedNavContent   │
└───────────┬─────────────┘
            │
            ▼
      Compose UI
```

### Key Principles

1. **Unidirectional Data Flow**: State flows down, events flow up
2. **Single Source of Truth**: `Navigator.state` is the only source
3. **Immutable Updates**: Every navigation creates a new tree
4. **Separation of Concerns**: Logic layer doesn't know about UI, UI observes state

---

## Summary

| Component | Layer | Responsibility |
|-----------|-------|----------------|
| `Navigator` | Logic | Navigation operations interface |
| `TreeNavigator` | Logic | Concrete implementation with StateFlow |
| `TreeMutator` | Logic | Pure functions for tree manipulation |
| `NavigationHost` | Rendering | Entry point, infrastructure setup |
| `NavNodeRenderer` | Rendering | Type-based dispatch to specialized renderers |
| `ScreenRenderer` | Rendering | Leaf content rendering |
| `StackRenderer` | Rendering | Animated stack transitions |
| `TabRenderer` | Rendering | Tab wrapper and switching |
| `PaneRenderer` | Rendering | Adaptive multi-pane layouts |
| `NavNode` | Data | Base interface for tree nodes |
| `ScreenNode` | Data | Single screen destination |
| `StackNode` | Data | Linear navigation stack |
| `TabNode` | Data | Parallel tabbed navigation |
| `PaneNode` | Data | Adaptive multi-pane container |
