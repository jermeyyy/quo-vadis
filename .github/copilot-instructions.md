# Quo Vadis Navigation Library

Kotlin Multiplatform navigation library with Compose Multiplatform UI using a **tree-based architecture**.

## Project Structure

| Module | Purpose | Package |
|--------|---------|---------|
| `quo-vadis-core` | Core navigation library (Navigator, NavNodes, TreeMutator) | `com.jermey.quo.vadis.core` |
| `quo-vadis-annotations` | KSP annotations (`@Stack`, `@Destination`, `@Screen`, `@Tabs`, `@Pane`) | `com.jermey.quo.vadis.annotations` |
| `quo-vadis-ksp` | Code generator for zero-boilerplate navigation | - |
| `quo-vadis-gradle-plugin` | Gradle plugin for KSP configuration | - |
| `quo-vadis-core-flow-mvi` | FlowMVI integration for MVI architecture | `com.jermey.quo.vadis.flowmvi` |
| `composeApp` | Demo application showcasing all patterns | `com.jermey.navplayground.demo` |
| `feature1`, `feature2` | Multi-module feature examples | - |

## Architecture: NavNode Tree

Navigation state is an **immutable tree** of nodes:

```
StackNode (root)
├── TabNode (MainTabs)
│   ├── StackNode (HomeTab)
│   │   ├── ScreenNode (Home)
│   │   └── ScreenNode (Detail)
│   └── StackNode (ProfileTab)
│       └── ScreenNode (Profile)
└── PaneNode (adaptive layout)
    ├── StackNode (primary)
    └── StackNode (detail)
```

### Core Types

- **`NavNode`** - Base sealed interface (`quo-vadis-core/.../navigation/node/`)
- **`ScreenNode`** - Leaf node for individual screens
- **`StackNode`** - Push/pop navigation stack
- **`TabNode`** - Parallel tab-based navigation
- **`PaneNode`** - Adaptive multi-pane layouts

### Key Components

- **`Navigator`** interface - Central navigation controller (`quo-vadis-core/.../navigation/navigator/Navigator.kt`)
- **`TreeNavigator`** - Concrete implementation with StateFlow state
- **`TreeMutator`** - Pure functions for immutable tree operations (`quo-vadis-core/.../navigation/internal/tree/TreeMutator.kt`)
- **`NavigationHost`** - Compose entry point (`quo-vadis-core/.../compose/NavigationHost.kt`)

## Annotation-Based Navigation

### Defining Destinations

```kotlin
@Stack(name = "home", startDestination = Feed::class)
sealed class HomeDestination : NavDestination {
    @Destination(route = "home/feed")
    data object Feed : HomeDestination()

    @Destination(route = "home/article/{articleId}")
    data class Article(@Argument val articleId: String) : HomeDestination()
}
```

### Screen Binding

```kotlin
@Screen(HomeDestination.Feed::class)
@Composable
fun FeedScreen(navigator: Navigator) { /* ... */ }

@Screen(HomeDestination.Article::class)
@Composable
fun ArticleScreen(destination: HomeDestination.Article, navigator: Navigator) { /* ... */ }
```

### Tabs and Panes

- **`@Tabs`** + **`@TabItem`** - Tab containers with independent backstacks
- **`@TabsContainer`** - Custom tab bar UI
- **`@Pane`** + **`@PaneItem`** - Adaptive multi-pane layouts
- **`@PaneContainer`** - Custom pane wrapper

## Build Commands

```bash
# Full build
./gradlew build

# Run tests
./gradlew test

# Detekt (static analysis)
./gradlew detekt

# Generate API docs
./gradlew :quo-vadis-core:dokkaGenerateHtml

# Run demo (platform-specific)
./gradlew :composeApp:run                           # Desktop
./gradlew :composeApp:installDebug                  # Android
./gradlew :composeApp:jsBrowserDevelopmentRun       # JS Web
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # WASM Web
```

## Code Style & Conventions

- **Destinations**: Sealed classes extending `NavDestination` with `@Stack` annotation
- **Screens**: `@Composable` functions with `@Screen(Destination::class)` annotation
- **Arguments**: Use `@Argument` on data class properties (auto-serialized for deep links)
- **Naming**: `*Destination` for sealed classes, `*Screen` for composables, `*Container` for MVI
- **Immutability**: All tree mutations via `TreeMutator` - never modify `NavNode` directly

## MVI Integration (quo-vadis-core-flow-mvi)

### Screen-Scoped Container

```kotlin
class ProfileContainer(scope: NavigationContainerScope) :
    NavigationContainer<ProfileState, ProfileIntent, ProfileAction>(scope) {
    override val store = store(ProfileState()) { /* ... */ }
}

// Usage in screen
val store = rememberContainer<ProfileContainer, ProfileState, ProfileIntent, ProfileAction>()
```

### Shared Container (Tab/Pane-Scoped)

```kotlin
class MainTabsContainer(scope: SharedContainerScope) :
    SharedNavigationContainer<TabsState, TabsIntent, TabsAction>(scope) {
    override val store = store(TabsState()) { /* ... */ }
}

// Usage in tab wrapper
val store = rememberSharedContainer<MainTabsContainer, ...>()
```

## Key Patterns

1. **Unidirectional data flow**: `state: StateFlow<NavNode>` is single source of truth
2. **Type-safe navigation**: No string-based routing, use destination data classes
3. **Scope-aware navigation**: Destinations can specify scope for automatic parent-stack routing
4. **Container-aware navigation**: `ContainerRegistry` builds Tab/Pane structures automatically
5. **Predictive back**: Android 13+ and iOS swipe-back gestures supported

## Testing

Use `FakeNavigator` from `quo-vadis-core` for unit tests:

```kotlin
val navigator = TreeNavigator(
    config = GeneratedNavigationConfig,
    initialState = config.buildNavNode(HomeDestination::class, null)!!
)
navigator.navigate(HomeDestination.Article("123"))
assertEquals(HomeDestination.Article("123"), navigator.currentDestination.value)
```

## Platform Targets

Android, iOS (arm64/simulator/x64), Desktop (JVM), JavaScript (IR), WebAssembly (wasmJs)
