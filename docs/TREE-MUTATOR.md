# TreeMutator API Reference

Advanced tree manipulation for Quo Vadis navigation.

## Overview

`TreeMutator` is a pure functional façade for manipulating the navigation tree. While `Navigator` provides high-level navigation APIs that handle state updates and transitions automatically, `TreeMutator` offers direct control over tree transformations for advanced use cases.

```kotlin
// Navigator API - handles state updates automatically
navigator.navigate(ProfileDestination(userId))

// TreeMutator API - pure function, returns new tree
val newTree = TreeMutator.push(currentTree, ProfileDestination(userId))
navigator.updateState(newTree, transition = null)
```

### When to Use TreeMutator

| Use Case | Recommended API |
|----------|-----------------|
| Standard navigation | `Navigator.navigate()` |
| Simple back navigation | `Navigator.navigateBack()` |
| Tab switching | `Navigator.switchTab()` |
| **Batch operations** | `TreeMutator` |
| **Custom back logic** | `TreeMutator` |
| **State diffing/analytics** | `TreeMutator` |
| **Testing navigation logic** | `TreeMutator` |
| **Building custom flows** | `TreeMutator` |

---

## Design Philosophy

TreeMutator follows functional programming principles:

### Pure Functions

Every operation is a **pure function** that takes an input tree and returns a new tree:

```kotlin
// Mathematical model: S_new = f(S_old, Action)
val newState: NavNode = TreeMutator.push(oldState, destination)
```

### Immutability

The original tree is **never modified**. All operations create new tree instances:

```kotlin
val original = navigator.state.value
val modified = TreeMutator.pop(original)

// original is unchanged
assertNotSame(original, modified)
```

### Structural Sharing

Unchanged subtrees are **reused by reference**, making operations memory-efficient:

```kotlin
// Given: Root → TabNode → [Stack1, Stack2]
// After pushing to Stack1, Stack2 is reused by reference
val afterPush = TreeMutator.push(root, destination)
```

### Thread Safety

Since all operations are pure functions with no mutable state, `TreeMutator` is **inherently thread-safe** without synchronization.

---

## Push Operations

Operations for forward navigation.

### `push()`

Push a destination onto the deepest active stack.

