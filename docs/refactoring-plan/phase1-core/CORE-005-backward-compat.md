# CORE-005: Create Backward Compatibility Layer

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | CORE-005 |
| **Task Name** | Create Backward Compatibility Layer |
| **Phase** | Phase 1: Core State Refactoring |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | CORE-001, CORE-003 |
| **Blocked By** | CORE-001, CORE-003 |
| **Blocks** | - |

---

## Overview

This task creates a backward compatibility layer that allows existing code using the `BackStack` API to continue functioning during the migration to the tree-based `NavNode` architecture.

### Goals

1. **Zero-breakage migration** - Existing code continues to work without modification
2. **Gradual adoption** - Teams can migrate at their own pace
3. **Clear deprecation path** - Deprecated APIs guide toward new patterns
4. **Equivalent functionality** - All existing operations have tree-based equivalents

### Strategy

| Legacy API | Compatibility Approach | Migration Path |
|------------|----------------------|----------------|
| `navigator.backStack` | Adapter projecting tree state | Use `navigator.state` |
| `navigator.entries` | Computed from active path | Use `state.activePathToLeaf()` |
| `backStack.push()` | Delegate to `TreeMutator.push()` | Use `navigator.navigate()` |
| `backStack.pop()` | Delegate to `TreeMutator.pop()` | Use `navigator.navigateBack()` |
| `BackStackEntry` | Created from `ScreenNode` | Use `ScreenNode` directly |

---

## File Locations

| File | Action |
|------|--------|
| `quo-vadis-core/.../core/BackStackCompat.kt` | Create |
| `quo-vadis-core/.../core/NavigatorExtensions.kt` | Create |
| `quo-vadis-core/.../core/LegacyBackStackAdapter.kt` | Create (part of CORE-003) |

---

## Implementation

### Phase 1: Extension Properties

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/**
 * Backward compatibility extensions for Navigator.
 * 
 * These extensions provide the familiar BackStack-style API while
 * internally using the new NavNode tree structure.
 * 
 * ## Migration Guide
 * 
 * | Old Pattern | New Pattern |
 * |-------------|-------------|
 * | `navigator.activeStack` | `navigator.state.value.activeStack()` |
 * | `navigator.backStackEntries` | `navigator.state.value.activePathToLeaf()` |
 * | `navigator.backStackSize` | `navigator.state.value.activeStack()?.size` |
 * 
 * @see NavNode
 * @see TreeMutator
 */

// =============================================================================
// STACK ACCESS EXTENSIONS
// =============================================================================

/**
 * Get the currently active StackNode.
 * 
 * @deprecated Use `state.value.activeStack()` instead.
 */
@Deprecated(
    message = "Use state.value.activeStack() for tree-based navigation",
    replaceWith = ReplaceWith(
        "state.value.activeStack()",
        "com.jermey.quo.vadis.core.navigation.core.activeStack"
    )
)
val Navigator.activeStack: StackNode?
    get() = state.value.activeStack()

/**
 * Get backstack entries as a list (active path only).
 * 
 * @deprecated Use `state.value.activePathToLeaf()` instead.
 */
@Deprecated(
    message = "Use state.value.activePathToLeaf() for tree-based navigation",
    replaceWith = ReplaceWith(
        "state.value.activePathToLeaf().filterIsInstance<ScreenNode>()",
        "com.jermey.quo.vadis.core.navigation.core.activePathToLeaf"
    )
)
val Navigator.backStackEntries: List<BackStackEntry>
    get() = state.value.activePathToLeaf()
        .filterIsInstance<ScreenNode>()
        .map { BackStackEntry(it.destination, it.destination.transition) }

/**
 * Get the size of the active backstack.
 * 
 * @deprecated Use `state.value.activeStack()?.size` instead.
 */
@Deprecated(
    message = "Use state.value.activeStack()?.size instead",
    replaceWith = ReplaceWith("state.value.activeStack()?.size ?: 0")
)
val Navigator.backStackSize: Int
    get() = state.value.activeStack()?.size ?: 0

