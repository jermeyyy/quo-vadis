# RENDER-007: SaveableStateHolder Integration

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-007 |
| **Task Name** | SaveableStateHolder Integration |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 3-4 days |
| **Dependencies** | RENDER-004 |
| **Blocked By** | RENDER-004 |
| **Blocks** | - |

---

## Overview

`SaveableStateHolder` is the Compose mechanism for preserving state across composition lifecycle changes. In the context of QuoVadisHost, it's essential for:

1. **Tab state preservation** - When switching tabs, inactive tab's state should survive
2. **Back stack restoration** - Screens on the back stack maintain their state
3. **Process death survival** - State persists across activity recreation
4. **Differentiated caching** - Different navigation contexts require different caching strategies

### Caching Requirements by Node Type

| Node Type | Cross-Node Navigation | Intra-Node Navigation |
|-----------|----------------------|----------------------|
| **TabNode** | Cache whole wrapper (scaffold, app bar, bottom nav) | Cache only tab content |
| **PaneNode** | Cache whole wrapper (multi-pane layout) | Cache only pane content |
| **StackNode** | N/A | Standard screen caching |
| **ScreenNode** | N/A | Full screen caching |

### Why Differentiated Caching Matters

- **Wrapper stability**: User's wrapper composable (Scaffold with BottomNavigation) shouldn't be recreated during tab switches
- **Content independence**: Each tab's content state is preserved individually
- **Performance**: Avoiding unnecessary recomposition of stable wrapper elements
- **User experience**: Smooth transitions without visual glitches in navigation chrome

### The Problem

Without SaveableStateHolder:

```kotlin
// Tab 0 is active
HomeScreen() // User scrolls to position 100

// User switches to Tab 1
ProfileScreen()

// User switches back to Tab 0
HomeScreen() // Scroll position LOST! Back to 0
```

With SaveableStateHolder:

```kotlin
// Tab 0 is active
SaveableStateProvider(key = "tab-0") {
    HomeScreen() // User scrolls to position 100
}

// User switches to Tab 1 - Tab 0 state saved
SaveableStateProvider(key = "tab-1") {
    ProfileScreen()
}

// User switches back to Tab 0 - State restored
SaveableStateProvider(key = "tab-0") {
    HomeScreen() // Scroll position 100 restored!
}
```

### Design Goals

| Goal | Approach |
|------|----------|
| **Automatic state preservation** | Wrap all screens in SaveableStateProvider |
| **Unique key generation** | Use NavNode.key for identification |
| **Cleanup on removal** | Remove saved state when screen is popped |
| **Process death survival** | Integrate with rememberSaveable |
| **Differentiated caching** | Different cache scopes for different navigation contexts |

---

## Differentiated Caching Strategy

Not all navigation scenarios require the same caching approach. The caching strategy must differentiate between:

1. **Cross-node-type navigation** - Navigating between different node types (e.g., switching tabs)
2. **Intra-node navigation** - Navigation within the same node (e.g., pushing screen within a tab's stack)

### Cache Scope Types

```kotlin
/**
 * Defines the scope of state caching for navigation surfaces.
 */
enum class CacheScope {
    /**
     * Normal screen caching for ScreenNode and StackNode.
     * The entire screen composable is cached.
     */
    FULL_SCREEN,
    
    /**
     * Cache entire wrapper for TabNode/PaneNode during cross-node navigation.
     * Preserves scaffold, app bar, bottom navigation, etc.
     */
    WHOLE_WRAPPER,
    
    /**
     * Cache only content, not wrapper, for intra-tab/pane navigation.
     * Wrapper remains stable while content changes.
     */
    CONTENT_ONLY
}
```

### Caching Decision Logic

```kotlin
class NavigationStateHolder(/*...*/) {
    
    /**
     * Determines the appropriate cache scope based on navigation context.
     *
     * @param transition Current transition state
     * @param surfaceId Unique identifier for the surface
     * @param surfaceMode The rendering mode of the surface
     * @return The appropriate CacheScope for this navigation context
     */
    fun determineCacheScope(
        transition: TransitionState,
        surfaceId: String,
        surfaceMode: SurfaceRenderingMode
    ): CacheScope {
        return when {
            // Cross-node-type navigation: cache whole wrapper
            transition.isCrossNodeTypeNavigation() && 
            surfaceMode in listOf(SurfaceRenderingMode.TAB_WRAPPER, SurfaceRenderingMode.PANE_WRAPPER) -> 
                CacheScope.WHOLE_WRAPPER
            
            // Intra-tab navigation: cache only content
            !transition.isCrossNodeTypeNavigation() &&
            surfaceMode == SurfaceRenderingMode.TAB_CONTENT ->
                CacheScope.CONTENT_ONLY
            
            // Intra-pane navigation: cache only pane content
            !transition.isCrossNodeTypeNavigation() &&
            surfaceMode == SurfaceRenderingMode.PANE_CONTENT ->
                CacheScope.CONTENT_ONLY
            
            // Default screen caching
            else -> CacheScope.FULL_SCREEN
        }
    }
    
    /**
     * Applies the appropriate caching strategy based on scope.
     */
    @Composable
    fun SaveableWithScope(
        key: String,
        scope: CacheScope,
        wrapperContent: @Composable (() -> Unit) -> Unit = { it() },
        content: @Composable () -> Unit
    ) {
        when (scope) {
            CacheScope.FULL_SCREEN -> {
                SaveableScreen(key = key) {
                    wrapperContent { content() }
                }
            }
            CacheScope.WHOLE_WRAPPER -> {
                SaveableScreen(key = "wrapper-$key") {
                    wrapperContent {
                        content()
                    }
                }
            }
            CacheScope.CONTENT_ONLY -> {
                // Wrapper is NOT wrapped in SaveableStateProvider
                // Only content is cached
                wrapperContent {
                    SaveableScreen(key = "content-$key") {
                        content()
                    }
                }
            }
        }
    }
}
```

### Decision Flow Diagram

```
                    ┌─────────────────┐
                    │ Navigation Event│
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │ Is cross-node   │
                    │ type navigation?│
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │ YES                         │ NO
              │                             │
    ┌─────────▼─────────┐         ┌─────────▼─────────┐
    │ Is TAB_WRAPPER or │         │ Is TAB_CONTENT or │
    │ PANE_WRAPPER?     │         │ PANE_CONTENT?     │
    └─────────┬─────────┘         └─────────┬─────────┘
              │                             │
       ┌──────┴──────┐               ┌──────┴──────┐
       │YES       NO │               │YES       NO │
       │             │               │             │
       ▼             ▼               ▼             ▼
  WHOLE_WRAPPER  FULL_SCREEN   CONTENT_ONLY  FULL_SCREEN
```

---

## Wrapper vs Content State Management

The differentiated caching strategy enables separate management of wrapper and content state.

### Wrapper State Preservation

Wrapper state includes:
- **Scaffold state** (drawer open/closed, snackbar queue)
- **App bar state** (expanded/collapsed, search mode)
- **Bottom navigation state** (selected index, badge counts)
- **FAB state** (visibility, extended state)

```kotlin
/**
 * Wrapper state is preserved across ALL tab switches.
 * This composable is cached with WHOLE_WRAPPER scope.
 */
@Composable
fun TabbedAppWrapper(
    tabNode: TabNode,
    stateHolder: NavigationStateHolder,
    content: @Composable () -> Unit
) {
    // This state survives tab switches because wrapper is cached
    val scaffoldState = rememberScaffoldState()
    var selectedTab by rememberSaveable { mutableStateOf(tabNode.activeIndex) }
    
    Scaffold(
        scaffoldState = scaffoldState,
        bottomBar = {
            BottomNavigation {
                tabNode.stacks.forEachIndexed { index, stack ->
                    BottomNavigationItem(
                        selected = index == selectedTab,
                        onClick = { selectedTab = index },
                        icon = { /* ... */ },
                        label = { /* ... */ }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}
```

### Content State Preservation

Each tab's content maintains its own state independently:

```kotlin
/**
 * Tab content state is preserved per-tab.
 * This composable is cached with CONTENT_ONLY scope.
 */
@Composable
fun TabContent(
    stackNode: StackNode,
    stateHolder: NavigationStateHolder
) {
    // Each tab has its own scroll position
    val scrollState = rememberLazyListState()
    
    // Each tab has its own filter state
    var filterQuery by rememberSaveable { mutableStateOf("") }
    
    Column {
        SearchBar(
            query = filterQuery,
            onQueryChange = { filterQuery = it }
        )
        
        LazyColumn(state = scrollState) {
            // Content specific to this tab
        }
    }
}
```

### Multi-Pane State Management

For PaneNode in multi-pane mode, the same principle applies:

```kotlin
/**
 * Pane wrapper manages the multi-pane layout.
 * Cached with WHOLE_WRAPPER scope during cross-node navigation.
 */
@Composable
fun MultiPaneWrapper(
    paneNode: PaneNode,
    stateHolder: NavigationStateHolder,
    content: @Composable () -> Unit
) {
    // Pane layout state survives pane switches
    var paneWeights by rememberSaveable {
        mutableStateOf(listOf(0.3f, 0.7f))
    }
    
    Row {
        paneNode.panes.forEachIndexed { index, pane ->
            Box(
                modifier = Modifier
                    .weight(paneWeights[index])
                    .fillMaxHeight()
            ) {
                // Individual pane content
            }
        }
    }
}
```

### State Isolation Table

| State Type | Scope | Survives Tab Switch | Survives Intra-Tab Nav | Survives Process Death |
|------------|-------|---------------------|------------------------|------------------------|
| Scaffold state | Wrapper | ✅ | ✅ | ✅ (with rememberSaveable) |
| Bottom nav selection | Wrapper | ✅ | ✅ | ✅ |
| Tab scroll position | Content | ✅ | ✅ | ✅ (with rememberSaveable) |
| Tab filter query | Content | ✅ | ✅ | ✅ |
| Screen-specific state | Screen | N/A | ✅ | ✅ |

---

## File Locations

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt (updates)
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavigationStateHolder.kt (new)
```

---

## Implementation

### Enhanced QuoVadisHost with SaveableStateHolder

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * Updated QuoVadisHost with comprehensive state preservation.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    enablePredictiveBack: Boolean = true,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    val navState by navigator.stateFlow.collectAsState()
    val transitionState by navigator.transitionStateFlow.collectAsState()
    
    // Core state holder for preserving screen states
    val saveableStateHolder = rememberSaveableStateHolder()
    
    // Track which keys are currently in the tree for cleanup
    val activeKeys = remember(navState) {
        collectAllKeys(navState)
    }
    
    // Previous keys for detecting removals
    var previousKeys by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    // Clean up state for removed screens
    LaunchedEffect(activeKeys) {
        val removedKeys = previousKeys - activeKeys
        removedKeys.forEach { key ->
            saveableStateHolder.removeState(key)
        }
        previousKeys = activeKeys
    }
    
    // ... rest of QuoVadisHost implementation using saveableStateHolder ...
}

/**
 * Collects all keys from a NavNode tree.
 */
private fun collectAllKeys(node: NavNode): Set<String> {
    val keys = mutableSetOf<String>()
    collectKeysRecursive(node, keys)
    return keys
}

private fun collectKeysRecursive(node: NavNode, keys: MutableSet<String>) {
    keys.add(node.key)
    when (node) {
        is ScreenNode -> { /* Leaf node, already added */ }
        is StackNode -> node.children.forEach { collectKeysRecursive(it, keys) }
        is TabNode -> node.stacks.forEach { collectKeysRecursive(it, keys) }
        is PaneNode -> node.panes.forEach { collectKeysRecursive(it, keys) }
    }
}
```

### NavigationStateHolder Wrapper

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * Manages state preservation for navigation screens.
 * 
 * NavigationStateHolder wraps SaveableStateHolder with navigation-specific
 * logic for:
 * - Automatic key management based on NavNode.key
 * - State cleanup when screens are removed
 * - Tab state preservation across tab switches
 * 
 * ## Usage
 * 
 * ```kotlin
 * val stateHolder = rememberNavigationStateHolder()
 * 
 * surfaces.forEach { surface ->
 *     stateHolder.SaveableScreen(key = surface.id) {
 *         surface.content()
 *     }
 * }
 * ```
 */
class NavigationStateHolder internal constructor(
    private val saveableStateHolder: SaveableStateHolder
) {
    private val retainedKeys = mutableSetOf<String>()
    
    /**
     * Provides saveable state for a screen.
     * 
     * @param key Unique identifier for this screen (typically NavNode.key)
     * @param content The screen content
     */
    @Composable
    fun SaveableScreen(
        key: String,
        content: @Composable () -> Unit
    ) {
        saveableStateHolder.SaveableStateProvider(key = key) {
            content()
        }
    }
    
    /**
     * Marks a key as retained (should not be cleaned up even if not rendered).
     * 
     * Useful for tabs - we want to retain all tab states even when only
     * the active tab is rendered.
     */
    fun retain(key: String) {
        retainedKeys.add(key)
    }
    
    /**
     * Marks a key as no longer retained.
     */
    fun release(key: String) {
        retainedKeys.remove(key)
    }
    
    /**
     * Removes saved state for the given key.
     */
    fun removeState(key: String) {
        if (key !in retainedKeys) {
            saveableStateHolder.removeState(key)
        }
    }
    
    /**
     * Updates the set of active keys and cleans up removed ones.
     * 
     * @param activeKeys Set of keys currently in the navigation tree
     * @param previousKeys Set of keys from the previous state
     */
    fun cleanup(activeKeys: Set<String>, previousKeys: Set<String>) {
        val removedKeys = previousKeys - activeKeys - retainedKeys
        removedKeys.forEach { key ->
            saveableStateHolder.removeState(key)
        }
    }
}

/**
 * Creates and remembers a NavigationStateHolder.
 */
@Composable
fun rememberNavigationStateHolder(): NavigationStateHolder {
    val saveableStateHolder = rememberSaveableStateHolder()
    return remember { NavigationStateHolder(saveableStateHolder) }
}
```

### Tab-Specific State Preservation

```kotlin
/**
 * Extension for preserving all tab states.
 * 
 * This ensures that inactive tabs maintain their state even when
 * not being rendered.
 */
@Composable
fun NavigationStateHolder.PreserveTabStates(
    tabNode: TabNode,
    content: @Composable () -> Unit
) {
    // Retain all tab stack keys
    DisposableEffect(tabNode.key) {
        tabNode.stacks.forEach { stack ->
            retain(stack.key)
        }
        
        onDispose {
            tabNode.stacks.forEach { stack ->
                release(stack.key)
            }
        }
    }
    
    content()
}
```

### Surface Container with State Preservation

```kotlin
/**
 * Updated RenderableSurfaceContainer with state preservation.
 */
@Composable
private fun RenderableSurfaceContainer(
    surface: RenderableSurface,
    stateHolder: NavigationStateHolder,
    transitionState: TransitionState,
    sharedTransitionScope: SharedTransitionScope,
    animationRegistry: AnimationRegistry
) {
    val isVisible = surface.transitionState !is SurfaceTransitionState.Hidden
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(surface.zOrder.toFloat())
    ) {
        // Use NavigationStateHolder for state preservation
        stateHolder.SaveableScreen(key = surface.id) {
            AnimatedVisibility(
                visible = isVisible,
                enter = surface.animationSpec.enter,
                exit = surface.animationSpec.exit,
                modifier = Modifier.fillMaxSize()
            ) {
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
```

### Integration with rememberSaveable

For data that needs to survive process death:

```kotlin
/**
 * Example of using rememberSaveable in a screen.
 */
@Composable
fun HomeScreen() {
    // This state survives:
    // 1. Tab switches
    // 2. Configuration changes
    // 3. Process death (if serializable)
    var scrollPosition by rememberSaveable { mutableStateOf(0) }
    
    LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = scrollPosition)
    ) {
        items(items) { item ->
            ItemCard(item)
        }
    }
}
```

### State Restoration Flow

```
Process Death Scenario:

1. User at HomeScreen (scroll position 50)
   └── SaveableStateProvider("home-screen")
       └── rememberSaveable { scrollPosition = 50 }

2. Process is killed by system
   └── State saved to Bundle

3. Process recreated
   └── Bundle restored

4. QuoVadisHost recomposes
   └── SaveableStateProvider("home-screen")
       └── rememberSaveable restores { scrollPosition = 50 }

5. HomeScreen shows with scroll position 50 ✓
```

---

## Key Management Strategy

### Key Format

Keys should be:
- **Unique** within the entire navigation tree
- **Stable** across recompositions
- **Predictable** for debugging

Recommended format: NavNode.key (which is typically UUID-based)

```kotlin
// Good: Using NavNode.key directly
stateHolder.SaveableScreen(key = surface.id) { /* ... */ }

// Good: For debugging, use structured keys
val key = "tab-${tabIndex}/stack-${stackIndex}/screen-${screenIndex}"

// Bad: Non-stable keys
val key = UUID.randomUUID().toString() // Different on each recomposition!
```

### Tab Key Management

For tabs, we need to preserve state for ALL stacks, not just the active one:

```kotlin
@Composable
fun QuoVadisHost(/* ... */) {
    // ...
    
    // Find all TabNodes and retain their stack keys
    val tabNodes = findAllTabNodes(navState)
    tabNodes.forEach { tabNode ->
        tabNode.stacks.forEach { stack ->
            // Retain even if not currently rendered
            stateHolder.retain(stack.key)
            // Also retain all children recursively
            collectAllKeys(stack).forEach { childKey ->
                stateHolder.retain(childKey)
            }
        }
    }
    
    // ...
}

private fun findAllTabNodes(node: NavNode): List<TabNode> {
    val result = mutableListOf<TabNode>()
    findTabNodesRecursive(node, result)
    return result
}

private fun findTabNodesRecursive(node: NavNode, result: MutableList<TabNode>) {
    when (node) {
        is ScreenNode -> { /* No tabs in screen */ }
        is StackNode -> node.children.forEach { findTabNodesRecursive(it, result) }
        is TabNode -> {
            result.add(node)
            node.stacks.forEach { findTabNodesRecursive(it, result) }
        }
        is PaneNode -> node.panes.forEach { findTabNodesRecursive(it, result) }
    }
}
```

---

## Implementation Steps

### Step 1: Basic SaveableStateHolder

1. Add `rememberSaveableStateHolder()` to QuoVadisHost
2. Wrap surface content with `SaveableStateProvider`
3. Use `surface.id` as key

### Step 2: State Cleanup

1. Track active keys from NavNode tree
2. Detect removed keys on state change
3. Call `removeState()` for removed keys

### Step 3: NavigationStateHolder Class

1. Create `NavigationStateHolder` wrapper
2. Add `retain()` and `release()` methods
3. Implement `cleanup()` logic

### Step 4: Tab State Preservation

1. Identify all TabNodes in tree
2. Retain keys for all tab stacks
3. Retain children keys recursively

### Step 5: Testing

1. Test tab switch preserves state
2. Test pop removes state
3. Test process death restoration

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/QuoVadisHost.kt` | Modify | Add SaveableStateHolder integration |
| `quo-vadis-core/.../compose/NavigationStateHolder.kt` | Create | Wrapper class with retention logic |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-004 (QuoVadisHost) | Hard | Must complete first |

---

## Acceptance Criteria

### Core State Preservation
- [ ] `SaveableStateHolder` integrated into QuoVadisHost
- [ ] Each surface wrapped with `SaveableStateProvider(key = surface.id)`
- [ ] State cleanup on screen removal (pop)
- [ ] Tab state preserved across tab switches
- [ ] Inactive tab screens retain state
- [ ] `NavigationStateHolder` wrapper class implemented
- [ ] `retain()` and `release()` methods work correctly
- [ ] No memory leaks from unreleased states
- [ ] Process death restoration works with `rememberSaveable`
- [ ] Keys are stable across recompositions

### Differentiated Caching
- [ ] `CacheScope` enum defined with `FULL_SCREEN`, `WHOLE_WRAPPER`, `CONTENT_ONLY`
- [ ] `determineCacheScope()` method implemented in `NavigationStateHolder`
- [ ] Cross-node navigation caches whole wrapper (TabNode/PaneNode)
- [ ] Intra-tab navigation caches only content
- [ ] Intra-pane navigation caches only content
- [ ] Wrapper state preserved during tab/pane switches
- [ ] `SaveableWithScope()` composable implemented

### Documentation & Testing
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for key collection
- [ ] Unit tests for differentiated caching scenarios
- [ ] UI tests for state preservation scenarios
- [ ] UI tests for wrapper vs content state isolation

---

## Testing Notes

### Unit Tests

```kotlin
@Test
fun `collectAllKeys finds all keys in tree`() {
    val screen1 = ScreenNode("s1", "stack", mockDest)
    val screen2 = ScreenNode("s2", "stack", mockDest)
    val stack = StackNode("stack", null, listOf(screen1, screen2))
    
    val keys = collectAllKeys(stack)
    
    assertEquals(setOf("stack", "s1", "s2"), keys)
}

@Test
fun `collectAllKeys handles tabs`() {
    val home = ScreenNode("home", "tab0-stack", mockDest)
    val profile = ScreenNode("profile", "tab1-stack", mockDest)
    val stack0 = StackNode("tab0-stack", "tabs", listOf(home))
    val stack1 = StackNode("tab1-stack", "tabs", listOf(profile))
    val tabs = TabNode("tabs", null, listOf(stack0, stack1))
    
    val keys = collectAllKeys(tabs)
    
    assertEquals(setOf("tabs", "tab0-stack", "home", "tab1-stack", "profile"), keys)
}
```

### UI Tests

```kotlin
@Test
fun `tab switch preserves scroll position`() {
    val navigator = Navigator(/* TabNode setup */)
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { dest ->
            when (dest) {
                is HomeDestination -> HomeScreenWithScroll()
                is ProfileDestination -> ProfileScreen()
            }
        }
    }
    
    // Scroll home screen
    composeTestRule.onNodeWithTag("home-list").performScrollTo(
        composeTestRule.onNodeWithTag("item-50")
    )
    
    // Switch to profile tab
    navigator.switchTab(1)
    composeTestRule.waitForIdle()
    
    // Switch back to home tab
    navigator.switchTab(0)
    composeTestRule.waitForIdle()
    
    // Verify scroll position preserved
    composeTestRule.onNodeWithTag("item-50").assertIsDisplayed()
}

@Test
fun `pop cleans up state`() {
    val navigator = Navigator(/* setup */)
    var stateWasRestored = false
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { dest ->
            when (dest) {
                is HomeDestination -> HomeScreen()
                is DetailDestination -> DetailScreen(
                    onStateRestored = { stateWasRestored = true }
                )
            }
        }
    }
    
    // Navigate to detail and set some state
    navigator.push(DetailDestination)
    composeTestRule.waitForIdle()
    
    // Pop back
    navigator.pop()
    composeTestRule.waitForIdle()
    
    // Push detail again - state should NOT be restored
    stateWasRestored = false
    navigator.push(DetailDestination)
    composeTestRule.waitForIdle()
    
    assertFalse(stateWasRestored) // State was cleaned up
}
```

### Differentiated Caching Tests

```kotlin
@Test
fun `cross-node navigation caches whole tab wrapper`() {
    val navigator = Navigator(/* TabNode setup */)
    var wrapperRecompositionCount = 0
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { dest ->
            TabbedWrapper(
                onComposition = { wrapperRecompositionCount++ }
            ) {
                when (dest) {
                    is HomeDestination -> HomeScreen()
                    is ProfileDestination -> ProfileScreen()
                }
            }
        }
    }
    
    val initialCount = wrapperRecompositionCount
    
    // Switch tabs (cross-node navigation)
    navigator.switchTab(1)
    composeTestRule.waitForIdle()
    
    // Switch back
    navigator.switchTab(0)
    composeTestRule.waitForIdle()
    
    // Wrapper should NOT have recomposed (cached as WHOLE_WRAPPER)
    assertEquals(initialCount, wrapperRecompositionCount)
}

@Test
fun `intra-tab navigation caches only tab content`() {
    val navigator = Navigator(/* TabNode with StackNode setup */)
    var contentStatePreserved = false
    var wrapperStatePreserved = false
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { dest ->
            TabbedWrapper(
                onStateCheck = { wrapperStatePreserved = it }
            ) {
                when (dest) {
                    is HomeDestination -> HomeScreen(
                        onStateCheck = { contentStatePreserved = it }
                    )
                    is DetailDestination -> DetailScreen()
                }
            }
        }
    }
    
    // Set state in home screen
    composeTestRule.onNodeWithTag("home-input").performTextInput("test")
    
    // Navigate within tab (intra-tab navigation)
    navigator.push(DetailDestination)
    composeTestRule.waitForIdle()
    
    // Pop back
    navigator.pop()
    composeTestRule.waitForIdle()
    
    // Content state should be preserved (cached as CONTENT_ONLY)
    assertTrue(contentStatePreserved)
    // Wrapper state should also be preserved (never left composition)
    assertTrue(wrapperStatePreserved)
}

@Test
fun `pane wrapper state preserved during pane navigation`() {
    val navigator = Navigator(/* PaneNode setup */)
    var paneLayoutState: List<Float>? = null
    
    composeTestRule.setContent {
        QuoVadisHost(navigator) { dest ->
            MultiPaneWrapper(
                onLayoutState = { paneLayoutState = it }
            ) {
                // Pane content
            }
        }
    }
    
    // Modify pane layout (e.g., resize)
    val originalLayout = paneLayoutState?.toList()
    
    // Navigate within pane
    navigator.push(DetailDestination)
    composeTestRule.waitForIdle()
    
    // Layout state should be preserved
    assertEquals(originalLayout, paneLayoutState)
}

@Test
fun `determineCacheScope returns correct scope for cross-node navigation`() {
    val stateHolder = NavigationStateHolder(mockSaveableStateHolder)
    val transition = mockTransitionState(isCrossNodeType = true)
    
    val scope = stateHolder.determineCacheScope(
        transition = transition,
        surfaceId = "tab-wrapper",
        surfaceMode = SurfaceRenderingMode.TAB_WRAPPER
    )
    
    assertEquals(CacheScope.WHOLE_WRAPPER, scope)
}

@Test
fun `determineCacheScope returns correct scope for intra-tab navigation`() {
    val stateHolder = NavigationStateHolder(mockSaveableStateHolder)
    val transition = mockTransitionState(isCrossNodeType = false)
    
    val scope = stateHolder.determineCacheScope(
        transition = transition,
        surfaceId = "tab-content",
        surfaceMode = SurfaceRenderingMode.TAB_CONTENT
    )
    
    assertEquals(CacheScope.CONTENT_ONLY, scope)
}

@Test
fun `determineCacheScope returns FULL_SCREEN for regular screen navigation`() {
    val stateHolder = NavigationStateHolder(mockSaveableStateHolder)
    val transition = mockTransitionState(isCrossNodeType = false)
    
    val scope = stateHolder.determineCacheScope(
        transition = transition,
        surfaceId = "screen",
        surfaceMode = SurfaceRenderingMode.FULL_SCREEN
    )
    
    assertEquals(CacheScope.FULL_SCREEN, scope)
}
```

---

## Performance Considerations

### Memory Management

```kotlin
// Eagerly cleanup to prevent memory growth
LaunchedEffect(activeKeys) {
    // Small delay to ensure animations complete
    delay(500)
    
    val removedKeys = previousKeys - activeKeys - retainedKeys
    removedKeys.forEach { key ->
        saveableStateHolder.removeState(key)
    }
}
```

### Key Set Size

For deep navigation trees, key sets can become large. Consider:

```kotlin
// Only track screen-level keys, not container keys
private fun collectScreenKeys(node: NavNode): Set<String> {
    val keys = mutableSetOf<String>()
    when (node) {
        is ScreenNode -> keys.add(node.key)
        is StackNode -> node.children.forEach { keys.addAll(collectScreenKeys(it)) }
        is TabNode -> node.stacks.forEach { keys.addAll(collectScreenKeys(it)) }
        is PaneNode -> node.panes.forEach { keys.addAll(collectScreenKeys(it)) }
    }
    return keys
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost base implementation
- [Compose State Saving](https://developer.android.com/jetpack/compose/state#saveable)
- [SaveableStateHolder](https://developer.android.com/reference/kotlin/androidx/compose/runtime/saveable/SaveableStateHolder)
- [CORE-001](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode key property
