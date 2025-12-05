# RENDER-004: Build QuoVadisHost Composable

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-004 |
| **Task Name** | Build QuoVadisHost Composable |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 6-8 days |
| **Dependencies** | CORE-001, RENDER-001, RENDER-002A, RENDER-002B, RENDER-002C, RENDER-003, RENDER-008, RENDER-009 |
| **Blocked By** | CORE-001, RENDER-001, RENDER-002, RENDER-003 |
| **Blocks** | RENDER-005, RENDER-007, RENDER-008 |

---

## Overview

`QuoVadisHost` is the **Single Rendering Component** at the heart of the new architecture. It replaces all existing navigation hosts (`NavHost`, `GraphNavHost`, `TabbedNavHost`) with a unified renderer capable of:

- Rendering arbitrary NavNode tree structures
- Coordinating enter/exit animations
- Supporting shared element transitions
- Preserving tab state via SaveableStateHolder
- Handling predictive back gestures

### Architecture Position

```
Navigator (StateFlow<NavNode>) ────► QuoVadisHost ────► Compose UI
         │                                │
         │ TransitionState                │ SharedTransitionScope
         ▼                                │
    TreeFlattener ────► List<RenderableSurface>
                                          │
                        AnimatedContent ◄─┘
                        (per surface)
```

### Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single source of truth** | Observes Navigator's StateFlow |
| **Composition over inheritance** | Small composables compose into host |
| **Animation coordination** | Central AnimationRegistry |
| **State preservation** | SaveableStateHolder for tabs |
| **Shared elements** | SharedTransitionLayout at root |

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt
```

---

## Implementation

### Core QuoVadisHost Composable

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import com.jermey.quo.vadis.core.navigation.core.*
import kotlinx.coroutines.flow.StateFlow

/**
 * The unified navigation host that renders any NavNode tree structure.
 * 
 * QuoVadisHost is the single rendering component that replaces all previous
 * navigation hosts (NavHost, GraphNavHost, TabbedNavHost). It:
 * 
 * 1. Observes the Navigator's state flow
 * 2. Flattens the NavNode tree into renderable surfaces
 * 3. Renders each surface with appropriate z-ordering
 * 4. Coordinates enter/exit animations
 * 5. Provides SharedTransitionScope for shared element transitions
 * 6. Preserves tab state via SaveableStateHolder
 * 
 * ## Basic Usage
 * 
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberNavigator(initialGraph)
 *     
 *     QuoVadisHost(
 *         navigator = navigator,
 *         modifier = Modifier.fillMaxSize()
 *     ) { destination ->
 *         // Resolve destination to composable
 *         when (destination) {
 *             is HomeDestination -> HomeScreen()
 *             is ProfileDestination -> ProfileScreen(destination.userId)
 *         }
 *     }
 * }
 * ```
 * 
 * ## With Shared Elements
 * 
 * ```kotlin
 * QuoVadisHost(navigator = navigator) { destination ->
 *     // sharedTransitionScope is available in this lambda
 *     ProfileScreen(
 *         sharedTransitionScope = this,
 *         animatedVisibilityScope = animatedContentScope
 *     )
 * }
 * ```
 * 
 * @param navigator The Navigator instance managing navigation state
 * @param modifier Modifier for the root container
 * @param animationRegistry Registry for custom animations (optional)
 * @param content Content resolver that maps Destination to Composable
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    enablePredictiveBack: Boolean = true,
    
    // NEW: User provides their tab wrapper composable
    tabWrapper: @Composable (
        tabNode: TabNode,
        activeTabContent: @Composable () -> Unit
    ) -> Unit = { _, content -> content() },
    
    // NEW: User provides their pane wrapper composable (for large screens)
    paneWrapper: @Composable (
        paneNode: PaneNode,
        paneContents: List<PaneContent>
    ) -> Unit = { _, contents -> DefaultPaneLayout(contents) },
    
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    // Collect navigation state
    val navState by navigator.stateFlow.collectAsState()
    val transitionState by navigator.transitionStateFlow.collectAsState()
    
    // State holder for preserving tab states
    val saveableStateHolder = rememberSaveableStateHolder()
    
    // Create tree flattener with content resolver
    val contentResolver = remember(content) {
        TreeFlattener.ContentResolver { node ->
            @Composable {
                if (node is ScreenNode) {
                    content(node.destination)
                }
            }
        }
    }
    
    val flattener = remember(contentResolver, animationRegistry) {
        TreeFlattener(
            contentResolver = contentResolver,
            animationResolver = animationRegistry.toAnimationResolver()
        )
    }
    
    // WindowSizeClass integration for adaptive layouts
    val windowSizeClass = calculateWindowSizeClass()
    
    // Flatten the navigation tree with window size awareness
    val flattenResult = remember(navState, transitionState, windowSizeClass) {
        flattener.flattenState(navState, transitionState, windowSizeClass)
    }
    
    val surfaces = flattenResult.surfaces
    
    // Differentiated caching strategy based on navigation type
    val cachingStrategy = determineCachingStrategy(
        flattenResult.cachingHints,
        transitionState
    )
    
    // Cache whole wrapper for cross-node-type navigation
    // Cache only content for intra-tab/pane navigation
    
    // Root container with SharedTransitionLayout
    SharedTransitionLayout(modifier = modifier) {
        QuoVadisHostContent(
            surfaces = surfaces,
            transitionState = transitionState,
            saveableStateHolder = saveableStateHolder,
            sharedTransitionScope = this,
            animationRegistry = animationRegistry
        )
    }
}