```kotlin
fun push(
    root: NavNode,
    destination: NavDestination,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Basic Usage:**

```kotlin
val newTree = TreeMutator.push(
    root = navigator.state.value,
    destination = ArticleDestination(articleId = "123")
)
navigator.updateState(newTree, transition = SlideTransition)
```

**Tree Behavior:**

```
Before: StackNode → [HomeScreen]
After:  StackNode → [HomeScreen, ArticleScreen(123)]
```

### `push()` with Scope Awareness

Push with intelligent scope handling, tab switching, and pane role routing.

```kotlin
fun push(
    root: NavNode,
    destination: NavDestination,
    scopeRegistry: ScopeRegistry,
    paneRoleRegistry: PaneRoleRegistry = PaneRoleRegistry.Empty,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Navigation Logic:**

1. **Scope Check** - If destination is out of scope, push to parent stack
2. **Tab Switching** - If destination exists in another tab, switch instead of duplicating
3. **Pane Role Routing** - If destination has a pane role, push to that pane's stack

**Example - Scope-Aware Navigation:**

```kotlin
// Given tree: RootStack → TabNode(scope="messages") → MessageList
// Navigating to ProfileDestination (NOT in "messages" scope)
val newTree = TreeMutator.push(
    root = currentTree,
    destination = ProfileDestination(userId),
    scopeRegistry = scopeRegistry  // from NavigationConfig
)
// Result: Profile pushed ABOVE TabNode, not inside it
// RootStack → [TabNode, ProfileScreen]
```

### `pushToStack()`

Push to a specific stack by key, regardless of which stack is currently active.

```kotlin
fun pushToStack(
    root: NavNode,
    stackKey: String,
    destination: NavDestination,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Use Case - Pre-populating Tab Stacks:**

```kotlin
// Pre-load content into inactive tabs
val withPreloadedTabs = TabNode.stacks.foldIndexed(currentTree) { index, tree, stack ->
    if (index != activeIndex) {
        TreeMutator.pushToStack(tree, stack.key, preloadDestinations[index])
    } else tree
}
```

### `pushAll()`

Push multiple destinations in a single operation (more efficient than multiple pushes).

```kotlin
fun pushAll(
    root: NavNode,
    destinations: List<NavDestination>,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Use Case - Deep Link with Multiple Screens:**

```kotlin
// Deep link: app://orders/123/tracking
val newTree = TreeMutator.pushAll(
    root = currentTree,
    destinations = listOf(
        OrderListDestination,
        OrderDetailDestination("123"),
        TrackingDestination("123")
    )
)
```

### `clearAndPush()`

Clear the active stack and push a single screen (navigate and clear).

```kotlin
fun clearAndPush(
    root: NavNode,
    destination: NavDestination,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Use Case - Return to Home:**

```kotlin
// Clear everything and show home
val newTree = TreeMutator.clearAndPush(
    root = currentTree,
    destination = HomeDestination
)
```

### `clearStackAndPush()`

Clear a specific stack by key and push a screen.

```kotlin
fun clearStackAndPush(
    root: NavNode,
    stackKey: String,
    destination: NavDestination,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

### `replaceCurrent()`

Replace the current screen without adding to the back stack.

```kotlin
fun replaceCurrent(
    root: NavNode,
    destination: NavDestination,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Use Case - Auth Flow:**

```kotlin
// Replace login screen with dashboard after authentication
val newTree = TreeMutator.replaceCurrent(
    root = currentTree,
    destination = DashboardDestination
)
```

---

## Pop Operations

Operations for backward navigation.

### `pop()`

Pop the active screen from the deepest active stack.

```kotlin
fun pop(
    root: NavNode,
    behavior: PopBehavior = PopBehavior.PRESERVE_EMPTY
): NavNode?
```

Returns `null` if the tree cannot be popped (at root or empty).

**PopBehavior Options:**

| Behavior | Description |
|----------|-------------|
| `PRESERVE_EMPTY` | Keep empty stacks in place |
| `CASCADE` | Remove empty stacks, cascading up the tree |

**Example - Cascade Pop:**

```kotlin
// With CASCADE, popping last item removes the entire stack
val result = TreeMutator.pop(root, PopBehavior.CASCADE)

// Before: RootStack → NestedStack → [ScreenA]
// After CASCADE pop: RootStack → [] (NestedStack removed)
```

### `popTo()`

Pop all screens until a predicate matches.

```kotlin
fun popTo(
    root: NavNode,
    inclusive: Boolean = false,
    predicate: (NavNode) -> Boolean
): NavNode
```

**Example - Pop to Specific Screen Type:**

```kotlin
// Pop to the home screen
val newTree = TreeMutator.popTo(root, inclusive = false) { node ->
    node is ScreenNode && node.destination is HomeDestination
}
```

### `popToRoute()`

Pop to a screen with the given route string.

```kotlin
fun popToRoute(
    root: NavNode,
    route: String,
    inclusive: Boolean = false
): NavNode
```

**Example:**

```kotlin
val newTree = TreeMutator.popToRoute(
    root = currentTree,
    route = "home/feed",
    inclusive = false
)
```

### `popToDestination<D>()`

Pop to a screen with a destination of the specified type.

```kotlin
inline fun <reified D : NavDestination> popToDestination(
    root: NavNode,
    inclusive: Boolean = false
): NavNode
```

**Example:**

```kotlin
// Pop to the first OrderListDestination
val newTree = TreeMutator.popToDestination<OrderListDestination>(
    root = currentTree,
    inclusive = false
)
```

---

## Tab Operations

Operations for tab-based navigation containers.

### `switchTab()`

Switch to a specific tab by TabNode key and index.

```kotlin
fun switchTab(
    root: NavNode,
    tabNodeKey: String,
    newIndex: Int
): NavNode
```

**Example:**

```kotlin
val newTree = TreeMutator.switchTab(
    root = currentTree,
    tabNodeKey = mainTabs.key,
    newIndex = 2  // Switch to third tab
)
```

### `switchActiveTab()`

Switch tab in the first TabNode found in the active path (when you don't know the key).

```kotlin
fun switchActiveTab(
    root: NavNode,
    newIndex: Int
): NavNode
```

**Example:**

```kotlin
// Switch active TabNode to index 0 (e.g., Home tab)
val newTree = TreeMutator.switchActiveTab(currentTree, newIndex = 0)
```

---

## Pane Operations

Operations for adaptive pane navigation (list-detail layouts).

### `navigateToPane()`

Navigate to a destination within a specific pane role.

```kotlin
fun navigateToPane(
    root: NavNode,
    nodeKey: String,
    role: PaneRole,
    destination: NavDestination,
    switchFocus: Boolean = true,
    generateKey: () -> String = KeyGenerator.Default::generate
): NavNode
```

**Example - List-Detail Pattern:**

```kotlin
// When item selected in list, show detail in Supporting pane
val newTree = TreeMutator.navigateToPane(
    root = currentTree,
    nodeKey = paneNode.key,
    role = PaneRole.Supporting,
    destination = ItemDetailDestination(itemId),
    switchFocus = true  // Focus moves to detail pane
)
```

### `switchActivePane()`

Change the active pane focus without navigating.

```kotlin
fun switchActivePane(
    root: NavNode,
    nodeKey: String,
    role: PaneRole
): NavNode
```

**Example:**

```kotlin
// Switch focus to Supporting pane
val newTree = TreeMutator.switchActivePane(
    root = currentTree,
    nodeKey = paneNode.key,
    role = PaneRole.Supporting
)
```

### `popPane()`

Pop the top entry from a specific pane's stack.

```kotlin
fun popPane(
    root: NavNode,
    nodeKey: String,
    role: PaneRole
): NavNode?
```

**Example:**

```kotlin
// Pop from detail pane specifically
val newTree = TreeMutator.popPane(
    root = currentTree,
    nodeKey = paneNode.key,
    role = PaneRole.Supporting
)
```

### `popWithPaneBehavior()`

Pop respecting the configured `PaneBackBehavior`.

```kotlin
fun popWithPaneBehavior(root: NavNode): PopResult
```

Returns a `PopResult` indicating the outcome.

### `popPaneAdaptive()`

Pop with window size awareness.

```kotlin
fun popPaneAdaptive(
    root: NavNode,
    isCompact: Boolean
): PopResult
```

| Mode | Behavior |
|------|----------|
| Compact | Simple stack-like back (ignores `PaneBackBehavior`) |
| Expanded | Uses configured `PaneBackBehavior` |

### `setPaneConfiguration()` / `removePaneConfiguration()`

Configure or remove pane configurations at runtime.

```kotlin
fun setPaneConfiguration(
    root: NavNode,
    nodeKey: String,
    role: PaneRole,
    config: PaneConfiguration
): NavNode

fun removePaneConfiguration(
    root: NavNode,
    nodeKey: String,
    role: PaneRole
): NavNode
```

---

## Back Navigation

Complex back navigation with tree awareness.

### `popWithTabBehavior()`

Intelligent back navigation that considers tabs, panes, and nested stacks.

```kotlin
fun popWithTabBehavior(
    root: NavNode,
    isCompact: Boolean = true
): BackResult
```

**Algorithm:**

1. If active stack has items → pop from stack
2. If inside TabNode and not on initial tab → pop entire TabNode
3. If inside PaneNode → use pane behavior based on `isCompact`
4. Cascade up the tree until handled or delegate to system

### `BackResult` Sealed Class

```kotlin
sealed class BackResult {
    /** Back was handled, new tree state returned */
    data class Handled(val newState: NavNode) : BackResult()
    
    /** Back should be delegated to system (e.g., close app) */
    data object DelegateToSystem : BackResult()
    
    /** Back could not be handled (internal error) */
    data object CannotHandle : BackResult()
}
```

**Example - Custom Back Handler:**

```kotlin
fun handleBack(): Boolean {
    val result = TreeMutator.popWithTabBehavior(
        root = navigator.state.value,
        isCompact = windowSizeClass.isCompact
    )
    
    return when (result) {
        is BackResult.Handled -> {
            navigator.updateState(result.newState, transition = null)
            true
        }
        is BackResult.DelegateToSystem -> false  // Let system handle
        is BackResult.CannotHandle -> false
    }
}
```

### `canHandleBackNavigation()`

Check if back can be handled without delegating to the system.

```kotlin
fun canHandleBackNavigation(root: NavNode): Boolean
```

Considers:
- Pop opportunities in active stack
- Tab switching as alternative to popping
- Cascade back (removing TabNode/PaneNode from parent)

### `canGoBack()`

Simple check if any pop is possible.

```kotlin
fun canGoBack(root: NavNode): Boolean
```

### `currentDestination()`

Get the current active destination.

```kotlin
fun currentDestination(root: NavNode): NavDestination?
```

---

## Node Operations

Low-level tree manipulation.

### `replaceNode()`

Replace a node in the tree by key (with structural sharing).

```kotlin
fun replaceNode(
    root: NavNode,
    targetKey: String,
    newNode: NavNode
): NavNode
```

**Example - Update Screen State:**

```kotlin
// Replace a screen node with an updated version
val updatedScreen = existingScreen.copy(/* modified properties */)
val newTree = TreeMutator.replaceNode(
    root = currentTree,
    targetKey = existingScreen.key,
    newNode = updatedScreen
)
```

### `removeNode()`

Remove a node from the tree by key.

```kotlin
fun removeNode(
    root: NavNode,
    targetKey: String
): NavNode?
```

**Restrictions:**

- Cannot remove the root node (returns `null`)
- Cannot remove tab stacks from TabNode (use `switchTab`)
- Cannot remove pane content directly (use `removePaneConfiguration`)

---

## PopResult Sealed Class

Result type for pane pop operations.

```kotlin
sealed class PopResult {
    /** Successfully popped within current pane */
    data class Popped(val newState: NavNode) : PopResult()
    
    /** Pane is empty, behavior depends on PaneBackBehavior */
    data class PaneEmpty(val paneRole: PaneRole) : PopResult()
    
    /** Cannot pop - would leave tree in invalid state */
    data object CannotPop : PopResult()
    
    /** Back behavior requires scaffold/visual change (renderer must handle) */
    data object RequiresScaffoldChange : PopResult()
}
```

---

## Tree Traversal Utilities

Extension functions on `NavNode` for tree inspection.

| Function | Returns | Description |
|----------|---------|-------------|
| `findByKey(key)` | `NavNode?` | Find node by key recursively |
| `activeLeaf()` | `ScreenNode?` | Get deepest active screen |
| `activeStack()` | `StackNode?` | Get deepest active stack |
| `activePathToLeaf()` | `List<NavNode>` | Path from root to active leaf |
| `allScreens()` | `List<ScreenNode>` | All screens in subtree |
| `allPaneNodes()` | `List<PaneNode>` | All panes in subtree |
| `allTabNodes()` | `List<TabNode>` | All tabs in subtree |
| `allStackNodes()` | `List<StackNode>` | All stacks in subtree |
| `depth()` | `Int` | Tree depth from this node |
| `nodeCount()` | `Int` | Total nodes in subtree |
| `canHandleBackInternally()` | `Boolean` | Can this node handle back? |

**Example - Find Active Path:**

```kotlin
val activePath = navigator.state.value.activePathToLeaf()
// [RootStack, MainTabs, HomeStack, HomeScreen]
```

---

## Advanced Use Cases

### Custom Navigation Coordinator

Build a coordinator that combines multiple navigation actions:

```kotlin
class OrderFlowCoordinator(private val navigator: Navigator) {
    
    fun startCheckout(cartId: String) {
        var tree = navigator.state.value
        
        // Clear any existing checkout screens
        tree = TreeMutator.popTo(tree, inclusive = true) { node ->
            node is ScreenNode && node.destination is CheckoutDestination
        }
        
        // Push entire checkout flow
        tree = TreeMutator.pushAll(tree, listOf(
            CheckoutDestination.Shipping(cartId),
            CheckoutDestination.Payment(cartId),
            CheckoutDestination.Review(cartId)
        ))
        
        navigator.updateState(tree, SlideTransition)
    }
    
    fun abandonCheckout() {
        val tree = TreeMutator.popToDestination<HomeDestination>(
            root = navigator.state.value,
            inclusive = false
        )
        navigator.updateState(tree, FadeTransition)
    }
}
```

### State Diffing for Analytics

Track navigation changes for analytics:

```kotlin
class NavigationAnalytics(private val navigator: Navigator) {
    
    init {
        navigator.state
            .scan(null to navigator.state.value) { (_, prev), current -> 
                prev to current 
            }
            .collect { (previous, current) ->
                if (previous != null) {
                    trackNavigationChange(previous, current)
                }
            }
    }
    
    private fun trackNavigationChange(old: NavNode, new: NavNode) {
        val oldScreens = old.allScreens().map { it.destination::class }
        val newScreens = new.allScreens().map { it.destination::class }
        
        val added = newScreens - oldScreens.toSet()
        val removed = oldScreens - newScreens.toSet()
        
        added.forEach { analytics.trackScreenOpened(it.simpleName) }
        removed.forEach { analytics.trackScreenClosed(it.simpleName) }
    }
}
```

### Navigation Guards

Implement navigation guards that intercept and modify navigation:

```kotlin
class AuthNavigationGuard(
    private val navigator: Navigator,
    private val authManager: AuthManager
) {
    
    fun navigateWithGuard(destination: NavDestination) {
        val tree = navigator.state.value
        
        val finalTree = if (requiresAuth(destination) && !authManager.isAuthenticated) {
            // Push auth flow, then target destination
            TreeMutator.pushAll(tree, listOf(
                LoginDestination(returnTo = destination.route),
            ))
        } else {
            TreeMutator.push(tree, destination, navigator.config.scopeRegistry)
        }
        
        navigator.updateState(finalTree, null)
    }
    
    private fun requiresAuth(destination: NavDestination): Boolean {
        return destination::class.annotations.any { it is RequiresAuth }
    }
}
```

### Testing Navigation Logic

Test navigation behavior without UI:

```kotlin
class NavigationTests {
    
    @Test
    fun `pop from nested stack cascades correctly`() {
        // Build initial state
        val root = StackNode(
            key = "root",
            children = listOf(
                ScreenNode(key = "home", destination = HomeDestination),
                StackNode(
                    key = "nested",
                    children = listOf(
                        ScreenNode(key = "detail", destination = DetailDestination("1"))
                    )
                )
            )
        )
        
        // Pop with cascade
        val result = TreeMutator.pop(root, PopBehavior.CASCADE)
        
        // Nested stack should be removed, leaving just home
        assertNotNull(result)
        assertTrue(result is StackNode)
        assertEquals(1, (result as StackNode).children.size)
        assertEquals("home", result.children.first().key)
    }
    
    @Test
    fun `push respects scope boundaries`() {
        val scopeRegistry = ScopeRegistry.Builder()
            .scope("messages") { it is MessageDestination }
            .build()
        
        val root = buildTestTreeWithMessageScope()
        
        // Push out-of-scope destination
        val result = TreeMutator.push(
            root = root,
            destination = ProfileDestination("user1"),
            scopeRegistry = scopeRegistry
        )
        
        // Should be pushed above the scoped container
        val activePath = result.activePathToLeaf()
        val profileIndex = activePath.indexOfFirst { 
            it is ScreenNode && it.destination is ProfileDestination 
        }
        val tabIndex = activePath.indexOfFirst { it is TabNode }
        
        assertTrue(profileIndex > tabIndex)
    }
}
```

### Batch State Restoration

Restore complex navigation state efficiently:

```kotlin
class StateRestorer(private val navigator: Navigator) {
    
    fun restoreFromDeepLink(uri: Uri) {
        // Parse deep link into destinations
        val destinations = parseDeepLink(uri)
        
        // Build tree in a single operation
        var tree: NavNode = StackNode(
            key = generateKey(),
            parentKey = null,
            children = emptyList()
        )
        
        // Add all destinations
        tree = TreeMutator.pushAll(tree, destinations) { generateKey() }
        
        // Apply to navigator
        navigator.updateState(tree, transition = null)
    }
    
    fun restoreFromSavedState(savedTree: NavNode) {
        // Validate and potentially migrate saved state
        val validatedTree = migrateIfNeeded(savedTree)
        navigator.updateState(validatedTree, transition = null)
    }
}
```

---

## Thread Safety Considerations

All `TreeMutator` operations are pure functions, but state updates on `Navigator` should be synchronized:

```kotlin
// Safe: TreeMutator operations are pure
val newTree1 = TreeMutator.push(currentTree, destination1)
val newTree2 = TreeMutator.push(currentTree, destination2)

// Navigate updates should happen on main thread
withContext(Dispatchers.Main) {
    navigator.updateState(newTree1, transition)
}
```

---

## Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|-----------------|-------|
| `push()` | O(d) | d = tree depth |
| `pop()` | O(d) | d = tree depth |
| `findByKey()` | O(n) | n = total nodes |
| `replaceNode()` | O(d) | Structural sharing |
| `activeLeaf()` | O(d) | Single path traversal |
| `allScreens()` | O(n) | Full tree traversal |

Structural sharing ensures memory efficiency even with frequent mutations.

---

## See Also

- [NAV-NODES.md](NAV-NODES.md) - Navigation tree node types
- [NAVIGATOR.md](NAVIGATOR.md) - High-level Navigator API
- [ARCHITECTURE.md](ARCHITECTURE.md) - System architecture overview
- [TRANSITIONS.md](TRANSITIONS.md) - Animation and transition system
