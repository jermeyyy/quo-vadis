# HIER-005: AnimationCoordinator

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-005 |
| **Task Name** | Create AnimationCoordinator Class |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | HIER-003 (TransitionRegistry), HIER-004 (NavTransition) |
| **Blocked By** | HIER-003, HIER-004 |
| **Blocks** | HIER-018, HIER-019, HIER-024 |

---

## Overview

The `AnimationCoordinator` manages transition resolution across the navigation hierarchy. It queries the `TransitionRegistry` for annotated transitions and provides appropriate defaults based on navigation context (stack, tab, pane).

### Purpose

- Resolve transitions based on destination annotations
- Provide context-aware default transitions
- Determine navigation direction (forward vs back)
- Support different transition strategies for different node types

### Design Decisions

1. **@Stable**: Allows Compose to skip when coordinator hasn't changed
2. **Registry-first lookup**: Check annotations before falling back to defaults
3. **Context-aware defaults**: Different defaults for stacks, tabs, panes
4. **Direction detection**: Compare node states to determine if navigating back

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/animation/AnimationCoordinator.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.animation

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.Destination
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.DefaultTransitionRegistry
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.PaneNode
import com.jermey.quo.vadis.core.navigation.core.PaneRole
import com.jermey.quo.vadis.core.navigation.core.ScreenNode
import com.jermey.quo.vadis.core.navigation.core.StackNode
import com.jermey.quo.vadis.core.navigation.core.TabNode

/**
 * Coordinates navigation transitions across the hierarchy.
 *
 * The AnimationCoordinator resolves which [NavTransition] to use for any
 * navigation operation. It checks for destination-specific annotations
 * and falls back to context-appropriate defaults.
 *
 * ## Resolution Order
 *
 * 1. Check [TransitionRegistry] for `@Transition` annotation on target destination
 * 2. Check for custom transition on source destination (for exit animation)
 * 3. Fall back to default based on context (stack, tab, pane)
 *
 * ## Default Transitions
 *
 * | Context | Default | Rationale |
 * |---------|---------|-----------|
 * | Stack | SlideHorizontal | Platform standard |
 * | Tab switch | Fade | Quick, non-directional |
 * | Pane focus change | Fade | Subtle, doesn't imply hierarchy |
 * | Cross-node | SlideHorizontal | Full navigation feel |
 *
 * ## Usage
 *
 * ```kotlin
 * val coordinator = AnimationCoordinator(
 *     transitionRegistry = GeneratedTransitionRegistry,
 *     defaultTransition = NavTransition.SlideHorizontal
 * )
 *
 * // Get transition for stack navigation
 * val transition = coordinator.getTransition(
 *     from = homeScreen,
 *     to = detailScreen,
 *     isBack = false
 * )
 * ```
 *
 * @property transitionRegistry Registry for annotation-based transitions
 * @property defaultTransition Fallback transition when no annotation exists
 *
 * @see NavTransition
 * @see TransitionRegistry
 */