/**
 * Check if the navigator can go back in the active stack.
 * 
 * @deprecated Use `state.value.activeStack()?.canGoBack` instead.
 */
@Deprecated(
    message = "Use state.value.activeStack()?.canGoBack instead",
    replaceWith = ReplaceWith("state.value.activeStack()?.canGoBack ?: false")
)
val Navigator.canGoBack: Boolean
    get() = state.value.activeStack()?.canGoBack ?: false

// =============================================================================
// FLOW EXTENSIONS
// =============================================================================

/**
 * Flow of the active StackNode.
 * 
 * @deprecated Use `state.map { it.activeStack() }` instead.
 */
@Deprecated(
    message = "Use state.map { it.activeStack() } instead",
    replaceWith = ReplaceWith(
        "state.map { it.activeStack() }",
        "kotlinx.coroutines.flow.map"
    )
)
val Navigator.activeStackFlow: StateFlow<StackNode?>
    get() = state.map { it.activeStack() } as StateFlow<StackNode?>

/**
 * Flow of whether navigation back is possible.
 * 
 * @deprecated Use `state.map { it.activeStack()?.canGoBack ?: false }` instead.
 */
@Deprecated(
    message = "Use state.map { it.activeStack()?.canGoBack } instead"
)
val Navigator.canGoBackFlow: StateFlow<Boolean>
    get() = state.map { it.activeStack()?.canGoBack ?: false } as StateFlow<Boolean>

// =============================================================================
// COMPOSABLE EXTENSIONS
// =============================================================================

/**
 * Observe the active stack as Compose State.
 * 
 * @deprecated Use `navigator.state.collectAsState()` and derive from tree.
 */
@Deprecated(
    message = "Use navigator.state.collectAsState() and extract from tree",
    replaceWith = ReplaceWith(
        "state.collectAsState().value.activeStack()",
        "androidx.compose.runtime.collectAsState",
        "com.jermey.quo.vadis.core.navigation.core.activeStack"
    )
)
@Composable
fun Navigator.activeStackAsState(): State<StackNode?> {
    return state.map { it.activeStack() }.collectAsState(initial = activeStack)
}

/**
 * Observe backstack entries as Compose State.
 * 
 * @deprecated Use `navigator.state.collectAsState()` and derive from tree.
 */
@Deprecated(
    message = "Use navigator.state.collectAsState() and extract from tree"
)
@Composable
fun Navigator.backStackEntriesAsState(): State<List<BackStackEntry>> {
    return state.map { 
        it.activePathToLeaf()
            .filterIsInstance<ScreenNode>()
            .map { node -> BackStackEntry(node.destination, node.destination.transition) }
    }.collectAsState(initial = backStackEntries)
}
```

### Phase 2: Conversion Functions

```kotlin
package com.jermey.quo.vadis.core.navigation.core

/**
 * Conversion utilities between NavNode tree and BackStack representations.
 * 
 * These utilities support gradual migration by allowing conversion between
 * the old linear backstack model and the new tree structure.
 */

// =============================================================================
// NAVNODE TO BACKSTACK CONVERSION
// =============================================================================

/**
 * Convert the active path of a NavNode tree to a BackStack-compatible list.
 * 
 * This extracts ScreenNodes from the active path and converts them to
 * BackStackEntry objects, preserving the linear representation.
 * 
 * @return List of BackStackEntry representing the active navigation history
 * @deprecated This is a compatibility helper; prefer working with NavNode directly.
 */
@Deprecated("Use NavNode tree structure directly for full navigation context")
fun NavNode.toBackStack(): List<BackStackEntry> {
    val screens = activePathToLeaf().filterIsInstance<ScreenNode>()
    return screens.map { screen ->
        BackStackEntry(
            destination = screen.destination,
            transition = screen.destination.transition
        )
    }
}

