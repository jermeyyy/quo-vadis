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
| `NavNode` | Base type for all nodes | Has `key`, parent reference |
| `ScreenNode` | Single screen/destination | Contains `Destination`, handles content |
| `StackNode` | Stack of screens | Push/pop operations, backstack |
| `TabNode` | Tab container | Independent stacks per tab |
| `PaneNode` | Adaptive layout | Primary/detail panes |

## Key Components

### Navigator Interface

The central navigation controller:

```kotlin
interface Navigator {
    val state: StateFlow<NavNode>
    
    // Basic navigation
    fun navigate(destination: Destination)
    fun navigateBack(): Boolean
    
    // Tree manipulation
    fun mutate(mutation: TreeMutator.() -> NavNode?)
    
    // Stack operations
    fun popTo(predicate: (NavNode) -> Boolean): Boolean
    fun popToRoot(): Boolean
}
```

### TreeMutator

For complex navigation state changes:

```kotlin
navigator.mutate { node ->
    node.pop()
}

navigator.mutate { node ->
    node.popTo { it.destination.route == "home" }
}

navigator.mutate { node ->
    node.push(DetailDestination(id))
}
```

### Destination Pattern

Type-safe destinations using sealed classes:

```kotlin
sealed class AppDestination : Destination {
    @Route("home")
    data object Home : AppDestination()
    
    @Route("detail/{id}")
    @Argument(DetailData::class)
    data class Detail(val id: String) : AppDestination(),
        TypedDestination<DetailData> {
        override val data = DetailData(id)
    }
}
```

## Registry System

### Registries

The library uses a registry pattern for decoupling:

| Registry | Purpose |
|----------|---------|
| `RouteRegistry` | Maps routes to destinations |
| `ScreenRegistry` | Maps destinations to composables |
| `ContainerRegistry` | Maps container nodes to layouts |
| `TransitionRegistry` | Maps destinations to transitions |
| `WrapperRegistry` | Tab/Pane wrapper composables |
| `BackHandlerRegistry` | Custom back handling |
| `ScopeRegistry` | Scoped dependencies |

### Registration (via KSP)

KSP generates registration code:

```kotlin
// Generated initializer
object AppDestinationRouteInitializer {
    fun initialize() {
        RouteRegistry.register("home", AppDestination.Home::class)
        RouteRegistry.register("detail/{id}", AppDestination.Detail::class)
    }
}
```

## Rendering Architecture

### NavTreeRenderer

Renders the navigation tree to Compose UI:

1. Flattens tree to visible nodes
2. Determines transitions
3. Handles predictive back animation
4. Manages composition lifecycle

### AnimatedNavContent

Handles screen transitions:

```kotlin
AnimatedNavContent(
    targetNode = currentNode,
    transition = transition,
    backProgress = backProgress
) { node ->
    // Render screen content
}
```

## Compose Integration

### NavigationHost

Main entry point for navigation UI:

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    defaultTransition: NavigationTransition = NavigationTransitions.Fade,
    predictiveBackEnabled: Boolean = true
)
```

### Content Registration

Using `@Content` annotation:

```kotlin
@Content(AppDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) {
    HomeScreen(navigator)
}

@Content(AppDestination.Detail::class)
@Composable
fun DetailContent(data: DetailData, navigator: Navigator) {
    DetailScreen(data, navigator)
}
```

## Predictive Back Navigation

### CascadeBackState

Coordinates back gesture across the tree:

```kotlin
enum class PredictiveBackMode {
    None,       // No predictive back
    Animate,    // Animate during gesture
    Complete    // Gesture completed
}
```

### PredictiveBackController

Handles gesture progress and completion.

## Testing Architecture

### FakeNavigator

For unit testing navigation logic:

```kotlin
val fakeNavigator = FakeNavigator()
fakeNavigator.navigate(HomeDestination)

assertTrue(fakeNavigator.verifyNavigateTo("home"))
assertEquals(1, fakeNavigator.navigationStack.size)
```

### FakeNavRenderScope

For testing composable content in isolation.

## Deep Linking

### DeepLink Pattern

```kotlin
@DeepLink("myapp://detail/{id}")
data class DetailDestination(val id: String) : Destination

// Handler
val handler = GeneratedDeepLinkHandler()
val destination = handler.handleUri("myapp://detail/123")
```

## Modular Navigation

### Gray Box Pattern

Feature modules expose navigation without exposing implementation:

```kotlin
// Feature module exposes
interface FeatureNavigation {
    fun navigateToFeature(navigator: Navigator, params: FeatureParams)
}

// Implementation hidden
internal class FeatureNavigationImpl : FeatureNavigation {
    override fun navigateToFeature(navigator: Navigator, params: FeatureParams) {
        navigator.navigate(FeatureDestination(params))
    }
}
```

## DI Integration

### Koin Support

Built-in Koin integration helpers in `KoinIntegration.kt`:

```kotlin
@Composable
fun NavigationHost(
    navigator: Navigator,
    koinModule: Module = module { }
) {
    KoinContext(modules = listOf(koinModule)) {
        // Navigation content
    }
}
```