/**
 * Internal composable that renders the flattened surfaces.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun QuoVadisHostContent(
    surfaces: List<RenderableSurface>,
    transitionState: TransitionState,
    saveableStateHolder: SaveableStateHolder,
    sharedTransitionScope: SharedTransitionScope,
    animationRegistry: AnimationRegistry
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Render each surface sorted by z-order
        surfaces
            .sortedBy { it.zOrder }
            .filter { it.shouldRender }
            .forEach { surface ->
                key(surface.id) {
                    RenderableSurfaceContainer(
                        surface = surface,
                        transitionState = transitionState,
                        saveableStateHolder = saveableStateHolder,
                        sharedTransitionScope = sharedTransitionScope,
                        animationRegistry = animationRegistry
                    )
                }
            }
    }
}

/**
 * Container for rendering a single surface with animations.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun RenderableSurfaceContainer(
    surface: RenderableSurface,
    transitionState: TransitionState,
    saveableStateHolder: SaveableStateHolder,
    sharedTransitionScope: SharedTransitionScope,
    animationRegistry: AnimationRegistry
) {
    // Determine visibility for animation
    val isVisible = surface.transitionState !is SurfaceTransitionState.Exiting ||
                   (surface.transitionState as? SurfaceTransitionState.Exiting)?.progress?.let { it < 1f } ?: true
    
    // Apply z-index for proper layering
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(surface.zOrder.toFloat())
    ) {
        // Wrap in SaveableStateProvider for state preservation
        saveableStateHolder.SaveableStateProvider(key = surface.id) {
            // Animate enter/exit
            AnimatedVisibility(
                visible = isVisible,
                enter = surface.animationSpec.enter,
                exit = surface.animationSpec.exit,
                modifier = Modifier.fillMaxSize()
            ) {
                // Apply predictive back transformations if applicable
                val contentModifier = if (surface.isPredictive) {
                    Modifier.predictiveBackTransform(
                        progress = surface.animationProgress ?: 0f,
                        isExiting = surface.transitionState is SurfaceTransitionState.Exiting
                    )
                } else {
                    Modifier
                }
                
                Box(modifier = contentModifier.fillMaxSize()) {
                    surface.content()
                }
            }
        }
    }
}

/**
 * Modifier for predictive back gesture transformations.
 */
