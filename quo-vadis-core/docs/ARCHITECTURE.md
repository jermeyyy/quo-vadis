# Navigation Library - Architecture Overview

## Design Principles

This navigation library follows these key principles:

1. **Modularization First**: Support for feature modules with gray box pattern
2. **Type Safety**: Compile-time safety for navigation
3. **MVI Compatible**: First-class support for MVI architecture
4. **Testable**: Easy to test with fake implementations
5. **Multiplatform**: Works on Android, iOS, Desktop, Web
6. **No External Dependencies**: Independent from other navigation libraries

## Architecture Layers

```
┌─────────────────────────────────────────────────────────┐
│                    Application Layer                    │
│  (Your screens, ViewModels, feature modules)            │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│               Code Generation Layer (KSP)               │
│  - Processes @Graph, @Route, @Argument, @Content        │
│  - Generates route initializers                          │
│  - Generates graph builders                              │
│  - Generates typed navigation extensions                 │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Integration Layer                     │
│  - MVI Support (Intents, Effects, State)                │
│  - DI Integration (Koin, Kodein, etc.)                  │
│  - Testing Support (FakeNavigator)                       │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                    Compose Layer                         │
│  - NavHost (Rendering)                                   │
│  - GraphNavHost (Graph-based rendering)                 │
│  - Animation/Transition support                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Core Layer                           │
│  - Navigator (Controller)                                │
│  - BackStack (State management)                          │
│  - Destination (Data model)                              │
│  - TypedDestination (Serializable destinations)          │
│  - NavigationGraph (Modular graphs)                      │
│  - DeepLink (URL handling)                               │
└─────────────────────────────────────────────────────────┘
```

## Core Concepts

### 1. Destination
A `Destination` represents where you want to navigate. It can be a simple object or a data class with typed arguments.

**Two Approaches:**

**Annotation-Based (Recommended):**
```kotlin
@Graph("feature")
sealed class FeatureDestination : Destination

@Route("feature/home")
data object Home : FeatureDestination()

@Serializable
data class DetailData(val itemId: String)

@Route("feature/detail")
@Argument(DetailData::class)
data class Detail(val itemId: String) 
    : FeatureDestination(), TypedDestination<DetailData> {
    override val data = DetailData(itemId)
}
```

**Manual DSL:**
```kotlin
sealed class FeatureDestination : Destination {
    object Home : FeatureDestination() {
        override val route = "feature/home"
    }
    data class Detail(val itemId: String) : FeatureDestination() {
        override val route = "feature/detail"
        override val arguments = mapOf("itemId" to itemId)
    }
}
```

**Benefits:**
- Type-safe navigation targets (both approaches)
- Serializable for deep links and state restoration
- Can carry typed data
- Annotation approach generates helpful extensions

### 2. Navigator
The `Navigator` is the central controller that manages all navigation operations.

**Responsibilities:**
- Execute navigation commands
- Manage the backstack
- Handle deep links
- Coordinate with navigation graphs
- Provide observable state

### 3. BackStack
Direct access to the navigation stack with reactive state.

**Features:**
- Observable via StateFlow
- Direct manipulation (push, pop, replace, clear)
- Advanced operations (popUntil, popToRoot)
- Supports complex navigation patterns

### 4. NavigationGraph
Enables modular architecture by allowing features to define their navigation independently.

**Gray Box Pattern:**
```
Feature Module A          Feature Module B
    ┌──────────┐              ┌──────────┐
    │  Screen1 │              │  Screen3 │
    │  Screen2 │              │  Screen4 │
    └──────────┘              └──────────┘
         │                         │
    Entry Points             Entry Points
         │                         │
         └──────────┬──────────────┘
                    │
              ┌─────▼─────┐
              │ Navigator │
              └───────────┘
```

Modules expose entry points but hide internal navigation details.

### 5. NavigationTransition
Declarative API for screen transitions.

**Supported:**
- Fade
- Horizontal/Vertical slides
- Scale animations
- Custom compositions
- Shared element transitions (framework ready)

### 6. DeepLink
URI-based navigation with pattern matching.

**Features:**
- Pattern matching with parameters
- Query parameter support
- Integration with navigation graphs
- Universal link support

