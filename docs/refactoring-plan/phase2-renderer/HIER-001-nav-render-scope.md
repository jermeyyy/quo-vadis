# HIER-001: NavRenderScope Interface

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-001 |
| **Task Name** | Create NavRenderScope Interface |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-016, HIER-017, HIER-018, HIER-021, HIER-022, HIER-024 |

---

## Overview

The `NavRenderScope` interface is the foundational context object that provides all renderers with access to navigation state, caching, animation coordination, and composable registries. It serves as the primary dependency injection mechanism for the hierarchical rendering system.

### Purpose

- Provide a unified context for all node renderers
- Enable dependency injection without prop drilling
- Support `CompositionLocal` access for nested composables
- Decouple renderers from concrete implementations

### Design Decisions

1. **Interface-based**: Allows for fake implementations in testing
2. **@Stable**: Ensures Compose skipping optimization works correctly
3. **CompositionLocal**: Enables deep access without explicit passing

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/NavRenderScope.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.hierarchical

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.cache.ComposableCache
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.WrapperRegistry

/**
 * Provides context to all hierarchical navigation renderers.
 *
 * This scope is the central dependency injection point for the rendering system,
 * giving renderers access to navigation state, caching, animations, and registries.
 *
 * ## Usage
 *
 * Renderers receive this scope and use it to:
 * - Access the navigator for state observation
 * - Cache composables for animation preservation
 * - Coordinate animations across navigation transitions
 * - Handle predictive back gestures
 * - Resolve destinations to composables via registries
 *
 * ## Example
 *
 * ```kotlin
 * @Composable
 * internal fun ScreenRenderer(
 *     node: ScreenNode,
 *     scope: NavRenderScope,
 *     modifier: Modifier
 * ) {
 *     scope.cache.CachedEntry(node.key) {
 *         scope.screenRegistry.Content(
 *             destination = node.destination,
 *             navigator = scope.navigator,
 *             sharedTransitionScope = scope.sharedTransitionScope,
 *             animatedVisibilityScope = LocalAnimatedVisibilityScope.current
 *         )
 *     }
 * }
 * ```
 *
 * @see NavRenderScopeImpl
 * @see LocalNavRenderScope
 */
@Stable
interface NavRenderScope {
    
    /**
     * The navigator managing navigation state.
     *
     * Used by renderers to:
     * - Observe current navigation state
     * - Perform navigation operations
     * - Check navigation capabilities (canGoBack, etc.)
     */
    val navigator: Navigator
    
    /**
     * Cache for preserving composable state across recompositions.
     *
     * Renderers use this to wrap content that should survive
     * navigation transitions, enabling smooth animations.
     */
    val cache: ComposableCache
    
    /**
     * Coordinator for resolving and managing navigation transitions.
     *
     * Provides transition animations based on:
     * - Destination annotations (@Transition)
     * - Navigation direction (forward/back)
     * - Node type (stack, tab, pane)
     */
    val animationCoordinator: AnimationCoordinator
    
    /**
     * Controller for predictive back gesture handling.
     *
     * Tracks gesture state and progress, enabling
     * renderers to apply appropriate transforms.
     */
    val predictiveBackController: PredictiveBackController
    
    /**
     * Shared transition scope for cross-screen shared elements.
     *
     * Provided by [SharedTransitionLayout] at the root,
     * enabling shared element transitions across any
     * screens in the navigation hierarchy.
     */
    val sharedTransitionScope: SharedTransitionScope
    
    /**
     * Registry mapping destinations to screen composables.
     *
     * KSP-generated from @Screen annotations.
     */
    val screenRegistry: ScreenRegistry
    
    /**
     * Registry mapping tab/pane nodes to wrapper composables.
     *
     * KSP-generated from @TabWrapper and @PaneWrapper annotations.
     */
    val wrapperRegistry: WrapperRegistry
    
