# RENDER-008: Legacy Host Migration Wrappers

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-008 |
| **Task Name** | Legacy Host Migration Wrappers |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | RENDER-004 |
| **Blocked By** | RENDER-004 |
| **Blocks** | - |

---

## Overview

This task creates compatibility wrappers that allow existing code using `NavHost`, `GraphNavHost`, and `TabbedNavHost` to continue working while internally delegating to the new `QuoVadisHost`. This enables:

1. **Gradual migration** - No big-bang rewrite required
2. **Compile-time guidance** - Deprecation warnings point to new APIs
3. **Drop-in replacement** - Existing call sites work without modification
4. **Feature parity** - Legacy APIs support new features through QuoVadisHost

### Migration Strategy

```
Phase 1: Add deprecation warnings to existing hosts
         └── Users see warnings but code continues to work

Phase 2: Create wrapper implementations
         └── Existing hosts delegate to QuoVadisHost internally

Phase 3: Users migrate to QuoVadisHost
         └── Following ReplaceWith suggestions in deprecations

Phase 4: Remove legacy hosts (2+ major versions later)
         └── Only QuoVadisHost remains
```

### API Surface

| Legacy API | Replacement | Status |
|------------|-------------|--------|
| `NavHost` | `QuoVadisHost` | Deprecated |
| `GraphNavHost` | `QuoVadisHost` with graph | Deprecated |
| `TabbedNavHost` | `QuoVadisHost` with TabNode | Deprecated |
| `Navigator` (old) | `Navigator` (new) | Updated |

---

## File Locations

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/NavHostCompat.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/GraphNavHostCompat.kt
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabbedNavHostCompat.kt
```

---

## Implementation

### NavHost Compatibility Wrapper

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Legacy NavHost wrapper for backward compatibility.
 * 
 * This composable provides the same API as the original NavHost while
 * internally using QuoVadisHost. It converts the linear backstack model
 * to the new NavNode tree structure.
 * 
 * ## Migration Guide
 * 
 * Replace:
 * ```kotlin
 * NavHost(
 *     navigator = navigator,
 *     modifier = Modifier.fillMaxSize()
 * ) { destination ->
 *     ScreenContent(destination)
 * }
 * ```
 * 
 * With:
 * ```kotlin
 * QuoVadisHost(
 *     navigator = treeNavigator,
 *     modifier = Modifier.fillMaxSize()
 * ) { destination ->
 *     ScreenContent(destination)
 * }
 * ```
 * 
 * @see QuoVadisHost
 */
@Deprecated(
    message = "NavHost is deprecated. Use QuoVadisHost for unified navigation rendering.",
    replaceWith = ReplaceWith(
        expression = "QuoVadisHost(navigator.toTreeNavigator(), modifier, content)",
        imports = ["com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost"]
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun NavHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeIn() + slideInHorizontally { it },
    exitTransition: ExitTransition = fadeOut() + slideOutHorizontally { -it / 3 },
    popEnterTransition: EnterTransition = fadeIn() + slideInHorizontally { -it / 3 },
    popExitTransition: ExitTransition = fadeOut() + slideOutHorizontally { it },
    content: @Composable (Destination) -> Unit
) {
    // Convert legacy Navigator to tree-based state
    val treeState = remember(navigator) {
        LegacyNavigatorAdapter(navigator)
    }
    
    // Create animation registry from legacy transitions
    val animationRegistry = remember(enterTransition, exitTransition, popEnterTransition, popExitTransition) {
        AnimationRegistry {
            registerDefault(
                direction = TransitionDirection.FORWARD,
                spec = SurfaceAnimationSpec(enter = enterTransition, exit = exitTransition)
            )
            registerDefault(
                direction = TransitionDirection.BACKWARD,
                spec = SurfaceAnimationSpec(enter = popEnterTransition, exit = popExitTransition)
            )
        }
    }
    
    // Delegate to QuoVadisHost
    QuoVadisHost(
        navigator = treeState.treeNavigator,
        modifier = modifier,
        animationRegistry = animationRegistry
    ) { destination ->
        content(destination)
    }
}

/**
 * Adapter that converts legacy Navigator to tree-based Navigator.
 */
internal class LegacyNavigatorAdapter(
    private val legacyNavigator: Navigator
) {
    val treeNavigator: TreeNavigator = TreeNavigator(
        initialState = convertToTree(legacyNavigator.backStack.value)
    )
    
    init {
        // Sync legacy navigator changes to tree navigator
        // This is a simplified implementation
    }
    
    private fun convertToTree(backStack: List<Destination>): NavNode {
        if (backStack.isEmpty()) {
            return StackNode(
                key = "root",
                parentKey = null,
                children = emptyList()
            )
        }
        
        val screenNodes = backStack.mapIndexed { index, destination ->
            ScreenNode(
                key = "screen-$index",
                parentKey = "root",
                destination = destination
            )
        }
        
        return StackNode(
            key = "root",
            parentKey = null,
            children = screenNodes
        )
    }
}
```