### 7. Code Generation Layer (KSP)

The annotation-based API leverages Kotlin Symbol Processing (KSP) to generate navigation boilerplate automatically.

**Architecture:**
```
Source Code with Annotations
          ↓
    KSP Processor
          ↓
    ┌─────────────────┐
    │   GraphProcessor│
    └─────────────────┘
          ↓
    ┌─────────────────────────────────────┐
    │  Generators (3 types)               │
    ├─────────────────────────────────────┤
    │  1. RouteInitializerGenerator       │
    │     → {Graph}RouteInitializer       │
    │                                     │
    │  2. GraphBuilderGenerator           │
    │     → build{Graph}Graph()           │
    │                                     │
    │  3. DestinationExtensionGenerator   │
    │     → navigateTo{Destination}()     │
    └─────────────────────────────────────┘
          ↓
    Generated Kotlin Code
```

**What Gets Generated:**

**1. Route Initializers**
```kotlin
// Input
@Graph("feature")
sealed class FeatureDestination : Destination

@Route("feature/home")
data object Home : FeatureDestination()

// Generated
object FeatureDestinationRouteInitializer {
    init {
        FeatureDestination.Home.registerRoute("feature/home")
    }
}
```

**2. Graph Builders**
```kotlin
// Input
@Content(FeatureDestination.Home::class)
@Composable
fun HomeContent(navigator: Navigator) { /* ... */ }

// Generated
fun buildFeatureDestinationGraph(): NavigationGraph {
    return navigationGraph("feature") {
        startDestination(FeatureDestination.Home)
        
        destination(FeatureDestination.Home) { _, navigator ->
            HomeContent(navigator)
        }
    }
}
```

**3. Typed Navigation Extensions**
```kotlin
// Input
@Serializable
data class DetailData(val itemId: String, val mode: String = "view")

@Route("feature/detail")
@Argument(DetailData::class)
data class Detail(val itemId: String, val mode: String = "view") 
    : FeatureDestination(), TypedDestination<DetailData> {
    override val data = DetailData(itemId, mode)
}

// Generated
fun Navigator.navigateToDetail(
    itemId: String,
    mode: String = "view",
    transition: NavigationTransition? = null
) {
    val destination = FeatureDestination.Detail(
        itemId = itemId,
        mode = mode
    )
    if (transition != null) {
        navigate(destination, transition)
    } else {
        navigate(destination)
    }
}
```

**Benefits:**
- **Zero Boilerplate**: No manual route registration or graph building
- **Type Safety**: Compile-time verification of all navigation operations
- **IDE Support**: Generated code is navigable and autocompleted
- **Automatic Serialization**: kotlinx.serialization handles complex types
- **Maintainability**: Generated code is consistent and tested

**KSP Processing Flow:**
1. **Discovery Phase**: Find all `@Graph` annotated sealed classes
2. **Validation Phase**: Verify `@Route`, `@Argument`, `@Content` usage
3. **Analysis Phase**: Extract destination info, content functions, data types
4. **Generation Phase**: Generate route initializers, graph builders, extensions
5. **Output Phase**: Write generated Kotlin files to `build/generated/ksp/`

**Error Handling:**
KSP processor validates:
- `@Graph` only on sealed classes extending `Destination`
- `@Route` values are unique within a graph
- `@Argument` data classes are `@Serializable`
- `@Content` function signatures match destination types
- `TypedDestination<T>` properly implemented

Compile errors are reported with clear messages and source locations.

**Performance:**
- Incremental processing: Only reprocesses changed files
- Caching: Generated code cached until annotations change
- Build time impact: Typically <1 second for dozens of destinations

### 8. Predictive Back Navigation
Provides smooth, animated back gestures on both iOS and Android with automatic screen caching.

**Key Features:**
- **Gesture Tracking**: Real-time progress during user drag
- **Separate Animation Phases**: Gesture animation vs exit animation
- **Screen Caching**: Keeps screens alive during animations
- **Cache Locking**: Prevents premature destruction
- **Deferred Navigation**: Navigation happens after animation completes

**Animation Coordinator Pattern:**
```
User Gesture → Capture Entries → Lock Cache
     ↓                                ↓
Gesture Animation         Exit Animation (after release)
     ↓                                ↓
Previous Screen Rendered    Current Screen Animates Out
     ↓                                ↓
                      Navigation After Animation
                               ↓
                      Unlock Cache → Show New Screen
```

