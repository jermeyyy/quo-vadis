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
│  - NavigationHost (Unified navigation rendering)         │
│  - NavNodeRenderer (Hierarchical node rendering)         │
│  - Animation/Transition support                          │
└─────────────────────────────────────────────────────────┘
                          ↓
┌─────────────────────────────────────────────────────────┐
│                     Core Layer                           │
│  - Navigator (Controller)                                │
│  - NavNode (Tree-based state: StackNode, TabNode, etc.) │
│  - TreeMutator (Immutable state transformations)         │
│  - Destination (Data model)                              │
│  - TypedDestination (Serializable destinations)          │
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
- Manage the NavNode tree state
- Handle deep links
- Provide observable state via `StateFlow<NavNode>`

**Key Properties:**
```kotlin
val state: StateFlow<NavNode>           // Current navigation tree
val currentDestination: StateFlow<Destination?>
val canNavigateBack: StateFlow<Boolean>
```

### 3. NavNode Tree
The navigation state is represented as an immutable tree of `NavNode` objects.

**Node Types:**
- `ScreenNode` - Leaf node representing a single destination
- `StackNode` - Container for a back-stack of screens
- `TabNode` - Container for tabbed navigation with multiple stacks
- `PaneNode` - Container for adaptive pane layouts (list-detail, etc.)

**Features:**
- Observable via `StateFlow<NavNode>`
- Immutable transformations via `TreeMutator`
- Supports complex hierarchical navigation patterns
- Serializable for state persistence

### 4. TreeMutator
Pure functional operations for transforming the NavNode tree.

**Key Operations:**
```kotlin
TreeMutator.push(root, destination)     // Add destination to active stack
TreeMutator.pop(root)                   // Remove top of active stack
TreeMutator.replaceCurrent(root, dest)  // Replace current destination
TreeMutator.clearAndPush(root, dest)    // Clear stack and push
TreeMutator.switchActiveTab(root, index) // Switch tab in TabNode
```

### 5. NavigationGraph
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

### 6. NavigationTransition
Declarative API for screen transitions.

**Supported:**
- Fade
- Horizontal/Vertical slides
- Scale animations
- Custom compositions
- Shared element transitions (framework ready)

### 7. DeepLink
URI-based navigation with pattern matching.

**Features:**
- Pattern matching with parameters
- Query parameter support
- Integration with navigation graphs
- Universal link support

### 8. Code Generation Layer (KSP)

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

### 9. Predictive Back Navigation
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

### 10. Hierarchical Rendering Architecture

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
ViewModel → NavigationIntent → Navigator → TreeMutator → NavNode Tree
                                                    ↓
                                            StateFlow<NavNode>
```

**Flow:**
1. User action triggers an Intent
2. ViewModel handles Intent, creates NavigationIntent
3. Navigator executes navigation via TreeMutator
4. NavNode tree updates immutably
5. UI observes state changes via StateFlow
6. Side effects emit NavigationEffect

## State Management

All navigation state is reactive and observable:

- `Navigator.state: StateFlow<NavNode>` - Complete navigation tree
- `Navigator.currentDestination: StateFlow<Destination?>` - Active destination
- `Navigator.canNavigateBack: StateFlow<Boolean>` - Back navigation availability

**NavNode Tree State:**
```kotlin
// Observe entire navigation tree
navigator.state.collect { navNode ->
    // React to any navigation change
}

// Observe current destination
navigator.currentDestination.collect { destination ->
    // React to destination changes
}
```

This enables:
- Reactive UI updates
- Easy testing
- State persistence via NavNode serialization
- Integration with any state management pattern

### State Persistence with NavNode

`NavNode` trees are fully serializable for state restoration:

```kotlin
// Serialize navigation state
val json = NavNodeSerializer.toJson(navigator.state.value)

// Restore navigation state
val restoredState = NavNodeSerializer.fromJson(json)
navigator.updateState(restoredState)
```

**Use Cases:**
- Process death restoration
- Deep link state restoration
- Navigation history persistence

### Tab State Persistence

Tab navigation state is automatically maintained within the `TabNode` structure. When using tabbed navigation, the active tab index and each tab's stack state is preserved.

**How It Works:**

1. **Storage**: Tab selection is part of the `TabNode.activeIndex` property
2. **Per-Tab Stacks**: Each tab maintains its own `StackNode` with independent history
3. **State Restoration**: Full NavNode tree serialization preserves all tab state

**TabNode Structure:**
```kotlin
val tabNode = TabNode(
    key = "main_tabs",
    children = listOf(homeStack, searchStack, profileStack),
    activeIndex = 0  // Currently selected tab
)
```

**Switching Tabs:**
```kotlin
// Via Navigator
navigator.switchTab(index = 1)

// Via TreeMutator (for direct state manipulation)
val newState = TreeMutator.switchActiveTab(currentState, newIndex = 1)
```

**Behavior:**
- Each tab maintains independent back history
- Switching tabs preserves each tab's stack state
- Deep links can target specific tabs
- Full tree serialization for state restoration

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