### GraphNavHost Compatibility Wrapper

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Legacy GraphNavHost wrapper for backward compatibility.
 * 
 * This composable provides the same API as the original GraphNavHost while
 * internally using QuoVadisHost. It uses the NavigationGraph for type-safe
 * content resolution.
 * 
 * ## Migration Guide
 * 
 * Replace:
 * ```kotlin
 * GraphNavHost(
 *     navigator = navigator,
 *     graph = AppNavGraph,
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 * 
 * With:
 * ```kotlin
 * QuoVadisHost(
 *     navigator = treeNavigator,
 *     graph = AppNavGraph,
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 * 
 * @see QuoVadisHost
 */
@Deprecated(
    message = "GraphNavHost is deprecated. Use QuoVadisHost with a NavigationGraph.",
    replaceWith = ReplaceWith(
        expression = "QuoVadisHost(navigator.toTreeNavigator(), graph, modifier)",
        imports = ["com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost"]
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun GraphNavHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier,
    enterTransition: EnterTransition = fadeIn() + slideInHorizontally { it },
    exitTransition: ExitTransition = fadeOut() + slideOutHorizontally { -it / 3 },
    popEnterTransition: EnterTransition = fadeIn() + slideInHorizontally { -it / 3 },
    popExitTransition: ExitTransition = fadeOut() + slideOutHorizontally { it }
) {
    // Convert legacy Navigator to tree-based state
    val treeState = remember(navigator) {
        LegacyNavigatorAdapter(navigator)
    }
    
    // Create animation registry from legacy transitions
    val animationRegistry = remember(enterTransition, exitTransition, popEnterTransition, popExitTransition) {
        AnimationRegistry {
            registerDefault(
                direction = TransitionDirection.FORWARD,
                spec = SurfaceAnimationSpec(enter = enterTransition, exit = exitTransition)
            )
            registerDefault(
                direction = TransitionDirection.BACKWARD,
                spec = SurfaceAnimationSpec(enter = popEnterTransition, exit = popExitTransition)
            )
        }
    }
    
    // Delegate to QuoVadisHost with graph
    QuoVadisHost(
        navigator = treeState.treeNavigator,
        graph = graph,
        modifier = modifier,
        animationRegistry = animationRegistry
    )
}
```

### TabbedNavHost Compatibility Wrapper

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Legacy TabbedNavHost wrapper for backward compatibility.
 * 
 * This composable provides the same API as the original TabbedNavHost while
 * internally using QuoVadisHost with a TabNode structure.
 * 
 * ## Migration Guide
 * 
 * Replace:
 * ```kotlin
 * TabbedNavHost(
 *     tabNavigator = tabNavigator,
 *     tabs = listOf(homeTab, profileTab, settingsTab),
 *     modifier = Modifier.fillMaxSize()
 * ) { tab, navigator ->
 *     TabContent(tab, navigator)
 * }
 * ```
 * 
 * With:
 * ```kotlin
 * QuoVadisHost(
 *     navigator = treeNavigator, // Contains TabNode
 *     modifier = Modifier.fillMaxSize()
 * ) { destination ->
 *     DestinationContent(destination)
 * }
 * ```
 * 
 * @see QuoVadisHost
 */
@Deprecated(
    message = "TabbedNavHost is deprecated. Use QuoVadisHost with a TabNode in the navigation tree.",
    replaceWith = ReplaceWith(
        expression = "QuoVadisHost(tabNavigator.toTreeNavigator(), modifier, content)",
        imports = ["com.jermey.quo.vadis.core.navigation.compose.QuoVadisHost"]
    ),
    level = DeprecationLevel.WARNING
)
@Composable
fun <T : TabDefinition> TabbedNavHost(
    tabNavigator: TabNavigatorState<T>,
    tabs: List<T>,
    modifier: Modifier = Modifier,
    tabBar: @Composable (selectedTab: T, onTabSelected: (T) -> Unit) -> Unit = { _, _ -> },
    enterTransition: EnterTransition = fadeIn(),
    exitTransition: ExitTransition = fadeOut(),
    content: @Composable (tab: T, navigator: Navigator) -> Unit
) {
    // Convert TabNavigatorState to tree-based Navigator
    val treeNavigator = remember(tabNavigator, tabs) {
        convertTabNavigatorToTree(tabNavigator, tabs)
    }
    
    // Observe tab changes and sync
    val currentTab by tabNavigator.currentTab.collectAsState()
    
    LaunchedEffect(currentTab) {
        val tabIndex = tabs.indexOf(currentTab)
        if (tabIndex >= 0) {
            treeNavigator.switchTab(tabIndex)
        }
    }
    
    // Create animation registry
    val animationRegistry = remember(enterTransition, exitTransition) {
        AnimationRegistry {
            registerDefault(spec = SurfaceAnimationSpec(enter = enterTransition, exit = exitTransition))
        }
    }
    
    Column(modifier = modifier) {
        // Tab bar (legacy API support)
        tabBar(currentTab, { tab ->
            tabNavigator.switchTo(tab)
        })
        
        // Content via QuoVadisHost
        QuoVadisHost(
            navigator = treeNavigator,
            modifier = Modifier.weight(1f),
            animationRegistry = animationRegistry
        ) { destination ->
            // Find which tab this destination belongs to
            val tabIndex = findTabForDestination(treeNavigator.state.value, destination)
            val tab = tabs.getOrNull(tabIndex) ?: return@QuoVadisHost
            val tabNavigatorInstance = tabNavigator.navigatorFor(tab)
            
            content(tab, tabNavigatorInstance)
        }
    }
}

/**
 * Converts a TabNavigatorState to a tree-based Navigator with TabNode.
 */
private fun <T : TabDefinition> convertTabNavigatorToTree(
    tabNavigator: TabNavigatorState<T>,
    tabs: List<T>
): TreeNavigator {
    val tabStacks = tabs.mapIndexed { index, tab ->
        val navigator = tabNavigator.navigatorFor(tab)
        val screenNodes = navigator.backStack.value.mapIndexed { screenIndex, destination ->
            ScreenNode(
                key = "tab-$index-screen-$screenIndex",
                parentKey = "tab-$index-stack",
                destination = destination
            )
        }
        
        StackNode(
            key = "tab-$index-stack",
            parentKey = "tabs",
            children = screenNodes
        )
    }
    
    val tabNode = TabNode(
        key = "tabs",
        parentKey = "root",
        stacks = tabStacks,
        activeStackIndex = tabs.indexOf(tabNavigator.currentTab.value)
    )
    
    val rootStack = StackNode(
        key = "root",
        parentKey = null,
        children = listOf(tabNode)
    )
    
    return TreeNavigator(initialState = rootStack)
}

/**
 * Finds which tab index a destination belongs to.
 */
private fun findTabForDestination(root: NavNode, destination: Destination): Int {
    // Implementation to traverse tree and find tab index
    // This is a simplified version
    return 0
}
```

### Deprecation Annotations

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import kotlin.annotation.AnnotationTarget.*

/**
 * Marks APIs that are deprecated in favor of QuoVadisHost.
 */
@Target(CLASS, FUNCTION, PROPERTY, TYPEALIAS)
@Retention(AnnotationRetention.BINARY)
@MustBeDocumented
annotation class DeprecatedNavigation(
    val message: String,
    val replaceWith: String
)
```

### Extension Functions for Migration

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Converts a legacy Navigator to a tree-based Navigator.
 * 
 * This extension simplifies migration by converting the linear backstack
 * to a StackNode tree structure.
 * 
 * ## Usage
 * 
 * ```kotlin
 * val legacyNavigator: Navigator = ...
 * val treeNavigator = legacyNavigator.toTreeNavigator()
 * 
 * QuoVadisHost(navigator = treeNavigator) { /* ... */ }
 * ```
 */
@Deprecated(
    message = "Use TreeNavigator directly for new code",
    level = DeprecationLevel.WARNING
)
fun Navigator.toTreeNavigator(): TreeNavigator {
    val backStack = this.backStack.value
    
    val screenNodes = backStack.mapIndexed { index, destination ->
        ScreenNode(
            key = "screen-$index",
            parentKey = "root",
            destination = destination
        )
    }
    
    val rootStack = StackNode(
        key = "root",
        parentKey = null,
        children = screenNodes
    )
    
    return TreeNavigator(initialState = rootStack)
}

/**
 * Converts a TabNavigatorState to a tree-based Navigator with TabNode.
 */
@Deprecated(
    message = "Use TreeNavigator with TabNode directly for new code",
    level = DeprecationLevel.WARNING
)
fun <T : TabDefinition> TabNavigatorState<T>.toTreeNavigator(
    tabs: List<T>
): TreeNavigator {
    return convertTabNavigatorToTree(this, tabs)
}
```

---

## Migration Guide Documentation

```kotlin
/**
 * # Migration Guide: Legacy Hosts to QuoVadisHost
 * 
 * ## Overview
 * 
 * Quo Vadis 2.0 introduces QuoVadisHost as the unified navigation host,
 * replacing NavHost, GraphNavHost, and TabbedNavHost. This guide helps
 * you migrate existing code.
 * 
 * ## Step 1: Update Navigator
 * 
 * Old:
 * ```kotlin
 * val navigator = rememberNavigator(initialDestination = HomeDestination)
 * ```
 * 
 * New:
 * ```kotlin
 * val navigator = rememberTreeNavigator(
 *     initialState = StackNode("root", null, listOf(
 *         ScreenNode("home", "root", HomeDestination)
 *     ))
 * )
 * ```
 * 
 * Or use the migration helper:
 * ```kotlin
 * val legacyNavigator = rememberNavigator(HomeDestination)
 * val treeNavigator = legacyNavigator.toTreeNavigator()
 * ```
 * 
 * ## Step 2: Replace NavHost
 * 
 * Old:
 * ```kotlin
 * NavHost(navigator = navigator) { destination ->
 *     when (destination) {
 *         is HomeDestination -> HomeScreen()
 *         is ProfileDestination -> ProfileScreen(destination.userId)
 *     }
 * }
 * ```
 * 
 * New:
 * ```kotlin
 * QuoVadisHost(navigator = treeNavigator) { destination ->
 *     when (destination) {
 *         is HomeDestination -> HomeScreen()
 *         is ProfileDestination -> ProfileScreen(destination.userId)
 *     }
 * }
 * ```
 * 
 * ## Step 3: Replace TabbedNavHost
 * 
 * Old:
 * ```kotlin
 * TabbedNavHost(
 *     tabNavigator = tabNavigator,
 *     tabs = listOf(HomeTab, ProfileTab)
 * ) { tab, navigator ->
 *     NavHost(navigator) { destination ->
 *         TabContent(tab, destination)
 *     }
 * }
 * ```
 * 
 * New:
 * ```kotlin
 * // Create tree with TabNode
 * val treeNavigator = rememberTreeNavigator(
 *     initialState = StackNode("root", null, listOf(
 *         TabNode("tabs", "root", listOf(
 *             StackNode("home-stack", "tabs", listOf(HomeScreen())),
 *             StackNode("profile-stack", "tabs", listOf(ProfileScreen()))
 *         ))
 *     ))
 * )
 * 
 * // Single QuoVadisHost handles everything
 * QuoVadisHost(navigator = treeNavigator) { destination ->
 *     DestinationContent(destination)
 * }
 * ```
 * 
 * ## Step 4: Update Navigation Calls
 * 
 * Old:
 * ```kotlin
 * navigator.push(ProfileDestination(userId))
 * navigator.pop()
 * tabNavigator.switchTo(ProfileTab)
 * ```
 * 
 * New:
 * ```kotlin
 * treeNavigator.push(ProfileDestination(userId))
 * treeNavigator.pop()
 * treeNavigator.switchTab(1) // Or use TabNode key
 * ```
 * 
 * ## Benefits of Migration
 * 
 * 1. **Unified rendering** - One host for all navigation patterns
 * 2. **Shared elements** - Work across tabs and stacks
 * 3. **Predictive back** - Consistent gesture support everywhere
 * 4. **Deep linking** - Tree path reconstruction
 * 5. **State preservation** - Automatic with SaveableStateHolder
 */
```

---

## Implementation Steps

### Step 1: Create Compat Files

1. Create `NavHostCompat.kt` with deprecated NavHost
2. Create `GraphNavHostCompat.kt` with deprecated GraphNavHost
3. Create `TabbedNavHostCompat.kt` with deprecated TabbedNavHost

### Step 2: Implement Adapters

1. Create `LegacyNavigatorAdapter` class
2. Implement backstack to tree conversion
3. Handle navigation event synchronization

### Step 3: Add Deprecation Annotations

1. Add `@Deprecated` to all legacy composables
2. Include `ReplaceWith` for IDE quick-fix support
3. Set appropriate deprecation levels

### Step 4: Extension Functions

1. Create `toTreeNavigator()` for Navigator
2. Create `toTreeNavigator()` for TabNavigatorState
3. Document usage patterns

### Step 5: Documentation

1. Create migration guide
2. Add examples for common scenarios
3. Document breaking changes

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/NavHostCompat.kt` | Create | NavHost wrapper |
| `quo-vadis-core/.../compose/GraphNavHostCompat.kt` | Create | GraphNavHost wrapper |
| `quo-vadis-core/.../compose/TabbedNavHostCompat.kt` | Create | TabbedNavHost wrapper |
| `quo-vadis-core/.../compose/LegacyNavigatorAdapter.kt` | Create | Adapter classes |
| `quo-vadis-core/.../compose/MigrationExtensions.kt` | Create | Extension functions |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-004 (QuoVadisHost) | Hard | Must complete first |
| CORE-005 (Backward Compat) | Soft | Coordinates deprecation strategy |

---

## Acceptance Criteria

- [ ] `NavHost` deprecated wrapper compiles and functions
- [ ] `GraphNavHost` deprecated wrapper compiles and functions
- [ ] `TabbedNavHost` deprecated wrapper compiles and functions
- [ ] All wrappers internally delegate to `QuoVadisHost`
- [ ] `@Deprecated` annotations include `ReplaceWith`
- [ ] IDE shows deprecation warnings with fix suggestions
- [ ] `toTreeNavigator()` extension works for Navigator
- [ ] `toTreeNavigator()` extension works for TabNavigatorState
- [ ] Existing demo app compiles with deprecation warnings
- [ ] Migration guide documentation complete
- [ ] No breaking changes for existing API consumers
- [ ] Unit tests verify wrapper functionality
- [ ] Integration tests verify migration path

---

## Testing Notes

### Compilation Tests

```kotlin
@Test
fun `NavHost wrapper compiles with same signature`() {
    // Verify existing call sites compile
    @Composable
    fun TestNavHost() {
        val navigator = rememberNavigator(HomeDestination)
        
        // Should compile with deprecation warning
        NavHost(navigator = navigator) { destination ->
            Text("Content")
        }
    }
}
```

### Functional Tests

```kotlin
@Test
fun `NavHost wrapper renders correctly`() {
    val navigator = Navigator(HomeDestination)
    
    composeTestRule.setContent {
        @Suppress("DEPRECATION")
        NavHost(navigator = navigator) { destination ->
            when (destination) {
                is HomeDestination -> Text("Home")
            }
        }
    }
    
    composeTestRule.onNodeWithText("Home").assertIsDisplayed()
}

@Test
fun `TabbedNavHost wrapper preserves tab state`() {
    // Test that migrated TabbedNavHost still preserves tab states
}
```

### Migration Tests

```kotlin
@Test
fun `toTreeNavigator converts backstack correctly`() {
    val navigator = Navigator(listOf(HomeDestination, ProfileDestination))
    
    val treeNavigator = navigator.toTreeNavigator()
    val root = treeNavigator.state.value as StackNode
    
    assertEquals(2, root.children.size)
    assertEquals(HomeDestination, (root.children[0] as ScreenNode).destination)
    assertEquals(ProfileDestination, (root.children[1] as ScreenNode).destination)
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost implementation
- [MIG-003](../phase5-migration/MIG-003-host-adapter.md) - Host adapter utilities
- [Kotlin Deprecation](https://kotlinlang.org/docs/deprecation.html) - Deprecation best practices