**Three Animation Types:**
1. **Material3**: Scale + translate + rounded corners + shadow
2. **Scale**: Simple scale down with fade
3. **Slide**: Slide right with fade

Each type has matching gesture and exit animations for consistency.

**Implementation:**
- `PredictiveBackAnimationCoordinator`: Manages animation state
- `ComposableCache`: Caches screens with locking mechanism
- Type-specific animations: `material3BackAnimation()`, `material3ExitAnimation()`, etc.

### 9. Hierarchical Rendering Architecture

The library supports two rendering modes for displaying navigation state. The **hierarchical mode** (recommended) provides better animation coordination and simpler mental model.

#### Rendering Modes

| Feature | Flattened (Deprecated) | Hierarchical (Recommended) |
|---------|------------------------|---------------------------|
| Tab/pane wrapper composition | Siblings (z-ordered) | True parent-child |
| Animation coordination | Per-surface | Per-container |
| Predictive back | Per-screen | Entire subtree |
| Wrapper definition | Runtime lambdas | `@TabWrapper`/`@PaneWrapper` annotations |
| Content definition | Runtime lambdas | `@Screen` annotations |

#### Hierarchical Rendering Pipeline

```
NavNode Tree (State)
       ↓
NavTreeRenderer (Recursive dispatch)
       ↓
┌──────────────────────────────────────────────────────────┐
│  Node-Specific Renderers                                 │
├──────────────────────────────────────────────────────────┤
│  ScreenRenderer    → Renders leaf destinations           │
│  StackRenderer     → AnimatedNavContent for transitions  │
│  TabRenderer       → @TabWrapper + AnimatedNavContent    │
│  PaneRenderer      → @PaneWrapper + adaptive layout      │
└──────────────────────────────────────────────────────────┘
       ↓
Composable Output with Proper Parent-Child Hierarchy
```

#### Key Components

**NavRenderScope**: Central context provided to all renderers
- `navigator`: Current Navigator instance
- `cache`: ComposableCache for state preservation
- `animationCoordinator`: Resolves transitions
- `predictiveBackController`: Manages gesture state
- `screenRegistry`: Maps destinations to composables
- `wrapperRegistry`: Maps nodes to wrapper composables

**AnimatedNavContent**: Custom AnimatedContent for navigation
- Tracks displayed/previous state for direction detection
- Switches to PredictiveBackContent during gestures
- Provides AnimatedVisibilityScope for shared elements

**ComposableCache**: Enhanced caching with locking
- `CachedEntry()`: Cacheable composable with SaveableStateHolder
- `lock()/unlock()`: Animation protection during transitions
- LRU eviction respecting locked entries

#### Wrapper Annotations

Tab and pane wrappers are defined via annotations:

```kotlin
@TabWrapper(tabClass = MainTabs::class)
@Composable
fun TabWrapperScope.MainTabWrapper(content: @Composable () -> Unit) {
    Scaffold(
        bottomBar = { /* NavigationBar */ }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()  // Library renders active tab here
        }
    }
}

@PaneWrapper(paneClass = ListDetailPane::class)
@Composable
fun PaneWrapperScope.ListDetailWrapper(content: @Composable () -> Unit) {
    if (isExpanded) {
        Row { /* Multi-pane layout */ }
    } else {
        content()  // Single pane
    }
}
```

#### Transition Annotations

Per-destination transitions via `@Transition`:

```kotlin
@Destination(route = "details")
@Transition(type = TransitionType.SlideHorizontal)
data class DetailsDestination(val id: String)
```

#### Migration

See [MIGRATION_HIERARCHICAL_RENDERING.md](MIGRATION_HIERARCHICAL_RENDERING.md) for complete migration guide from flattened to hierarchical rendering.

## Modularization Strategy

### Feature Module Structure