private fun Modifier.predictiveBackTransform(
    progress: Float,
    isExiting: Boolean
): Modifier {
    return if (isExiting) {
        // Exiting surface: scale down and shift
        this
            .graphicsLayer {
                val scale = 1f - (progress * 0.1f) // Scale from 1.0 to 0.9
                scaleX = scale
                scaleY = scale
                
                // Slight parallax shift
                translationX = progress * size.width * 0.1f
            }
    } else {
        // Entering surface (below): scale up from 0.9
        this
            .graphicsLayer {
                val scale = 0.9f + (progress * 0.1f) // Scale from 0.9 to 1.0
                scaleX = scale
                scaleY = scale
            }
    }
}
```

### QuoVadisHostScope

```kotlin
/**
 * Scope provided to content lambda in QuoVadisHost.
 * 
 * Provides access to:
 * - SharedTransitionScope for shared element transitions
 * - AnimatedContentScope for coordinated animations
 * - Navigation actions
 */
@OptIn(ExperimentalSharedTransitionApi::class)
interface QuoVadisHostScope : SharedTransitionScope {
    /**
     * The current AnimatedContentScope for coordinating animations.
     */
    val animatedContentScope: AnimatedContentScope
    
    /**
     * Navigator instance for programmatic navigation.
     */
    val navigator: Navigator
}

/**
 * Implementation of QuoVadisHostScope.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private class QuoVadisHostScopeImpl(
    private val sharedTransitionScope: SharedTransitionScope,
    override val animatedContentScope: AnimatedContentScope,
    override val navigator: Navigator
) : QuoVadisHostScope, SharedTransitionScope by sharedTransitionScope
```

### PaneContent Data Class

```kotlin
/**
 * Represents content for a single pane in a multi-pane layout.
 * 
 * Used by the paneWrapper parameter to provide structured access
 * to individual pane content with type information.
 * 
 * @param paneType The role/type of this pane (e.g., LIST, DETAIL)
 * @param content The composable content for this pane
 */
data class PaneContent(
    val paneType: PaneRole,
    val content: @Composable () -> Unit
)

/**
 * Default pane layout used when no custom paneWrapper is provided.
 */