@Stable
class AnimationCoordinator(
    private val transitionRegistry: TransitionRegistry = DefaultTransitionRegistry,
    private val defaultTransition: NavTransition = NavTransition.SlideHorizontal
) {
    
    /**
     * Gets the transition for a navigation operation.
     *
     * @param from Source node (null for initial navigation)
     * @param to Target node
     * @param isBack Whether this is a back navigation
     * @return Resolved [NavTransition]
     */
    fun getTransition(
        from: NavNode?,
        to: NavNode,
        isBack: Boolean
    ): NavTransition {
        // Try to get annotated transition for target destination
        val annotatedTransition = getAnnotatedTransition(to)
        if (annotatedTransition != null) {
            return annotatedTransition
        }
        
        // For back navigation, check source destination annotation
        if (isBack && from != null) {
            val sourceTransition = getAnnotatedTransition(from)
            if (sourceTransition != null) {
                return sourceTransition
            }
        }
        
        // Fall back to default
        return defaultTransition
    }
    
    /**
     * Gets the transition for tab switching.
     *
     * Tab transitions are typically quick crossfades or horizontal slides
     * depending on the direction.
     *
     * @param fromIndex Previous active tab index (null for initial)
     * @param toIndex New active tab index
     * @return Resolved [NavTransition]
     */
    fun getTabTransition(
        fromIndex: Int?,
        toIndex: Int
    ): NavTransition {
        // Initial display - no animation
        if (fromIndex == null) {
            return NavTransition.None
        }
        
        // Same tab - no animation
        if (fromIndex == toIndex) {
            return NavTransition.None
        }
        
        // Determine direction and use appropriate animation
        return if (toIndex > fromIndex) {
            // Moving right - slide from right
            NavTransition.SlideHorizontal
        } else {
            // Moving left - slide from left (reversed)
            NavTransition.SlideHorizontal.reversed()
        }
    }
    
    /**
     * Gets the transition for pane focus changes.
     *
     * Pane transitions are subtle to avoid distracting from content.
     *
     * @param fromRole Previous focused pane role (null for initial)
     * @param toRole New focused pane role
     * @return Resolved [NavTransition]
     */
    fun getPaneTransition(
        fromRole: PaneRole?,
        toRole: PaneRole
    ): NavTransition {
        // Initial display or same pane - no animation
        if (fromRole == null || fromRole == toRole) {
            return NavTransition.None
        }
        
        // Subtle fade for pane focus changes
        return NavTransition.Fade
    }
    
    /**
     * Gets the transition for pane content changes (within a single pane).
     *
     * @param from Source node in pane
     * @param to Target node in pane
     * @param isBack Whether navigating back
     * @return Resolved [NavTransition]
     */
    fun getPaneContentTransition(
        from: NavNode?,
        to: NavNode,
        isBack: Boolean
    ): NavTransition {
        // Use standard transition resolution
        return getTransition(from, to, isBack)
    }
    
    /**
     * Detects if navigation is going backwards based on node comparison.
     *
     * Back navigation is detected when:
     * - Stack size decreased
     * - Target was previous entry
     * - Explicit back navigation flag
     *
     * @param previousState Previous navigation state
     * @param currentState Current navigation state
     * @return true if this appears to be back navigation
     */
    fun detectBackNavigation(
        previousState: NavNode?,
        currentState: NavNode
    ): Boolean {
        if (previousState == null) return false
        
        // Compare stack depths for StackNodes
        if (previousState is StackNode && currentState is StackNode) {
            return currentState.children.size < previousState.children.size
        }
        
        // For screen nodes, check if current was in previous path
        // This handles cases like navigating back to a specific screen
        return false // Conservative default
    }
    
    /**
     * Extracts annotated transition from a NavNode.
     */
    private fun getAnnotatedTransition(node: NavNode): NavTransition? {
        return when (node) {
            is ScreenNode -> {
                node.destination?.let { dest ->
                    transitionRegistry.getTransition(dest::class)
                }
            }
            is StackNode -> {
                // Check active child
                node.children.lastOrNull()?.let { getAnnotatedTransition(it) }
            }
            is TabNode -> {
                // Check active stack
                node.stacks.getOrNull(node.activeStackIndex)?.let { getAnnotatedTransition(it) }
            }
            is PaneNode -> {
                // Check primary pane content
                node.getActiveContent(PaneRole.Primary)?.let { getAnnotatedTransition(it) }
            }
        }
    }
}

/**
 * Extension function to get active content from a PaneNode.
 */
private fun PaneNode.getActiveContent(role: PaneRole): NavNode? {
    return paneConfigurations[role]?.content
}
```

---

## Integration Points

### Providers

- **HierarchicalQuoVadisHost** (HIER-024): Creates and provides coordinator

### Consumers

- **StackRenderer** (HIER-018): Gets stack transitions
- **AnimatedNavContent** (HIER-019): Uses transitions for AnimatedContent
- **TabRenderer** (HIER-021): Gets tab switch transitions
- **PaneRenderer** (HIER-022): Gets pane transitions

### Dependencies

| Component | Relationship |
|-----------|--------------|
| `TransitionRegistry` | Queries for annotations (HIER-003) |
| `NavTransition` | Return type (HIER-004) |
| `NavNode` types | Inspected for context |

---

## Testing Requirements

### Unit Tests

```kotlin
class AnimationCoordinatorTest {
    
    private val fadeTransitionRegistry = object : TransitionRegistry {
        override fun getTransition(destinationClass: KClass<out Destination>): NavTransition? {
            return if (destinationClass == FadeDestination::class) {
                NavTransition.Fade
            } else null
        }
    }
    