**Annotation-Based Approach (Recommended):**
```kotlin
// Feature module - destinations.kt
@Graph("shop")
sealed class ShopDestination : Destination

@Route("shop/list")
data object ProductList : ShopDestination()

@Route("shop/detail")
@Argument(ProductDetailData::class)
data class ProductDetail(val productId: String) 
    : ShopDestination(), TypedDestination<ProductDetailData> {
    override val data = ProductDetailData(productId)
}

// Feature module - content.kt
@Content(ProductList::class)
@Composable
fun ProductListContent(navigator: Navigator) { /* ... */ }

@Content(ProductDetail::class)
@Composable
fun ProductDetailContent(data: ProductDetailData, navigator: Navigator) { /* ... */ }

// Feature module - public API
object ShopFeature {
    fun navigationGraph() = buildShopDestinationGraph()
    val entryPoint: Destination = ShopDestination.ProductList
}

// Other modules navigate to feature
navigator.navigate(ShopFeature.entryPoint)
```

**Manual DSL Approach:**
```kotlin
// Feature module exposes this
class FeatureNavigation : BaseModuleNavigation() {
    override fun buildGraph() = navigationGraph("feature") {
        startDestination(InternalDestination1)
        
        destination(InternalDestination1) { _, nav -> Screen1(nav) }
        destination(InternalDestination2) { _, nav -> Screen2(nav) }
    }
    
    override fun entryPoints() = listOf(
        PublicEntryPoint // Only this is exposed
    )
}

// Other modules can navigate to the feature
fun navigateToFeature() {
    navigator.navigate(featureNavigation.entryPoints().first())
}
```

**Benefits:**
- Clear module boundaries
- Internal navigation stays private
- Easy to refactor within module
- Compile-time safety across modules
- Annotation approach reduces boilerplate significantly

## MVI Integration

The library provides first-class MVI support:

```kotlin
ViewModel → NavigationIntent → Navigator → BackStack → State
                                              ↓
                                      NavigationEffect
```

**Flow:**
1. User action triggers an Intent
2. ViewModel handles Intent, creates NavigationIntent
3. Navigator executes navigation
4. BackStack updates state
5. UI observes state changes
6. Side effects emit NavigationEffect

## State Management

All navigation state is reactive and observable:

- `Navigator.currentDestination: StateFlow<Destination?>`
- `BackStack.current: StateFlow<BackStackEntry?>`
- `BackStack.stack: StateFlow<List<BackStackEntry>>`
- `BackStack.canGoBack: StateFlow<Boolean>`

This enables:
- Reactive UI updates
- Easy testing
- State persistence
- Integration with any state management pattern

### BackStackEntry Extras

`BackStackEntry` supports arbitrary key-value extras for persisting component-level state that needs to survive recomposition and configuration changes.

**Property:**
```kotlin
val extras: MutableMap<String, Any?>
```

**Extension Functions:**
```kotlin
// Type-safe getter with default value
inline fun <reified T> BackStackEntry.getExtra(key: String, defaultValue: T): T

// Type-safe setter
inline fun <reified T> BackStackEntry.setExtra(key: String, value: T)
```

**Use Cases:**
- Preserving UI state (scroll position, selected items)
- Caching component state between recompositions
- Storing navigation-related metadata
- Tab selection persistence (see below)

**Example:**
```kotlin
// Store state in entry
entry.setExtra("scrollPosition", scrollState.value)
entry.setExtra("selectedItemId", selectedItem?.id)

// Restore state from entry
val savedScrollPosition = entry.getExtra("scrollPosition", 0)
val savedSelectedId = entry.getExtra<String?>("selectedItemId", null)
```

### Tab State Persistence

The library provides automatic tab selection persistence for tabbed navigation patterns. When using `rememberTabNavigator`, the selected tab is automatically preserved across recompositions and configuration changes.

**How It Works:**

1. **Storage**: Tab selection state is stored in the parent `BackStackEntry.extras`
2. **Restoration**: On recomposition, the previously selected tab is restored
3. **Sync**: Tab selection changes are automatically synced to the entry

**Constant:**
```kotlin
const val EXTRA_SELECTED_TAB_ROUTE = "quo_vadis_selected_tab_route"
```

