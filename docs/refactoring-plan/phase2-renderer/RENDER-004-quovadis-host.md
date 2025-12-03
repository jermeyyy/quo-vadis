# RENDER-004: Build QuoVadisHost Composable

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-004 |
| **Task Name** | Build QuoVadisHost Composable |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 5-7 days |
| **Dependencies** | CORE-001, RENDER-001, RENDER-002, RENDER-003 |
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
    
    // Flatten the navigation tree
    val surfaces = remember(navState, transitionState) {
        flattener.flattenState(navState, transitionState)
    }
    
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
| RENDER-002 (TreeFlattener) | Hard | Must complete first |
| RENDER-003 (TransitionState) | Hard | Must complete first |

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
- [RENDER-002](./RENDER-002-flatten-algorithm.md) - TreeFlattener implementation
- [RENDER-003](./RENDER-003-transition-state.md) - TransitionState management
- [RENDER-005](./RENDER-005-predictive-back.md) - Predictive back integration
- [RENDER-006](./RENDER-006-animation-registry.md) - AnimationRegistry
- [RENDER-007](./RENDER-007-saveable-state.md) - SaveableStateHolder details
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-003](../phase1-core/CORE-003-navigator-refactor.md) - Navigator refactoring
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.2 "The Single Rendering Component"
