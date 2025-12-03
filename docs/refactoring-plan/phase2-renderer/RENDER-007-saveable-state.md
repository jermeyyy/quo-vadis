# RENDER-007: SaveableStateHolder Integration

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-007 |
| **Task Name** | SaveableStateHolder Integration |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | RENDER-004 |
| **Blocked By** | RENDER-004 |
| **Blocks** | - |

---

## Overview

`SaveableStateHolder` is the Compose mechanism for preserving state across composition lifecycle changes. In the context of QuoVadisHost, it's essential for:

1. **Tab state preservation** - When switching tabs, inactive tab's state should survive
2. **Back stack restoration** - Screens on the back stack maintain their state
3. **Process death survival** - State persists across activity recreation

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
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for key collection
- [ ] UI tests for state preservation scenarios

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
