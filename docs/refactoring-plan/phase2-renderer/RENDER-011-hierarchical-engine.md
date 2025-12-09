# RENDER-011: Hierarchical Rendering Engine Architecture

**Status**: Proposed  
**Created**: 2025-12-09  
**Supersedes**: RENDER-001 (RenderableSurface), RENDER-002A-C (Flattening approaches)

## Executive Summary

This document proposes a fundamental redesign of the navigation rendering engine. Instead of flattening the `NavNode` tree into a flat list of `RenderableSurface` instances, we propose **hierarchical rendering** that preserves the composable parent-child relationships defined by the navigation structure.

The new approach enables:
- Proper wrapper/content composition (tab wrappers contain tab content)
- Coordinated animations at the appropriate tree level
- Predictive back gestures that transform entire subtrees as units
- Single `QuoVadisHost` entry point for all navigation
- Shared element transitions across NavNode boundaries
- Full KSP generation of composable bindings and wrappers

---

## Problem Statement

### Current Architecture Issues

The `RenderableSurface` flattening approach creates an **impedance mismatch** between the logical navigation structure and the physical Compose rendering tree.

#### The Flattening Problem

When a `TabNode` is flattened:

```
TabNode (tabs)
├── wrapper: @Composable { scaffold, bottom nav }
├── StackNode (home-stack)
└── StackNode (profile-stack)  ← active

Flattens to TWO SEPARATE surfaces:
1. RenderableSurface(id="tabs-wrapper", mode=TAB_WRAPPER)
2. RenderableSurface(id="tabs-content-1", mode=TAB_CONTENT)
```

These surfaces become **siblings** in the Compose tree, not parent-child:

```
QuoVadisHost
└── Box
    ├── RenderableSurfaceContainer (wrapper) ← z-index: 0
    │   └── AnimatedVisibility
    │       └── Scaffold + BottomNav
    │
    └── RenderableSurfaceContainer (content) ← z-index: 100
        └── AnimatedVisibility
            └── ProfileScreen
```

#### Consequences

| Issue | Impact |
|-------|--------|
| **Animation Desync** | Wrapper and content animate independently with separate `AnimatedVisibility` |
| **Predictive Back Breaks** | Wrapper and content transform separately during gesture |
| **Caching Fragmentation** | Each surface has its own `SaveableScreen` - can't cache wrapper+content as unit |
| **Slot Semantics Lost** | Content should be *inside* wrapper, not *beside* it |

### Evidence: Layout Inspector

The attached Layout Inspector screenshot shows:
- Multiple `RenderableSurfaceContainer` instances as siblings
- Separate `SaveableStateProvider` per surface
- Independent `AnimatedVisibility` wrappers
- The `NavigationBar` (bottom nav) in one container, content in another

---

## Proposed Solution: Hierarchical Rendering

### Core Concept

**Render the `NavNode` tree as a composable hierarchy**, where the Compose tree mirrors the navigation structure:

```kotlin
QuoVadisHost(navigator)
└── SharedTransitionLayout                    // Shared element scope
    └── AnimatedNavContent(root)              // Root animation boundary
        └── TabWrapper                        // User's scaffold with bottom nav
            └── AnimatedNavContent(activeTab) // Tab content animation boundary
                └── StackContent              // Active screen in tab
                    └── HomeScreen            // @Screen composable
```

**Key principle**: Each `NavNode` type knows how to render itself and its children, maintaining proper composition.

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           QuoVadisHost                                  │
│  ┌───────────────────────────────────────────────────────────────────┐  │
│  │                    SharedTransitionLayout                         │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │                   ComposableCache                           │  │  │
│  │  │  (LRU cache of NavNode keys → cached composable states)     │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │              NavTreeRenderer (recursive)                    │  │  │
│  │  │                                                             │  │  │
│  │  │   renderNode(NavNode) → when(node) {                        │  │  │
│  │  │       ScreenNode → renderScreen()                           │  │  │
│  │  │       StackNode  → renderStack()                            │  │  │
│  │  │       TabNode    → renderTab()                              │  │  │
│  │  │       PaneNode   → renderPane()                             │  │  │
│  │  │   }                                                         │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │           AnimationCoordinator                              │  │  │
│  │  │  (manages transitions between NavNode states)               │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  │                                                                   │  │
│  │  ┌─────────────────────────────────────────────────────────────┐  │  │
│  │  │          PredictiveBackController                           │  │  │
│  │  │  (gesture handling, progress tracking, transform state)     │  │  │
│  │  └─────────────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Component Design