/**
 * Convert a specific StackNode to BackStackEntry list.
 * 
 * @return List of BackStackEntry from this stack's children
 */
@Deprecated("Use StackNode.children directly")
fun StackNode.toBackStackEntries(): List<BackStackEntry> {
    return children.filterIsInstance<ScreenNode>().map { screen ->
        BackStackEntry(
            destination = screen.destination,
            transition = screen.destination.transition
        )
    }
}

// =============================================================================
// BACKSTACK TO NAVNODE CONVERSION
// =============================================================================

/**
 * Convert a list of BackStackEntry to a StackNode.
 * 
 * Use this when migrating from the old BackStack API to the new tree structure.
 * 
 * @param key The key for the resulting StackNode
 * @param parentKey The parent key (null for root)
 * @param generateScreenKey Function to generate unique keys for ScreenNodes
 * @return A StackNode containing the entries as ScreenNodes
 */
fun List<BackStackEntry>.toStackNode(
    key: String = "root",
    parentKey: String? = null,
    generateScreenKey: (Int) -> String = { index -> "screen-$index" }
): StackNode {
    val children = mapIndexed { index, entry ->
        ScreenNode(
            key = generateScreenKey(index),
            parentKey = key,
            destination = entry.destination
        )
    }
    return StackNode(
        key = key,
        parentKey = parentKey,
        children = children
    )
}

/**
 * Convert a single BackStackEntry to a ScreenNode.
 * 
 * @param key The key for the resulting ScreenNode
 * @param parentKey The parent key
 * @return A ScreenNode representing the entry
 */
fun BackStackEntry.toScreenNode(
    key: String,
    parentKey: String?
): ScreenNode {
    return ScreenNode(
        key = key,
        parentKey = parentKey,
        destination = destination
    )
}

// =============================================================================
// MIGRATION HELPERS
// =============================================================================

/**
 * Create a NavNode tree from an existing Navigator's BackStack.
 * 
 * Use this to migrate state from the old navigator to the new tree-based one.
 * 
 * @param backStack The existing BackStack to migrate
 * @param rootKey Key for the root StackNode
 * @return A StackNode tree representing the current navigation state
 */
@Suppress("DEPRECATION")
fun createNavNodeFromBackStack(
    backStack: BackStack,
    rootKey: String = "root"
): NavNode {
    return backStack.entries.toList().toStackNode(
        key = rootKey,
        parentKey = null
    )
}

/**
 * Migrate from DefaultNavigator to TreeNavigator.
 * 
 * Creates a new TreeNavigator initialized with the state from an existing
 * DefaultNavigator.
 * 
 * @param legacy The existing DefaultNavigator to migrate from
 * @return A new TreeNavigator with equivalent state
 */
@Suppress("DEPRECATION")
fun migrateToTreeNavigator(legacy: Navigator): TreeNavigator {
    val initialState = createNavNodeFromBackStack(
        backStack = legacy.backStack,
        rootKey = "migrated-root"
    )
    return TreeNavigator(initialState = initialState)
}
```

### Phase 3: Deprecation Annotations Strategy

```kotlin
package com.jermey.quo.vadis.core.navigation.core

/**
 * Constants for deprecation messages used throughout the compatibility layer.
 */
internal object DeprecationMessages {
    
    const val BACKSTACK_DEPRECATION = """
        Direct BackStack access is deprecated. The navigation state is now managed as an 
        immutable NavNode tree. Use navigator.state to access the tree structure, and 
        TreeMutator operations for modifications.
        
        Migration guide:
        - navigator.backStack.entries → navigator.state.value.activePathToLeaf()
        - navigator.backStack.push(dest) → navigator.navigate(dest)
        - navigator.backStack.pop() → navigator.navigateBack()
        - navigator.backStack.current → navigator.currentDestination
    """
    
    const val ENTRIES_DEPRECATION = """
        Direct entries access is deprecated. The linear list representation loses 
        information about the tree structure (tabs, panes).
        
        For the active navigation path: navigator.state.value.activePathToLeaf()
        For all screens: navigator.state.value.allScreens()
    """
    