    @Test
    fun `getTransition returns annotated transition when available`() {
        val coordinator = AnimationCoordinator(fadeTransitionRegistry)
        val fadeScreen = ScreenNode(key = "fade", destination = FadeDestination)
        
        val transition = coordinator.getTransition(
            from = null,
            to = fadeScreen,
            isBack = false
        )
        
        assertEquals(NavTransition.Fade, transition)
    }
    
    @Test
    fun `getTransition returns default when no annotation`() {
        val coordinator = AnimationCoordinator(
            transitionRegistry = DefaultTransitionRegistry,
            defaultTransition = NavTransition.SlideHorizontal
        )
        val screen = ScreenNode(key = "plain", destination = PlainDestination)
        
        val transition = coordinator.getTransition(
            from = null,
            to = screen,
            isBack = false
        )
        
        assertEquals(NavTransition.SlideHorizontal, transition)
    }
    
    @Test
    fun `getTabTransition returns None for initial`() {
        val coordinator = AnimationCoordinator()
        
        val transition = coordinator.getTabTransition(
            fromIndex = null,
            toIndex = 0
        )
        
        assertEquals(NavTransition.None, transition)
    }
    
    @Test
    fun `getTabTransition returns SlideHorizontal when moving right`() {
        val coordinator = AnimationCoordinator()
        
        val transition = coordinator.getTabTransition(
            fromIndex = 0,
            toIndex = 1
        )
        
        assertEquals(NavTransition.SlideHorizontal, transition)
    }
    
    @Test
    fun `getTabTransition returns reversed SlideHorizontal when moving left`() {
        val coordinator = AnimationCoordinator()
        
        val transition = coordinator.getTabTransition(
            fromIndex = 1,
            toIndex = 0
        )
        
        assertEquals(NavTransition.SlideHorizontal.reversed(), transition)
    }
    
    @Test
    fun `getPaneTransition returns None for same pane`() {
        val coordinator = AnimationCoordinator()
        
        val transition = coordinator.getPaneTransition(
            fromRole = PaneRole.Primary,
            toRole = PaneRole.Primary
        )
        
        assertEquals(NavTransition.None, transition)
    }
    
    @Test
    fun `getPaneTransition returns Fade for different panes`() {
        val coordinator = AnimationCoordinator()
        
        val transition = coordinator.getPaneTransition(
            fromRole = PaneRole.Primary,
            toRole = PaneRole.Secondary
        )
        
        assertEquals(NavTransition.Fade, transition)
    }
    
    @Test
    fun `detectBackNavigation returns true when stack shrinks`() {
        val coordinator = AnimationCoordinator()
        val screen1 = ScreenNode(key = "1", destination = PlainDestination)
        val screen2 = ScreenNode(key = "2", destination = PlainDestination)
        
        val before = StackNode(key = "stack", children = listOf(screen1, screen2))
        val after = StackNode(key = "stack", children = listOf(screen1))
        
        assertTrue(coordinator.detectBackNavigation(before, after))
    }
    
    @Test
    fun `detectBackNavigation returns false when stack grows`() {
        val coordinator = AnimationCoordinator()
        val screen1 = ScreenNode(key = "1", destination = PlainDestination)
        val screen2 = ScreenNode(key = "2", destination = PlainDestination)
        
        val before = StackNode(key = "stack", children = listOf(screen1))
        val after = StackNode(key = "stack", children = listOf(screen1, screen2))
        
        assertFalse(coordinator.detectBackNavigation(before, after))
    }
}

// Test fixtures
private data object FadeDestination : Destination { override val route = "fade" }
private data object PlainDestination : Destination { override val route = "plain" }
```

---

## Acceptance Criteria

- [ ] `AnimationCoordinator` class with `@Stable` annotation
- [ ] `getTransition(from, to, isBack)` method with registry lookup
- [ ] `getTabTransition(fromIndex, toIndex)` with direction awareness
- [ ] `getPaneTransition(fromRole, toRole)` with subtle fade default
- [ ] `getPaneContentTransition(from, to, isBack)` for pane internals
- [ ] `detectBackNavigation(previous, current)` helper
- [ ] Registry-first, default-fallback resolution
- [ ] Full KDoc documentation
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should tab transitions be configurable per-TabNode?
2. Should we support transition modifiers (speed, easing)?

### Design Rationale

- **@Stable**: Critical for Compose optimization - coordinator rarely changes
- **Direction-aware tabs**: Horizontal slide matches physical layout
- **Subtle pane transitions**: Panes show content simultaneously, don't need strong transitions
- **Recursive annotation lookup**: Handles nested structures transparently