@Composable
fun DefaultPaneLayout(contents: List<PaneContent>) {
    Row(modifier = Modifier.fillMaxSize()) {
        contents.forEach { pane ->
            Box(
                modifier = Modifier.weight(
                    when (pane.paneType) {
                        PaneRole.LIST -> 0.3f
                        PaneRole.DETAIL -> 0.7f
                        else -> 1f / contents.size
                    }
                )
            ) {
                pane.content()
            }
        }
    }
}
```

### Alternative API with Content Slot

```kotlin
/**
 * QuoVadisHost variant that uses a content map instead of a lambda.
 * 
 * Useful when destinations are known at compile time and you want
 * to avoid recomposition when the content lambda changes.
 * 
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     contentMap = mapOf(
 *         HomeDestination::class to { HomeScreen() },
 *         ProfileDestination::class to { dest -> ProfileScreen(dest.userId) }
 *     )
 * )
 * ```
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <D : Destination> QuoVadisHost(
    navigator: Navigator,
    contentMap: Map<KClass<out D>, @Composable QuoVadisHostScope.(D) -> Unit>,
    modifier: Modifier = Modifier,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    fallback: @Composable QuoVadisHostScope.(Destination) -> Unit = { 
        error("No content registered for ${it::class.simpleName}")
    }
) {
    QuoVadisHost(
        navigator = navigator,
        modifier = modifier,
        animationRegistry = animationRegistry
    ) { destination ->
        @Suppress("UNCHECKED_CAST")
        val contentProvider = contentMap[destination::class] as? (@Composable QuoVadisHostScope.(D) -> Unit)
        if (contentProvider != null) {
            @Suppress("UNCHECKED_CAST")
            contentProvider(destination as D)
        } else {
            fallback(destination)
        }
    }
}
```

### Graph-Based API

```kotlin
/**
 * QuoVadisHost variant that uses a NavigationGraph for content resolution.
 * 
 * This is the most type-safe approach, using KSP-generated graphs.
 * 
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     graph = AppNavGraph // KSP-generated
 * )
 * ```
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default
) {
    QuoVadisHost(
        navigator = navigator,
        modifier = modifier,
        animationRegistry = animationRegistry
    ) { destination ->
        graph.resolveContent(destination, this)
    }
}
```

---

## Integration Points

### With Navigator

```kotlin
@Composable
fun rememberQuoVadisNavigator(
    initialState: NavNode
): Navigator {
    return remember {
        Navigator(initialState)
    }
}

// Usage
val navigator = rememberQuoVadisNavigator(
    initialState = StackNode(
        key = "root",
        parentKey = null,
        children = listOf(
            ScreenNode("home", "root", HomeDestination)
        )
    )
)

QuoVadisHost(navigator = navigator) { destination ->
    // ...
}
```

### With Shared Element Transitions

```kotlin
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    onPhotoClick: (Photo) -> Unit,
    modifier: Modifier = Modifier
) {
    // Inside QuoVadisHost, sharedTransitionScope is available
    LazyVerticalGrid(columns = GridCells.Fixed(3)) {
        items(photos) { photo ->
            Image(
                painter = rememberAsyncImagePainter(photo.url),
                contentDescription = null,
                modifier = Modifier
                    .sharedElement(
                        state = rememberSharedContentState(key = "photo-${photo.id}"),
                        animatedVisibilityScope = animatedContentScope
                    )
                    .clickable { onPhotoClick(photo) }
            )
        }
    }
}
```

### With AnimationRegistry

```kotlin
// Register custom animations
val customRegistry = AnimationRegistry.Default.copy {
    register(
        from = HomeDestination::class,
        to = ProfileDestination::class,
        direction = TransitionDirection.FORWARD,
        spec = SurfaceAnimationSpec(
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 1.1f)
        )
    )
}

QuoVadisHost(
    navigator = navigator,
    animationRegistry = customRegistry
) { destination ->
    // ...
}
```

---

## User Wrapper Rendering

QuoVadisHost supports user-provided wrapper composables for TabNode and PaneNode rendering, allowing complete customization of tab bars, navigation rails, and multi-pane layouts.

### TabNode Rendering

When rendering a `TabNode`, the wrapper surface invokes the user's `tabWrapper` with:
- `tabNode`: The current TabNode being rendered
- `activeTabContent`: A slot composable containing the library-managed tab content (stack navigation, animations, etc.)

```kotlin
// Internal rendering flow
@Composable
internal fun RenderTabNode(
    tabNode: TabNode,
    tabWrapper: @Composable (TabNode, @Composable () -> Unit) -> Unit,
    content: @Composable (Destination) -> Unit
) {
    tabWrapper(tabNode) {
        // Library renders the active tab stack here
        RenderStack(
            stackNode = tabNode.children[tabNode.activeStackIndex],
            content = content
        )
    }
}
```

The user's wrapper receives full control over the surrounding UI while the library manages tab content internally.

### PaneNode Rendering (Large Screens)

On large screens (determined by `WindowSizeClass`), `PaneNode` renders using the user's `paneWrapper`:
- `paneNode`: The current PaneNode being rendered
- `paneContents`: A list of `PaneContent` objects, each containing the pane type and content slot

```kotlin
// Internal rendering flow for large screens
@Composable
internal fun RenderPaneNodeLargeScreen(
    paneNode: PaneNode,
    paneWrapper: @Composable (PaneNode, List<PaneContent>) -> Unit,
    content: @Composable (Destination) -> Unit
) {
    val paneContents = paneNode.panes.map { pane ->
        PaneContent(
            paneType = pane.role,
            content = { RenderPaneContent(pane, content) }
        )
    }
    
    paneWrapper(paneNode, paneContents)
}
```

### PaneNode Rendering (Small Screens)

On small screens, `PaneNode` adapts to render as a `StackNode` - the `paneWrapper` is **not** invoked:

```kotlin
// Internal rendering flow for small screens
@Composable
internal fun RenderPaneNodeSmallScreen(
    paneNode: PaneNode,
    content: @Composable (Destination) -> Unit
) {
    // Render only the active pane as a stack
    val activePane = paneNode.getActivePaneForSmallScreen()
    RenderStack(
        stackNode = activePane.asStackNode(),
        content = content
    )
}
```

This adaptive behavior is automatic based on `WindowSizeClass` - users don't need to handle responsiveness manually.

---

## Render Loop Details

### Step-by-Step Rendering

1. **Collect State**: Observe `Navigator.stateFlow` and `transitionStateFlow`
2. **Flatten Tree**: Call `TreeFlattener.flattenState(navState, transitionState)`
3. **Sort by Z-Order**: Ensure proper layering
4. **Filter Renderable**: Skip `Hidden` surfaces
5. **Render Each Surface**:
   - Apply `zIndex` modifier
   - Wrap in `SaveableStateProvider` (for state preservation)
   - Wrap in `AnimatedVisibility` (for enter/exit)
   - Apply predictive back transforms (if applicable)
   - Render content

### Composition Lifecycle

```
QuoVadisHost
├── SharedTransitionLayout           // Enables shared elements
│   └── Box (root container)
│       ├── Surface 1 (zIndex=100)
│       │   └── SaveableStateProvider
│       │       └── AnimatedVisibility
│       │           └── Content
│       └── Surface 2 (zIndex=200)
│           └── SaveableStateProvider
│               └── AnimatedVisibility
│                   └── Content
```

---

## Implementation Steps

### Step 1: Core Structure

1. Create `QuoVadisHost.kt` file
2. Define `QuoVadisHostScope` interface
3. Implement basic `QuoVadisHost` composable
4. Add state collection and tree flattening

### Step 2: Rendering Pipeline

1. Implement `QuoVadisHostContent` composable
2. Implement `RenderableSurfaceContainer` composable
3. Add z-index handling
4. Integrate `SaveableStateHolder`

### Step 3: Animation Support

1. Wire up `AnimatedVisibility` for enter/exit
2. Add predictive back transform modifier
3. Integrate `AnimationRegistry`
4. Handle transition state updates

### Step 4: Shared Elements

1. Add `SharedTransitionLayout` wrapper
2. Expose `SharedTransitionScope` in scope
3. Test shared element transitions

### Step 5: Alternative APIs

1. Implement content map variant
2. Implement graph-based variant
3. Add documentation for each approach

### Step 6: Testing

1. Unit tests for rendering logic
2. Compose UI tests for animations
3. Integration tests with full navigation

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/QuoVadisHost.kt` | Create | Main implementation |
| `quo-vadis-core/.../compose/QuoVadisHostScope.kt` | Create | Scope interface (optional, can be in same file) |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| CORE-001 (NavNode Hierarchy) | Hard | Must complete first |
| RENDER-001 (RenderableSurface) | Hard | Must complete first |
| RENDER-002A (TreeFlattener Core) | Hard | Must complete first |
| RENDER-002B (Stack/Screen Flattening) | Hard | Must complete first |
| RENDER-002C (Tab/Pane Flattening) | Hard | Must complete first |
| RENDER-003 (TransitionState) | Hard | Must complete first |
| RENDER-008 (WindowSizeClass) | Hard | Must complete first |
| RENDER-009 (Caching Strategy) | Hard | Must complete first |

---

## Acceptance Criteria

- [ ] `QuoVadisHost` composable renders NavNode tree correctly
- [ ] State observation via `collectAsState()` works
- [ ] Tree flattening produces correct surface list
- [ ] Z-index ordering ensures proper layering
- [ ] `SaveableStateHolder` preserves tab states
- [ ] `AnimatedVisibility` handles enter/exit transitions
- [ ] Predictive back transforms applied correctly
- [ ] `SharedTransitionLayout` enables shared elements
- [ ] `QuoVadisHostScope` exposes necessary context
- [ ] Content map variant works
- [ ] Graph-based variant works
- [ ] Performance is acceptable (no unnecessary recompositions)
- [ ] Comprehensive KDoc documentation
- [ ] Compose UI tests pass
- [ ] `tabWrapper` parameter accepted and invoked
- [ ] `paneWrapper` parameter accepted and invoked
- [ ] `PaneContent` data class defined with paneType and content
- [ ] WindowSizeClass observed for PaneNode adaptation
- [ ] Differentiated caching based on navigation type
- [ ] Cross-node navigation caches whole wrapper
- [ ] Intra-tab navigation caches only content

---

## Usage Examples

### Basic Usage with Wrapper APIs

```kotlin
@Composable
fun MyApp() {
    val navigator = rememberNavigator(initialGraph)
    
    QuoVadisHost(
        navigator = navigator,
        tabWrapper = { tabNode, tabContent ->
            Scaffold(
                bottomBar = { MyBottomNavigation(tabNode.activeStackIndex) }
            ) {
                tabContent() // Library's tab content
            }
        },
        paneWrapper = { paneNode, paneContents ->
            Row {
                paneContents.forEach { pane ->
                    Box(modifier = Modifier.weight(if (pane.paneType == PaneRole.LIST) 0.3f else 0.7f)) {
                        pane.content()
                    }
                }
            }
        }
    ) { destination ->
        // Screen content resolution
        when (destination) {
            is HomeDestination -> HomeScreen()
            is ProfileDestination -> ProfileScreen(destination.userId)
            is SettingsDestination -> SettingsScreen()
        }
    }
}
```

### Custom Tab Navigation with Material 3

```kotlin
@Composable
fun MaterialTabsApp() {
    val navigator = rememberNavigator(initialGraph)
    
    QuoVadisHost(
        navigator = navigator,
        tabWrapper = { tabNode, tabContent ->
            val tabs = listOf("Home", "Search", "Profile")
            
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        tabs.forEachIndexed { index, title ->
                            NavigationBarItem(
                                selected = tabNode.activeStackIndex == index,
                                onClick = { navigator.switchTab(index) },
                                icon = { Icon(getIconForTab(index), contentDescription = title) },
                                label = { Text(title) }
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    tabContent()
                }
            }
        }
    ) { destination -> /* screen content */ }
}
```

### Adaptive List-Detail Layout

```kotlin
@Composable
fun AdaptiveListDetailApp() {
    val navigator = rememberNavigator(initialGraph)
    
    QuoVadisHost(
        navigator = navigator,
        paneWrapper = { paneNode, paneContents ->
            // Custom responsive layout
            Row(modifier = Modifier.fillMaxSize()) {
                // List pane with fixed width
                paneContents
                    .find { it.paneType == PaneRole.LIST }
                    ?.let { listPane ->
                        Surface(
                            modifier = Modifier.width(320.dp).fillMaxHeight(),
                            tonalElevation = 1.dp
                        ) {
                            listPane.content()
                        }
                    }
                
                // Divider
                VerticalDivider()
                
                // Detail pane fills remaining space
                paneContents
                    .find { it.paneType == PaneRole.DETAIL }
                    ?.let { detailPane ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            detailPane.content()
                        }
                    }
            }
        }
    ) { destination -> /* screen content */ }
}
```

### Navigation Rail for Large Screens

```kotlin
@Composable
fun NavigationRailApp() {
    val navigator = rememberNavigator(initialGraph)
    val windowSizeClass = calculateWindowSizeClass()
    
    QuoVadisHost(
        navigator = navigator,
        tabWrapper = { tabNode, tabContent ->
            if (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded) {
                // Large screen: use navigation rail
                Row {
                    NavigationRail {
                        NavigationRailItem(
                            selected = tabNode.activeStackIndex == 0,
                            onClick = { navigator.switchTab(0) },
                            icon = { Icon(Icons.Default.Home, "Home") }
                        )
                        NavigationRailItem(
                            selected = tabNode.activeStackIndex == 1,
                            onClick = { navigator.switchTab(1) },
                            icon = { Icon(Icons.Default.Settings, "Settings") }
                        )
                    }
                    tabContent()
                }
            } else {
                // Small screen: use bottom navigation
                Scaffold(
                    bottomBar = { /* BottomNavigation */ }
                ) {
                    tabContent()
                }
            }
        }
    ) { destination -> /* screen content */ }
}
```

---

## Performance Considerations

### Avoiding Recomposition

```kotlin
// BAD: Content lambda captured in remember causes recomposition
QuoVadisHost(navigator) { dest ->  // Lambda changes on every recomposition
    ScreenContent(dest)
}