    const val MUTABLE_BACKSTACK_DEPRECATION = """
        MutableBackStack is deprecated. All navigation state mutations should go 
        through the Navigator interface or TreeMutator operations.
        
        This ensures proper state management, animation coordination, and 
        predictive back gesture support.
    """
}

/**
 * Annotation to mark APIs that are part of the backward compatibility layer.
 * 
 * APIs with this annotation will be removed in a future major version.
 */
@RequiresOptIn(
    message = "This API is deprecated and will be removed in version 3.0. " +
              "Migrate to tree-based NavNode APIs.",
    level = RequiresOptIn.Level.WARNING
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.PROPERTY_GETTER
)
annotation class LegacyNavigationApi
```

### Phase 4: IDE Migration Assistance

Provide structured replacements that IDEs can use for automated migration:

```kotlin
// Example of properly structured deprecation for IDE support

@Deprecated(
    message = "Use navigate() with Destination directly",
    replaceWith = ReplaceWith(
        expression = "navigate(destination)",
        imports = ["com.jermey.quo.vadis.core.navigation.core.Navigator.navigate"]
    ),
    level = DeprecationLevel.WARNING
)
fun Navigator.push(destination: Destination) {
    navigate(destination)
}

@Deprecated(
    message = "Use navigateBack() instead",
    replaceWith = ReplaceWith(
        expression = "navigateBack()",
        imports = ["com.jermey.quo.vadis.core.navigation.core.Navigator.navigateBack"]
    ),
    level = DeprecationLevel.WARNING
)
fun Navigator.pop(): Boolean {
    return navigateBack()
}

@Deprecated(
    message = "Use navigateAndClearAll() instead",
    replaceWith = ReplaceWith(
        expression = "navigateAndClearAll(destination)",
        imports = ["com.jermey.quo.vadis.core.navigation.core.Navigator.navigateAndClearAll"]
    ),
    level = DeprecationLevel.WARNING  
)
fun Navigator.clearAndNavigate(destination: Destination) {
    navigateAndClearAll(destination)
}
```

---

## Deprecation Timeline

| Version | Action |
|---------|--------|
| 2.0.0 | Introduce tree-based API alongside legacy |
| 2.1.0 | Add deprecation warnings on legacy APIs |
| 2.2.0 | Deprecation level → WARNING |
| 2.5.0 | Deprecation level → ERROR |
| 3.0.0 | Remove legacy APIs |

### Semantic Versioning Compliance

- **Minor versions** (2.x): Add warnings, no breaking changes
- **Major version** (3.0): Remove deprecated APIs

---

## Usage Examples

### Before Migration (Current Code)

```kotlin
class HomeScreen {
    @Composable
    fun Content(navigator: Navigator) {
        val entries by navigator.backStack.entries.collectAsState()
        val canGoBack = navigator.backStack.canGoBack.collectAsState()
        
        Button(onClick = { navigator.backStack.push(DetailDestination(id)) }) {
            Text("Go to Detail")
        }
        
        if (canGoBack.value) {
            BackButton(onClick = { navigator.backStack.pop() })
        }
    }
}
```

### After Migration (New Code)

```kotlin
class HomeScreen {
    @Composable
    fun Content(navigator: Navigator) {
        val state by navigator.state.collectAsState()
        val activeStack = state.activeStack()
        val canGoBack = activeStack?.canGoBack ?: false
        
        Button(onClick = { navigator.navigate(DetailDestination(id)) }) {
            Text("Go to Detail")
        }
        
        if (canGoBack) {
            BackButton(onClick = { navigator.navigateBack() })
        }
    }
}
```

### Intermediate Step (Using Compat Extensions)

```kotlin
class HomeScreen {
    @Composable
    fun Content(navigator: Navigator) {
        // Using deprecated extensions (will show warning)
        @Suppress("DEPRECATION")
        val entries = navigator.backStackEntries
        
        @Suppress("DEPRECATION")
        val canGoBack = navigator.canGoBack
        
        // Using new API for navigation
        Button(onClick = { navigator.navigate(DetailDestination(id)) }) {
            Text("Go to Detail")
        }
        
        if (canGoBack) {
            BackButton(onClick = { navigator.navigateBack() })
        }
    }
}
```

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../core/BackStackCompat.kt` | Create | Extension properties and conversion functions |
| `quo-vadis-core/.../core/NavigatorExtensions.kt` | Create | Deprecated extension functions |
| `quo-vadis-core/.../core/DeprecationMessages.kt` | Create | Centralized deprecation messages |
| `quo-vadis-core/.../core/LegacyNavigationApi.kt` | Create | Opt-in annotation for legacy APIs |