### 1. NavTreeRenderer

The core component that recursively renders the navigation tree.

```kotlin
@Composable
internal fun NavTreeRenderer(
    node: NavNode,
    previousNode: NavNode?,  // For animation pairing
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

#### NavRenderScope

Provides context to all renderers:

```kotlin
@Stable
interface NavRenderScope {
    val navigator: Navigator
    val cache: ComposableCache
    val animationCoordinator: AnimationCoordinator
    val predictiveBackController: PredictiveBackController
    val sharedTransitionScope: SharedTransitionScope
    val screenRegistry: ScreenRegistry
    val wrapperRegistry: WrapperRegistry  // NEW: KSP-generated wrappers
    
    // Animation visibility scope for current render context
    @Composable
    fun withAnimatedVisibilityScope(
        visibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    )
}
```

### 2. ScreenRenderer

Renders leaf `ScreenNode` destinations:

```kotlin
@Composable
internal fun ScreenRenderer(
    node: ScreenNode,
    scope: NavRenderScope,
    modifier: Modifier
) {
    // Cache entry management
    scope.cache.CachedEntry(
        key = node.key,
        content = {
            CompositionLocalProvider(
                LocalBackStackEntry provides node.toBackStackEntry()
            ) {
                scope.screenRegistry.Content(
                    destination = node.destination,
                    navigator = scope.navigator,
                    sharedTransitionScope = scope.sharedTransitionScope,
                    animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                )
            }
        }
    )
}
```

### 3. StackRenderer

Renders `StackNode` with animated transitions between children:

```kotlin
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    val activeChild = node.children.lastOrNull() ?: return
    val previousActiveChild = previousNode?.children?.lastOrNull()
    
    // Determine animation direction
    val isBackNavigation = detectBackNavigation(node, previousNode)
    val transition = scope.animationCoordinator.getTransition(
        from = previousActiveChild,
        to = activeChild,
        isBack = isBackNavigation
    )
    
    // Animated content switching
    AnimatedNavContent(
        targetState = activeChild,
        transition = transition,
        scope = scope,
        predictiveBackEnabled = node.parentKey == null, // Only root stack
        modifier = modifier
    ) { child ->
        NavTreeRenderer(
            node = child,
            previousNode = previousActiveChild,
            scope = scope
        )
    }
}
```

### 4. TabRenderer

Renders `TabNode` with wrapper containing tab content:

```kotlin
@Composable
internal fun TabRenderer(
    node: TabNode,
    previousNode: TabNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    val activeStack = node.stacks.getOrNull(node.activeStackIndex)
    val previousActiveStack = previousNode?.stacks?.getOrNull(previousNode.activeStackIndex)
    
    // Tab wrapper scope for user's wrapper composable
    val tabWrapperScope = rememberTabWrapperScope(
        navigator = scope.navigator,
        tabNode = node
    )
    
    // Cache the ENTIRE tab node (wrapper + content)
    scope.cache.CachedEntry(
        key = node.key,
        content = {
            // KSP-generated wrapper invocation
            scope.wrapperRegistry.TabWrapper(
                wrapperScope = tabWrapperScope,
                tabNodeKey = node.key
            ) { // Content slot
                if (activeStack != null) {
                    // Animate between tabs (within the wrapper)
                    AnimatedNavContent(
                        targetState = activeStack,
                        transition = scope.animationCoordinator.getTabTransition(
                            fromIndex = previousNode?.activeStackIndex,
                            toIndex = node.activeStackIndex
                        ),
                        scope = scope,
                        predictiveBackEnabled = false, // Tab switching not via predictive back
                        modifier = Modifier
                    ) { stack ->
                        NavTreeRenderer(
                            node = stack,
                            previousNode = previousActiveStack,
                            scope = scope
                        )
                    }
                }
            }
        }
    )
}
```

**Critical difference from RenderableSurface approach**: The wrapper invocation **contains** the tab content as a slot. They are parent-child in the Compose tree, not siblings.

### 5. PaneRenderer

Renders `PaneNode` with adaptive layout wrapper:

```kotlin
@Composable
internal fun PaneRenderer(
    node: PaneNode,
    previousNode: PaneNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    val windowSizeClass = calculateWindowSizeClass()
    val isExpanded = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Expanded
    
    // Cache entire pane configuration
    scope.cache.CachedEntry(
        key = node.key,
        content = {
            if (isExpanded) {
                // Multi-pane: render wrapper with multiple content slots
                MultiPaneRenderer(node, scope, modifier)
            } else {
                // Single-pane: behave like a stack
                SinglePaneRenderer(node, previousNode, scope, modifier)
            }
        }
    )
}
```

---

## Animation System

### AnimatedNavContent

A custom `AnimatedContent` variant optimized for navigation:

```kotlin
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    // Track displayed state for animation
    var displayedState by remember { mutableStateOf(targetState) }
    var previousState by remember { mutableStateOf<T?>(null) }
    
    // Handle predictive back
    if (predictiveBackEnabled && scope.predictiveBackController.isActive) {
        PredictiveBackContent(
            current = displayedState,
            previous = previousState,
            progress = scope.predictiveBackController.progress,
            scope = scope,
            content = content
        )
    } else {
        // Standard AnimatedContent transition
        AnimatedContent(
            targetState = targetState,
            transitionSpec = {
                transition.createTransitionSpec(
                    isBack = targetState.key != displayedState.key && 
                            previousState?.key == targetState.key
                )
            },
            modifier = modifier
        ) { animatingState ->
            // Update tracking
            if (animatingState == targetState) {
                previousState = displayedState
                displayedState = targetState
            }
            
            // Provide animation scope and render
            scope.withAnimatedVisibilityScope(this) {
                content(animatingState)
            }
        }
    }
}
```

### NavTransition

Defines enter/exit animations:

```kotlin
@Immutable
data class NavTransition(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val popEnter: EnterTransition,
    val popExit: ExitTransition
) {
    fun createTransitionSpec(isBack: Boolean): ContentTransform {
        return if (isBack) {
            popEnter togetherWith popExit
        } else {
            enter togetherWith exit
        }
    }
    
    companion object {
        val SlideHorizontal = NavTransition(
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { -it / 3 },
            popEnter = slideInHorizontally { -it / 3 },
            popExit = slideOutHorizontally { it }
        )
        
        val Fade = NavTransition(
            enter = fadeIn(),
            exit = fadeOut(),
            popEnter = fadeIn(),
            popExit = fadeOut()
        )
        
        val None = NavTransition(
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            popEnter = EnterTransition.None,
            popExit = ExitTransition.None
        )
    }
}
```

### AnimationCoordinator

Manages transition resolution based on destination annotations:

```kotlin
@Stable
class AnimationCoordinator(
    private val transitionRegistry: TransitionRegistry,
    private val defaultTransition: NavTransition = NavTransition.SlideHorizontal
) {
    fun getTransition(
        from: NavNode?,
        to: NavNode,
        isBack: Boolean
    ): NavTransition {
        // Check @Transition annotation on destination
        val toDestination = (to as? ScreenNode)?.destination
        val annotatedTransition = toDestination?.let { 
            transitionRegistry.getTransition(it::class)
        }
        
        return annotatedTransition ?: defaultTransition
    }
    
    fun getTabTransition(
        fromIndex: Int?,
        toIndex: Int
    ): NavTransition {
        if (fromIndex == null) return NavTransition.None
        
        // Determine direction for horizontal slide
        return if (toIndex > fromIndex) {
            NavTransition.SlideHorizontal
        } else {
            NavTransition.SlideHorizontal.reversed()
        }
    }
}
```

---

## Predictive Back Handling

### PredictiveBackController

Centralized gesture handling:

```kotlin
@Stable
class PredictiveBackController {
    var isActive by mutableStateOf(false)
        private set
    