    /**
     * Provides an [AnimatedVisibilityScope] to content.
     *
     * Used by [AnimatedNavContent] to provide the visibility scope
     * required by shared element transitions and animated content.
     *
     * @param visibilityScope The scope from AnimatedContent/AnimatedVisibility
     * @param content The composable content to wrap
     */
    @Composable
    fun withAnimatedVisibilityScope(
        visibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    )
}

/**
 * CompositionLocal for accessing [NavRenderScope] in nested composables.
 *
 * This allows deeply nested composables to access the render scope
 * without explicit parameter passing.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun SomeNestedComponent() {
 *     val scope = LocalNavRenderScope.current
 *     // Use scope...
 * }
 * ```
 *
 * @throws IllegalStateException if accessed outside of [HierarchicalQuoVadisHost]
 */
val LocalNavRenderScope = compositionLocalOf<NavRenderScope> {
    error("NavRenderScope not provided. Ensure you are within HierarchicalQuoVadisHost.")
}

/**
 * CompositionLocal for the current [AnimatedVisibilityScope].
 *
 * Provided by [NavRenderScope.withAnimatedVisibilityScope] during
 * animated content transitions.
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
```

---

## Integration Points

### Providers

- **HierarchicalQuoVadisHost**: Creates and provides `NavRenderScopeImpl`
- **AnimatedNavContent**: Provides `AnimatedVisibilityScope` via `withAnimatedVisibilityScope`

### Consumers

- **NavTreeRenderer**: Receives scope, passes to specific renderers
- **ScreenRenderer**: Uses screenRegistry, cache, navigator
- **StackRenderer**: Uses animationCoordinator, predictiveBackController
- **TabRenderer**: Uses wrapperRegistry, cache
- **PaneRenderer**: Uses wrapperRegistry, cache

### Related Components

| Component | Relationship |
|-----------|--------------|
| `NavRenderScopeImpl` | Default implementation (HIER-024) |
| `FakeNavRenderScope` | Test fake (HIER-026) |
| `ComposableCache` | Provided via cache property (HIER-007) |
| `AnimationCoordinator` | Provided via animationCoordinator (HIER-005) |

---

## Testing Requirements

### Unit Tests

```kotlin
class NavRenderScopeTest {
    
    @Test
    fun `LocalNavRenderScope throws when not provided`() {
        assertFailsWith<IllegalStateException> {
            runComposeTest {
                LocalNavRenderScope.current
            }
        }
    }
    
    @Test
    fun `LocalNavRenderScope returns scope when provided`() = runComposeTest {
        val fakeScope = FakeNavRenderScope()
        
        setContent {
            CompositionLocalProvider(LocalNavRenderScope provides fakeScope) {
                val scope = LocalNavRenderScope.current
                assertEquals(fakeScope, scope)
            }
        }
    }
    
    @Test
    fun `LocalAnimatedVisibilityScope is null by default`() = runComposeTest {
        setContent {
            val scope = LocalAnimatedVisibilityScope.current
            assertNull(scope)
        }
    }
}
```

### Integration Tests

- Verify scope is accessible throughout navigation hierarchy
- Verify AnimatedVisibilityScope is correctly propagated
- Verify all properties are non-null when provided

---

## Acceptance Criteria

- [ ] `NavRenderScope` interface defined with all required properties
- [ ] `@Stable` annotation applied for Compose optimization
- [ ] `LocalNavRenderScope` CompositionLocal created with error default
- [ ] `LocalAnimatedVisibilityScope` CompositionLocal created with null default
- [ ] `withAnimatedVisibilityScope()` method declared
- [ ] Full KDoc documentation on interface, properties, and compositionLocals
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should `withAnimatedVisibilityScope` be a suspend function for animation coordination?
2. Should we add a `transitionRegistry` property or keep it internal to `AnimationCoordinator`?

### Design Rationale

- **Stable annotation**: Required for Compose to properly skip recomposition when scope hasn't changed
- **Composition locals**: Enable access without prop drilling, critical for deeply nested wrappers
- **Error default**: Fail-fast behavior prevents silent null access in production