---

## Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| CORE-001 | Hard | NavNode hierarchy for conversion functions |
| CORE-003 | Hard | TreeNavigator for migration utilities |

---

## Acceptance Criteria

- [ ] `Navigator.activeStack` extension property works and shows deprecation warning
- [ ] `Navigator.backStackEntries` extension property works and shows deprecation warning
- [ ] `Navigator.backStackSize` extension property works and shows deprecation warning
- [ ] `Navigator.canGoBack` extension property works and shows deprecation warning
- [ ] `NavNode.toBackStack()` converts tree to linear list
- [ ] `List<BackStackEntry>.toStackNode()` converts list to tree
- [ ] `migrateToTreeNavigator()` creates TreeNavigator from DefaultNavigator
- [ ] All deprecations have proper `@ReplaceWith` hints
- [ ] IDE can auto-apply migration suggestions
- [ ] Deprecation messages are clear and actionable
- [ ] Unit tests verify backward compatibility
- [ ] Integration tests verify migration path
- [ ] KDoc documents migration patterns

---

## Testing Notes

See [CORE-006](./CORE-006-unit-tests.md) for comprehensive test requirements.

```kotlin
@Test
fun `legacy backStackEntries matches tree active path`() {
    val navigator = TreeNavigator()
    navigator.setStartDestination(homeDestination)
    navigator.navigate(profileDestination)
    navigator.navigate(settingsDestination)
    
    @Suppress("DEPRECATION")
    val legacyEntries = navigator.backStackEntries
    val treeScreens = navigator.state.value.activePathToLeaf()
        .filterIsInstance<ScreenNode>()
    
    assertEquals(legacyEntries.size, treeScreens.size)
    legacyEntries.forEachIndexed { index, entry ->
        assertEquals(entry.destination, treeScreens[index].destination)
    }
}

@Test
fun `toBackStack produces equivalent entries`() {
    val tree = StackNode(
        key = "root",
        parentKey = null,
        children = listOf(
            ScreenNode("s1", "root", homeDestination),
            ScreenNode("s2", "root", profileDestination)
        )
    )
    
    @Suppress("DEPRECATION")
    val backStack = tree.toBackStack()
    
    assertEquals(2, backStack.size)
    assertEquals(homeDestination, backStack[0].destination)
    assertEquals(profileDestination, backStack[1].destination)
}

@Test
fun `migrateToTreeNavigator preserves state`() {
    val legacy = DefaultNavigator()
    legacy.setStartDestination(homeDestination)
    legacy.navigate(profileDestination)
    
    @Suppress("DEPRECATION")
    val migrated = migrateToTreeNavigator(legacy)
    
    assertEquals(profileDestination, migrated.currentDestination.value)
    assertEquals(homeDestination, migrated.previousDestination.value)
}
```

---

## References

- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 5 "Migration Strategy"
- [INDEX](../INDEX.md) - Phase 1 Overview
- [CORE-001](./CORE-001-navnode-hierarchy.md) - NavNode definitions
- [CORE-003](./CORE-003-navigator-refactor.md) - TreeNavigator implementation
- [Kotlin Deprecation Guide](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecated/)