**Integration Pattern:**
```kotlin
@Composable
fun TabbedScreen(
    navigator: Navigator,
    parentEntry: BackStackEntry
) {
    val tabState = rememberTabNavigator(
        config = TabNavigatorConfig(
            tabs = listOf(HomeTab, SearchTab, ProfileTab),
            defaultTab = HomeTab
        ),
        parentNavigator = navigator,
        parentEntry = parentEntry  // Entry for state persistence
    )
    
    TabScaffold(tabState = tabState) { tab ->
        when (tab) {
            HomeTab -> HomeContent()
            SearchTab -> SearchContent()
            ProfileTab -> ProfileContent()
        }
    }
}
```

**Behavior:**
- When user switches tabs, the selection is saved to `parentEntry.extras`
- When the composable is recreated (e.g., after predictive back gesture), the tab selection is restored
- When navigating away and back to the tabbed screen, the previously selected tab is shown

**Benefits:**
- Seamless user experience during configuration changes
- Consistent tab state during predictive back animations
- No manual state management required

## Testing Strategy

Use `FakeNavigator` for unit tests:

```kotlin
@Test
fun `navigate to details screen`() {
    val navigator = FakeNavigator()
    val viewModel = MyViewModel(navigator)
    
    viewModel.onItemClicked("123")
    
    assertTrue(navigator.verifyNavigateTo("details"))
    assertEquals(1, navigator.getNavigateCallCount("details"))
}
```

## Performance Considerations

1. **Lazy Graph Registration**: Graphs are created lazily
2. **State Flows**: Efficient reactive updates
3. **No Reflection**: All navigation is compile-time safe
4. **Minimal Allocations**: Reuses data structures where possible

## Extension Points

The library is designed to be extended:

1. **Custom Transitions**: Implement `NavigationTransition`
2. **Custom Serializers**: Implement `NavigationStateSerializer`
3. **Custom DeepLink Handlers**: Implement `DeepLinkHandler`
4. **Custom Destinations**: Extend `Destination` interface

## Platform Considerations

### Android
- Integrates with Activity/Fragment lifecycle
- Handles system back button
- Supports deep links from Intent

### iOS
- Works with UIKit integration layer
- Supports universal links
- Handles navigation bar

### Desktop
- Window-based navigation
- Keyboard shortcuts support

### Web
- Browser history integration
- URL-based routing
- Deep link support via URL

## Migration Path

### From Other Libraries

1. **From Compose Navigation**: Similar concepts, easier backstack control, annotation-based API optional
2. **From Voyager**: Similar screen-based approach, better modularization, code generation available
3. **From Custom Solution**: Gradual migration, can coexist with existing code

### From Manual DSL to Annotations

If you're already using Quo Vadis with manual DSL:

1. **Gradual Migration**: Both approaches work together in same project
2. **Per-Feature**: Migrate one feature module at a time
3. **Zero Breaking Changes**: Existing manual DSL code continues to work
4. **See MIGRATION.md**: Complete guide with examples and patterns

**Quick Comparison:**

| Aspect | Manual DSL | Annotation-Based |
|--------|-----------|------------------|
| Code Volume | More boilerplate | 50-70% less code |
| Setup Time | Slower | Faster |
| Control | Maximum | Standard patterns |
| Serialization | Manual | Automatic |
| Best For | Complex/dynamic | Most use cases |

## Best Practices

1. **Prefer Annotation-Based API**: Use for most features, reserve manual DSL for special cases
2. **Keep Destinations Simple**: Just data, no logic (both approaches)
3. **Use Sealed Classes**: For related destination groups (both approaches)
4. **One Graph Per Feature**: Clear module boundaries
5. **Use @Serializable**: For all typed destination data classes
6. **Test Navigation**: Use FakeNavigator extensively
7. **Handle Deep Links Early**: Setup in app initialization
8. **Use Transitions Sparingly**: Default fade is often enough
9. **Observe State Reactively**: Don't poll, use StateFlow
10. **Clear Backstack Judiciously**: Users expect back button to work
11. **Leverage Generated Code**: Use `navigateTo*()` extensions for type safety

## Future Enhancements

Potential additions:

- [ ] Result passing between screens
- [ ] Multi-window support
- [ ] Tab-based navigation helpers
- [ ] Bottom sheet destinations
- [ ] Dialog destinations
- [ ] Saved state handles
- [ ] Nested navigation graphs
- [ ] Conditional navigation
- [ ] Navigation analytics hooks