    var progress by mutableFloatStateOf(0f)
        private set
    
    private var onComplete: (() -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    
    suspend fun handleGesture(
        backEvent: Flow<BackEventCompat>,
        onNavigateBack: () -> Unit
    ) {
        isActive = true
        try {
            backEvent.collect { event ->
                progress = event.progress.coerceAtMost(PROGRESS_CLAMP)
            }
            // Gesture completed - animate to finish
            animateToCompletion()
            onNavigateBack()
        } catch (e: CancellationException) {
            // Gesture cancelled - animate back
            animateToCancel()
        } finally {
            isActive = false
            progress = 0f
        }
    }
    
    private suspend fun animateToCompletion() {
        animate(
            initialValue = progress,
            targetValue = 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium)
        ) { value, _ ->
            progress = value
        }
    }
    
    private suspend fun animateToCancel() {
        animate(
            initialValue = progress,
            targetValue = 0f,
            animationSpec = spring(stiffness = Spring.StiffnessLow)
        ) { value, _ ->
            progress = value
        }
    }
    
    companion object {
        private const val PROGRESS_CLAMP = 0.25f
    }
}
```

### PredictiveBackContent

Renders current and previous content with transforms:

```kotlin
@Composable
internal fun <T : NavNode> PredictiveBackContent(
    current: T,
    previous: T?,
    progress: Float,
    scope: NavRenderScope,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Previous (incoming) content - static behind current
        if (previous != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Slight parallax effect
                        translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
                    }
            ) {
                scope.cache.CachedEntry(key = previous.key) {
                    // Render previous with fake AnimatedVisibilityScope
                    StaticAnimatedVisibilityScope {
                        content(previous)
                    }
                }
            }
        }
        
        // Current (exiting) content - transforms based on gesture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = size.width * progress
                    scaleX = 1f - (progress * SCALE_FACTOR)
                    scaleY = 1f - (progress * SCALE_FACTOR)
                }
        ) {
            scope.cache.CachedEntry(key = current.key) {
                StaticAnimatedVisibilityScope {
                    content(current)
                }
            }
        }
    }
}