// GOOD: Use stable content map
val contentMap = remember {
    mapOf(
        HomeDestination::class to @Composable { HomeScreen() },
        // ...
    )
}
QuoVadisHost(navigator, contentMap = contentMap)
```

### Memoizing Flattened Surfaces

```kotlin
// Surfaces only recalculated when state changes
val surfaces = remember(navState, transitionState) {
    flattener.flattenState(navState, transitionState)
}
```

### Key-Based Composition

```kotlin
// Using `key` ensures surfaces are reused, not recreated
surfaces.forEach { surface ->
    key(surface.id) {  // Surface identity preserved
        RenderableSurfaceContainer(surface)
    }
}
```

---

## Testing Notes

### Compose UI Tests

```kotlin
@Test
fun `QuoVadisHost renders active screen`() {
    val navigator = Navigator(
        StackNode("root", null, listOf(
            ScreenNode("home", "root", HomeDestination)
        ))
    )
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { dest ->
            when (dest) {
                is HomeDestination -> Text("Home Screen")
            }
        }
    }
    
    composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()
}

@Test
fun `QuoVadisHost animates on push`() {
    val navigator = Navigator(/* initial state */)
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { /* ... */ }
    }
    
    // Trigger navigation
    navigator.push(ProfileDestination)
    
    // Verify animation occurred
    composeTestRule.mainClock.advanceTimeBy(150) // Half animation
    // Assert intermediate state...
    
    composeTestRule.mainClock.advanceTimeBy(150) // Complete animation
    // Assert final state...
}

