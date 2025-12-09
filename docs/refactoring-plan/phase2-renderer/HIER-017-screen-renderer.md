# HIER-017: ScreenRenderer

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-017 |
| **Task Name** | Create ScreenRenderer |
| **Phase** | Phase 3: Renderer Implementation |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | HIER-016 (NavTreeRenderer), HIER-007 (ComposableCache) |
| **Blocked By** | HIER-007 |
| **Blocks** | HIER-024 |

---

## Overview

The `ScreenRenderer` renders leaf `ScreenNode` destinations. It uses the cache for state preservation and invokes the screen registry to resolve the destination to its composable.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/ScreenRenderer.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.compose.LocalBackStackEntry
import com.jermey.quo.vadis.core.navigation.core.ScreenNode

/**
 * Renders a leaf [ScreenNode] destination.
 *
 * Uses the [ComposableCache] for state preservation and resolves
 * the destination to its composable via the [ScreenRegistry].
 *
 * @param node The screen node to render
 * @param scope Rendering context
 * @param modifier Modifier to apply
 */
@Composable
internal fun ScreenRenderer(
    node: ScreenNode,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    scope.cache.CachedEntry(key = node.key) {
        // Provide backstack entry for screen to access
        CompositionLocalProvider(
            LocalBackStackEntry provides node.toBackStackEntry()
        ) {
            // Get current animated visibility scope
            val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
            
            // Render the screen content
            scope.screenRegistry.Content(
                destination = node.destination,
                navigator = scope.navigator,
                sharedTransitionScope = scope.sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    }
}

/**
 * Extension to convert ScreenNode to BackStackEntry.
 */
internal fun ScreenNode.toBackStackEntry(): BackStackEntry {
    return BackStackEntry(
        key = key,
        destination = destination,
        arguments = destination.arguments
    )
}

/**
 * Entry in the navigation backstack.
 */
data class BackStackEntry(
    val key: String,
    val destination: Destination,
    val arguments: Map<String, Any?> = emptyMap()
)

/**
 * CompositionLocal for accessing current backstack entry.
 */
val LocalBackStackEntry = compositionLocalOf<BackStackEntry?> { null }
```

---

## Integration Points

- **NavTreeRenderer**: Calls ScreenRenderer for ScreenNode
- **ComposableCache**: Wraps content for state preservation
- **ScreenRegistry**: Resolves destination to composable
- **SharedTransitionScope**: Passed through for shared elements

---

## Acceptance Criteria

- [ ] `ScreenRenderer` composable with node, scope, modifier
- [ ] Uses `cache.CachedEntry` with node.key
- [ ] Provides `LocalBackStackEntry`
- [ ] Invokes `screenRegistry.Content` with all parameters
- [ ] `BackStackEntry` data class
- [ ] `toBackStackEntry()` extension function
- [ ] KDoc documentation