private const val PARALLAX_FACTOR = 0.3f
private const val SCALE_FACTOR = 0.1f
```

**Important**: When predictive back is active on a `TabNode`, the **entire** tab (wrapper + content) transforms as a single unit because they're in the same cached composable tree.

---

## Shared Element Transitions

### Design

Shared elements require:
1. A `SharedTransitionLayout` at the root
2. `SharedTransitionScope` available to all screen composables
3. `AnimatedVisibilityScope` for each animating screen

```kotlin
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    // ... other params
) {
    SharedTransitionLayout {
        val scope = rememberNavRenderScope(
            navigator = navigator,
            sharedTransitionScope = this,
            // ...
        )
        
        NavTreeRenderer(
            node = navigator.state.value,
            previousNode = navigator.previousState,
            scope = scope
        )
    }
}
```

### Screen Access

Screen composables access shared transition scopes via:

```kotlin
@Screen(DetailDestination::class)
@Composable
fun DetailScreen(
    destination: DetailDestination,
    navigator: Navigator,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    with(sharedTransitionScope) {
        Image(
            // ...
            modifier = Modifier.sharedElement(
                state = rememberSharedContentState("image-${destination.id}"),
                animatedVisibilityScope = animatedVisibilityScope
            )
        )
    }
}
```

### Cross-NavNode Shared Elements

Because `SharedTransitionLayout` is at the root, shared elements work across:
- Stack screens within the same stack
- Screens in different tabs
- Screens across navigation levels

The `sharedElement` modifier tracks elements by key, regardless of their position in the NavNode tree.

---

## KSP Generation: WrapperRegistry

### New Annotations

```kotlin
/**
 * Marks a composable function as a tab wrapper.
 * @param tabClass The @Tab annotated class this wrapper is for
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TabWrapper(
    val tabClass: KClass<*>
)

/**
 * Marks a composable function as a pane wrapper.
 * @param paneClass The @Pane annotated class this wrapper is for
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class PaneWrapper(
    val paneClass: KClass<*>
)
```

### Usage Example

```kotlin
@Tab(items = [HomeTab::class, ExploreTab::class, ProfileTab::class])
sealed class MainTabs

@TabWrapper(MainTabs::class)
@Composable
fun MainTabsWrapper(
    scope: TabWrapperScope,
    tabContent: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                scope.tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = scope.activeIndex == index,
                        onClick = { scope.switchTab(index) },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            tabContent()  // Content slot
        }
    }
}
```

### Generated WrapperRegistry

```kotlin
// Generated: GeneratedWrapperRegistry.kt
object GeneratedWrapperRegistry : WrapperRegistry {
    
    @Composable
    override fun TabWrapper(
        wrapperScope: TabWrapperScope,
        tabNodeKey: String,
        content: @Composable () -> Unit
    ) {
        when {
            tabNodeKey.contains("mainTabs") -> {
                MainTabsWrapper(
                    scope = wrapperScope,
                    tabContent = content
                )
            }
            else -> {
                // Default wrapper: just render content
                content()
            }
        }
    }
    
    @Composable
    override fun PaneWrapper(
        wrapperScope: PaneWrapperScope,
        paneNodeKey: String,
        content: @Composable PaneContentScope.() -> Unit
    ) {
        when {
            tabNodeKey.contains("masterDetail") -> {
                MasterDetailWrapper(
                    scope = wrapperScope,
                    paneContent = content
                )
            }
            else -> {
                // Default: side by side
                DefaultPaneWrapper(wrapperScope, content)
            }
        }
    }
}
```

---

## Caching Strategy

### ComposableCache

Enhanced caching that preserves entire NavNode subtrees:

```kotlin
@Stable
class ComposableCache(
    private val maxSize: Int = DEFAULT_MAX_SIZE
) {
    // LRU cache with access time tracking
    private val entries = mutableStateMapOf<String, CacheEntry>()
    private val accessOrder = mutableStateListOf<String>()
    private val lockedEntries = mutableStateSetOf<String>()
    
    @Composable
    fun CachedEntry(
        key: String,
        content: @Composable () -> Unit
    ) {
        // Track access
        DisposableEffect(key) {
            recordAccess(key)
            onDispose { }
        }
        
        // SaveableStateProvider preserves state across recomposition
        SaveableStateProvider(key) {
            content()
        }
    }
    
    fun lock(key: String) {
        lockedEntries.add(key)
    }
    
    fun unlock(key: String) {
        lockedEntries.remove(key)
        evictIfNeeded()
    }
    
    private fun recordAccess(key: String) {
        accessOrder.remove(key)
        accessOrder.add(key)
        evictIfNeeded()
    }
    
    private fun evictIfNeeded() {
        while (accessOrder.size > maxSize) {
            val oldest = accessOrder.firstOrNull { it !in lockedEntries }
            if (oldest != null) {
                accessOrder.remove(oldest)
                entries.remove(oldest)
            } else {
                break // All entries are locked
            }
        }
    }
    
    companion object {
        private const val DEFAULT_MAX_SIZE = 10
    }
}
```

### Cache Key Strategy

| NavNode Type | Cache Key Pattern | Cached Content |
|--------------|-------------------|----------------|
| ScreenNode | `"{parentKey}/screen/{route}"` | Screen composable + state |
| StackNode | `"{parentKey}/stack"` | Animated content container |
| TabNode | `"{parentKey}/tabs/{tabType}"` | Wrapper + all tab content |
| PaneNode | `"{parentKey}/panes/{paneType}"` | Wrapper + all pane content |

**Important**: TabNode and PaneNode cache the **entire** subtree, not individual parts. This ensures wrapper and content are always cached together.

---

## Migration Path

### Phase 1: Parallel Implementation

1. Create new components alongside existing:
   - `NavTreeRenderer` (new) coexists with `TreeFlattener` (existing)
   - `HierarchicalQuoVadisHost` (new) coexists with `QuoVadisHost` (existing)

2. Feature flag for switching:
   ```kotlin
   QuoVadisHost(
       navigator = navigator,
       renderingMode = RenderingMode.Hierarchical // or .Flattened
   )
   ```

### Phase 2: Annotation Updates

1. Add `@TabWrapper` and `@PaneWrapper` annotations
2. Update KSP processor to generate `WrapperRegistry`
3. Deprecate runtime wrapper parameters

### Phase 3: Transition

1. Update documentation with new patterns
2. Mark flattened rendering as deprecated
3. Provide migration guide for existing wrapper code

### Phase 4: Removal

1. Remove `RenderableSurface` and `TreeFlattener`
2. Remove deprecated APIs
3. Clean up dead code

---

## Comparison: Flattened vs. Hierarchical

| Aspect | Flattened (Current) | Hierarchical (Proposed) |
|--------|---------------------|-------------------------|
| **Compose Tree** | Flat list of surfaces | Mirrors NavNode tree |
| **Wrapper/Content** | Siblings | Parent-child |
| **Animation Coordination** | Per-surface | Per-NavNode level |
| **Predictive Back** | Transforms surfaces independently | Transforms entire subtrees |
| **Caching** | Per-surface fragments | Per-NavNode subtrees |
| **Shared Elements** | Requires coordination | Natural via root scope |
| **Wrapper Definition** | Runtime lambdas | KSP-generated bindings |
| **Complexity** | Higher (flattening logic) | Lower (recursive rendering) |

---

## Open Questions

1. **Default Wrappers**: Should we provide default wrappers for common patterns (tabs with bottom nav, panes with rail)?

2. **Animation Customization**: How granular should per-destination transition annotations be?

3. **State Restoration**: Should NavNode state be serializable for process death handling?

4. **Testing**: How do we provide `FakeNavRenderScope` for composable tests?

---

## Appendix: Full QuoVadisHost Implementation Sketch

```kotlin
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = GeneratedScreenRegistry,
    wrapperRegistry: WrapperRegistry = GeneratedWrapperRegistry,
    transitionRegistry: TransitionRegistry = GeneratedTransitionRegistry,
    defaultTransition: NavTransition = NavTransition.SlideHorizontal
) {
    val currentState by navigator.state.collectAsState()
    var previousState by remember { mutableStateOf<NavNode?>(null) }
    
    // Update previous state tracking
    LaunchedEffect(currentState) {
        // Small delay to keep previous for animation
        delay(16)
        previousState = currentState
    }
    
    // Create render scope
    val cache = remember { ComposableCache() }
    val animationCoordinator = remember {
        AnimationCoordinator(transitionRegistry, defaultTransition)
    }
    val predictiveBackController = remember { PredictiveBackController() }
    
    // Handle predictive back gesture
    val canGoBack = remember(currentState) {
        navigator.canNavigateBack()
    }
    
    PredictiveBackHandler(enabled = canGoBack) { backEvent ->
        predictiveBackController.handleGesture(backEvent) {
            navigator.navigateBack()
        }
    }
    
    // Render with shared transition support
    SharedTransitionLayout(modifier = modifier) {
        val scope = NavRenderScopeImpl(
            navigator = navigator,
            cache = cache,
            animationCoordinator = animationCoordinator,
            predictiveBackController = predictiveBackController,
            sharedTransitionScope = this,
            screenRegistry = screenRegistry,
            wrapperRegistry = wrapperRegistry
        )
        
        CompositionLocalProvider(
            LocalNavRenderScope provides scope
        ) {
            NavTreeRenderer(
                node = currentState,
                previousNode = previousState,
                scope = scope
            )
        }
    }
}
```

---

## References

- [RENDER-001-renderable-surface.md](RENDER-001-renderable-surface.md) - Original surface design (superseded)
- [RENDER-002A-core-flatten.md](RENDER-002A-core-flatten.md) - Flattening approach (superseded)
- [RENDER-005-predictive-back.md](RENDER-005-predictive-back.md) - Predictive back requirements (preserved)
- [Compose Navigation 3](https://developer.android.com/develop/ui/compose/navigation/navigation-3) - Reference architecture