@Test
fun `QuoVadisHost preserves tab state`() {
    val navigator = Navigator(/* TabNode with two stacks */)
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { /* ... */ }
    }
    
    // Navigate in tab 0
    navigator.push(DetailDestination)
    
    // Switch to tab 1
    navigator.switchTab(1)
    
    // Switch back to tab 0
    navigator.switchTab(0)
    
    // Tab 0 should still show DetailDestination
    composeTestRule.onNodeWithText("Detail").assertIsDisplayed()
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-001](./RENDER-001-renderable-surface.md) - RenderableSurface definition
- [RENDER-002A](./RENDER-002A-core-flatten.md) - Core TreeFlattener implementation
- [RENDER-002B](./RENDER-002B-tab-flattening.md) - TabNode flattening
- [RENDER-002C](./RENDER-002C-pane-flattening.md) - PaneNode flattening
- [RENDER-003](./RENDER-003-transition-state.md) - TransitionState management
- [RENDER-005](./RENDER-005-predictive-back.md) - Predictive back integration
- [RENDER-006](./RENDER-006-animation-registry.md) - AnimationRegistry
- [RENDER-007](./RENDER-007-saveable-state.md) - SaveableStateHolder details
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-003](../phase1-core/CORE-003-navigator-refactor.md) - Navigator refactoring
- [Original Architecture Plan](../../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.2 "The Single Rendering Component"
