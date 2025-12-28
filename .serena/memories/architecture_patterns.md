# Architecture Patterns

## Core Architecture

The Quo Vadis navigation library uses a **tree-based navigation architecture** where navigation state is represented as a tree of nodes.

### Navigation Tree Model

```
NavNode (root)
├── StackNode (main stack)
│   ├── ScreenNode (Home)
│   ├── ScreenNode (List)
│   └── ScreenNode (Detail)
├── TabNode (bottom tabs)
│   ├── StackNode (Tab 1 stack)
│   │   └── ScreenNode
│   └── StackNode (Tab 2 stack)
│       └── ScreenNode
└── PaneNode (adaptive layout)
    ├── StackNode (primary)
    └── StackNode (detail)
```

### Node Types

| Node | Purpose | Key Features |
|------|---------|--------------|
| `NavNode` | Base sealed interface | Has `key`, `parentKey` |
| `ScreenNode` | Single screen/destination | Contains `NavDestination`, implements `LifecycleAwareNode` |
| `StackNode` | Stack of screens | Push/pop operations, backstack |
| `TabNode` | Tab container | Independent stacks per tab, implements `LifecycleAwareNode` |
| `PaneNode` | Adaptive layout | Primary/detail panes, implements `LifecycleAwareNode` |

## Lifecycle Management

Navigation nodes implement `LifecycleAwareNode` for proper lifecycle state management.

### LifecycleAwareNode Interface

```kotlin
interface LifecycleAwareNode {
    val isAttachedToNavigator: Boolean
    val isDisplayed: Boolean
    var composeSavedState: Map<String, List<Any?>>?
    
    fun attachToNavigator()
    fun attachToUI()
    fun detachFromUI()
    fun detachFromNavigator()
    fun addOnDestroyCallback(callback: () -> Unit)
    fun removeOnDestroyCallback(callback: () -> Unit)
}
```

### State Transitions

```
[Created] -> attachToNavigator() -> [Attached] -> attachToUI() -> [Displayed]
                                          ^                            |
                                          |                            v
                                          +---- detachFromUI() --------+
                                          |
                                          v
                              detachFromNavigator() -> [Destroyed]
```

### Lifecycle Callbacks

External components (like MVI containers) can register for lifecycle events via `addOnDestroyCallback()`. This is used to close coroutine scopes and Koin scopes when a screen/container is destroyed.

## Key Components

### Navigator Interface

The central navigation controller:

```kotlin
interface Navigator {
    val state: StateFlow<NavNode>
    val currentDestination: StateFlow<NavDestination?>
    val canNavigateBack: StateFlow<Boolean>
    
    fun navigate(destination: NavDestination)
    fun navigateBack(): Boolean
    fun navigateForResult<T>(destination: ReturnsResult<T>): T?
    fun navigateBackWithResult<T>(result: T)
}
```

### TreeMutator

For tree manipulation:

```kotlin
// All operations are pure functions
TreeMutator.push(root, destination, scopeRegistry, keyGenerator)
TreeMutator.pop(root, behavior)
TreeMutator.popTo(root, predicate, inclusive)
TreeMutator.switchTab(root, nodeKey, tabIndex)
TreeMutator.navigateToPane(root, nodeKey, role, destination, switchFocus, keyGenerator)
```

## FlowMVI Integration

The `quo-vadis-core-flow-mvi` module provides MVI architecture integration.

### NavigationContainer (Screen-Scoped)

```kotlin
class ProfileContainer(scope: NavigationContainerScope) :
    NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {
    
    // Access via scope:
    // - navigator: Navigator
    // - screenKey: String
    // - coroutineScope: CoroutineScope
    
    override val store = store(ProfileState()) {
        reduce { intent -> /* handle intent */ }
    }
}

// In composable
val store = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
```

### SharedNavigationContainer (Container-Scoped)

For shared state across all screens in a Tab/Pane:

```kotlin
class MainTabsContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<TabsState, TabsIntent, TabsAction>(scope) {
    
    // Access via scope:
    // - navigator: Navigator
    // - containerKey: String
    // - containerScope: CoroutineScope
    
    override val store = store(TabsState()) {
        reduce { intent -> /* handle intent */ }
    }
}

// In tab wrapper
val store = rememberSharedContainer<MainTabsContainer, TabsState, TabsIntent, TabsAction>()
CompositionLocalProvider(LocalMyStore provides store) {
    content()
}

// Child screens access via CompositionLocal
val tabsStore = LocalMyStore.current
```

### Koin Registration

```kotlin
val myModule = module {
    navigationContainer<ProfileContainer> { scope ->
        ProfileContainer(scope)
    }
    
    sharedNavigationContainer<MainTabsContainer> { scope ->
        MainTabsContainer(scope)
    }
}
```

## Registry System

| Registry | Purpose |
|----------|---------|
| `RouteRegistry` | Maps routes to destinations |
| `ScreenRegistry` | Maps destinations to composables |
| `ContainerRegistry` | Container info + Tab/Pane wrapper composables |
| `TransitionRegistry` | Maps destinations to transitions |
| `ScopeRegistry` | Scoped navigation |

## CompositionLocals

| Local | Type | Description |
|-------|------|-------------|
| `LocalScreenNode` | `ScreenNode?` | Current screen node |
| `LocalContainerNode` | `LifecycleAwareNode?` | Current container node (Tab/Pane) |
| `LocalNavigator` | `Navigator?` | Navigator instance |
| `LocalNavRenderScope` | `NavRenderScope?` | Render context with animations |

## Navigation Results

Type-safe result passing between screens:

```kotlin
// Destination that returns a result
@Destination(route = "picker")
data object ItemPicker : MyDestination(), ReturnsResult<SelectedItem>

// Navigate and await result
val result: SelectedItem? = navigator.navigateForResult(ItemPicker)

// Return result from picker screen
navigator.navigateBackWithResult(SelectedItem("123", "Apple"))
```

## Rendering Architecture

### NavTreeRenderer

Renders the navigation tree to Compose UI:
1. Dispatches to specialized renderers (ScreenRenderer, StackRenderer, TabRenderer, PaneRenderer)
2. Manages lifecycle transitions (attachToUI, detachFromUI)
3. Handles predictive back animation
4. Provides CompositionLocals for screens

### NavigationHost

Main entry point for navigation UI:

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true,
    windowSizeClass: WindowSizeClass? = null
)
```

## Testing

### FakeNavigator

```kotlin
val navigator = FakeNavigator()
navigator.navigate(HomeDestination)
assertTrue(navigator.navigationStack.size == 1)
assertEquals(HomeDestination, navigator.currentDestination.value)
```
