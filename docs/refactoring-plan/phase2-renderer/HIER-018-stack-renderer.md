````markdown
# HIER-018: Stack Navigation Renderer

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-018 |
| **Task Name** | Stack Navigation Renderer |
| **Phase** | Phase 2: Hierarchical Rendering Engine |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | HIER-019 (AnimatedNavContent), CORE-001 (NavNode hierarchy) |
| **Blocked By** | HIER-019 |
| **Blocks** | HIER-020 (TabRenderer), HIER-021 (PaneRenderer) |

---

## Overview

The `StackRenderer` is responsible for rendering `StackNode` navigation containers with animated transitions between child nodes. It is a core component of the hierarchical rendering engine that maintains proper parent-child composition in the Compose tree.

### Key Responsibilities

| Responsibility | Description |
|---------------|-------------|
| **Active Child Rendering** | Renders only the active child (last in children list) |
| **Back Navigation Detection** | Compares current vs previous stack to detect back navigation |
| **Transition Coordination** | Delegates to `AnimatedNavContent` for enter/exit animations |
| **Predictive Back** | Enables predictive back gestures for root-level stacks only |
| **Recursive Rendering** | Delegates child rendering to `NavTreeRenderer` |

### Design Principle

The `StackRenderer` maintains the hierarchical composition model by:
- Rendering only the visible (active) child, not the entire stack
- Preserving animation context across navigation transitions
- Supporting both forward and back navigation with appropriate animations
- Integrating with the predictive back gesture system at the root level

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/renderer/StackRenderer.kt
```

---

## Implementation

### Core Component

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.StackNode

/**
 * Renders a [StackNode] with animated transitions between child nodes.
 *
 * The StackRenderer is responsible for:
 * - Displaying the active (last) child in the stack
 * - Detecting back vs forward navigation by comparing stack states
 * - Coordinating transitions through [AnimatedNavContent]
 * - Enabling predictive back gestures for root stacks
 *
 * ## Navigation Direction Detection
 *
 * Back navigation is detected when:
 * 1. The previous stack had more children than the current stack
 * 2. The new active child was present in the previous stack (popped to existing entry)
 *
 * This detection enables proper animation direction (pop animations vs push animations).
 *
 * ## Predictive Back Integration
 *
 * Predictive back is only enabled for **root stacks** (stacks with `parentKey == null`).
 * Nested stacks delegate back handling to their parent navigators.
 *
 * @param node The current StackNode state to render
 * @param previousNode The previous StackNode state for animation comparison
 * @param scope The navigation render scope providing animation and caching services
 * @param modifier Modifier to apply to the root container
 */
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    // Get active child - return early if stack is empty
    val activeChild = node.activeChild ?: return
    val previousActiveChild = previousNode?.activeChild
    
    // Determine if this is back navigation
    val isBackNavigation = remember(node.key, previousNode?.key, node.children.size, previousNode?.children?.size) {
        detectBackNavigation(
            currentStack = node,
            previousStack = previousNode
        )
    }
    
    // Get appropriate transition from coordinator
    val transition = remember(activeChild.key, previousActiveChild?.key, isBackNavigation) {
        scope.animationCoordinator.getTransition(
            from = previousActiveChild,
            to = activeChild,
            isBack = isBackNavigation
        )
    }
    
    // Determine if predictive back should be enabled
    // Only root stacks (no parent) handle predictive back directly
    val predictiveBackEnabled = node.parentKey == null
    
    // Render with animated content switching
    AnimatedNavContent(
        targetState = activeChild,
        transition = transition,
        scope = scope,
        predictiveBackEnabled = predictiveBackEnabled,
        modifier = modifier.fillMaxSize()
    ) { child ->
        // Recursively render the active child
        NavTreeRenderer(
            node = child,
            previousNode = if (child.key == previousActiveChild?.key) previousActiveChild else null,
            scope = scope
        )
    }
}

/**
 * Detects whether the navigation from [previousStack] to [currentStack] is a back navigation.
 *
 * Back navigation is detected when:
 * 1. Both stacks exist and have the same key (same stack container)
 * 2. Current stack has fewer children than previous (pop operation)
 * 3. OR current active child existed in previous stack at an earlier position
 *
 * @param currentStack The current StackNode state
 * @param previousStack The previous StackNode state (may be null for initial render)
 * @return true if this is a back navigation, false otherwise
 */
private fun detectBackNavigation(
    currentStack: StackNode,
    previousStack: StackNode?
): Boolean {
    // No previous state means initial render, not back navigation
    if (previousStack == null) return false
    
    // Different stack containers - not a stack navigation
    if (currentStack.key != previousStack.key) return false
    
    // Fewer children means pop operation (back navigation)
    if (currentStack.children.size < previousStack.children.size) {
        return true
    }
    
    // Check if current active child was already in the previous stack
    // This handles popTo operations where we jump back multiple entries
    val currentActiveKey = currentStack.activeChild?.key
    if (currentActiveKey != null) {
        val previousChildKeys = previousStack.children.dropLast(1).map { it.key }
        if (currentActiveKey in previousChildKeys) {
            return true
        }
    }
    
    return false
}

/**
 * Determines if the current child was the immediate previous child.
 *
 * Used for optimizing animation when navigating back exactly one step,
 * allowing for smoother transition caching.
 *
 * @param currentChild The current active child node
 * @param previousStack The previous stack state
 * @return true if current child was the second-to-last in previous stack
 */
private fun isImmediatePop(
    currentChild: NavNode,
    previousStack: StackNode?
): Boolean {
    if (previousStack == null || previousStack.children.size < 2) return false
    val secondToLast = previousStack.children.getOrNull(previousStack.children.size - 2)
    return secondToLast?.key == currentChild.key
}
```

### Stack Render Extensions

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.jermey.quo.vadis.core.navigation.core.NavNode
import com.jermey.quo.vadis.core.navigation.core.StackNode

/**
 * Extension properties and functions for StackNode rendering operations.
 */

/**
 * Returns the keys of all children in this stack for diffing purposes.
 */
internal val StackNode.childKeys: List<String>
    get() = children.map { it.key }

/**
 * Computes the stack difference between this and another StackNode.
 *
 * @param other The previous stack state to compare against
 * @return A [StackDiff] describing the navigation change
 */
internal fun StackNode.diffFrom(other: StackNode?): StackDiff {
    if (other == null) {
        return StackDiff(
            type = StackDiffType.INITIAL,
            addedKeys = childKeys,
            removedKeys = emptyList(),
            poppedCount = 0
        )
    }
    
    val currentKeys = childKeys
    val previousKeys = other.childKeys
    
    val addedKeys = currentKeys - previousKeys.toSet()
    val removedKeys = previousKeys - currentKeys.toSet()
    
    val diffType = when {
        addedKeys.isNotEmpty() && removedKeys.isEmpty() -> StackDiffType.PUSH
        removedKeys.isNotEmpty() && addedKeys.isEmpty() -> StackDiffType.POP
        addedKeys.isNotEmpty() && removedKeys.isNotEmpty() -> StackDiffType.REPLACE
        else -> StackDiffType.NONE
    }
    
    return StackDiff(
        type = diffType,
        addedKeys = addedKeys.toList(),
        removedKeys = removedKeys.toList(),
        poppedCount = removedKeys.size
    )
}

/**
 * Describes the difference between two stack states.
 */
internal data class StackDiff(
    val type: StackDiffType,
    val addedKeys: List<String>,
    val removedKeys: List<String>,
    val poppedCount: Int
)

/**
 * Types of stack navigation operations.
 */
internal enum class StackDiffType {
    /** Initial render with no previous state */
    INITIAL,
    /** One or more items pushed onto stack */
    PUSH,
    /** One or more items popped from stack */
    POP,
    /** Items both added and removed (e.g., replaceAll) */
    REPLACE,
    /** No change in stack structure */
    NONE
}

/**
 * Computes animation parameters based on stack diff.
 *
 * @param scope The render scope for accessing animation coordinator
 * @param previousStack The previous stack state
 * @return Animation configuration for the transition
 */
@Composable
internal fun StackNode.rememberAnimationParams(
    scope: NavRenderScope,
    previousStack: StackNode?
): StackAnimationParams {
    val diff = remember(this.key, this.children.size, previousStack?.children?.size) {
        diffFrom(previousStack)
    }
    
    return remember(diff) {
        StackAnimationParams(
            isBackNavigation = diff.type == StackDiffType.POP,
            poppedCount = diff.poppedCount,
            isPush = diff.type == StackDiffType.PUSH,
            isReplace = diff.type == StackDiffType.REPLACE
        )
    }
}

/**
 * Animation parameters computed from stack diff.
 */
internal data class StackAnimationParams(
    val isBackNavigation: Boolean,
    val poppedCount: Int,
    val isPush: Boolean,
    val isReplace: Boolean
) {
    val shouldAnimate: Boolean
        get() = isBackNavigation || isPush || isReplace
}
```

### Stack Transition Specs

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Immutable

/**
 * Default transition specifications for stack navigation.
 */
object StackTransitions {
    
    /**
     * Default duration for stack transitions in milliseconds.
     */
    const val DEFAULT_DURATION_MS = 300
    
    /**
     * Standard horizontal slide transition for stack navigation.
     *
     * - Push: New screen slides in from right, current slides left (partial)
     * - Pop: Previous screen slides in from left (partial), current slides out right
     */
    val SlideHorizontal = NavTransition(
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ) + fadeOut(
            targetAlpha = 0.7f,
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        popEnter = slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ) + fadeIn(
            initialAlpha = 0.7f,
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        popExit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DEFAULT_DURATION_MS)
        )
    )
    
    /**
     * Vertical slide transition for modal-style navigation.
     *
     * - Push: New screen slides up from bottom
     * - Pop: Current screen slides down
     */
    val SlideVertical = NavTransition(
        enter = androidx.compose.animation.slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        exit = fadeOut(
            targetAlpha = 0.9f,
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        popEnter = fadeIn(
            initialAlpha = 0.9f,
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        popExit = androidx.compose.animation.slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(DEFAULT_DURATION_MS)
        )
    )
    
    /**
     * Simple fade transition for subtle navigation.
     */
    val Fade = NavTransition(
        enter = fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
        exit = fadeOut(animationSpec = tween(DEFAULT_DURATION_MS)),
        popEnter = fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
        popExit = fadeOut(animationSpec = tween(DEFAULT_DURATION_MS))
    )
    
    /**
     * No transition - immediate switch.
     */
    val None = NavTransition(
        enter = EnterTransition.None,
        exit = ExitTransition.None,
        popEnter = EnterTransition.None,
        popExit = ExitTransition.None
    )
    
    /**
     * iOS-style navigation transition with parallax effect.
     *
     * - Push: New screen slides in from right, current slides left with dimming
     * - Pop: Reverse with spring animation
     */
    val iOSStyle = NavTransition(
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ) + fadeOut(
            targetAlpha = 0.5f,
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        popEnter = slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 3 },
            animationSpec = tween(DEFAULT_DURATION_MS)
        ) + fadeIn(
            initialAlpha = 0.5f,
            animationSpec = tween(DEFAULT_DURATION_MS)
        ),
        popExit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(DEFAULT_DURATION_MS)
        )
    )
}
```

---

## Integration Points

### Dependencies

| Component | Purpose | Location |
|-----------|---------|----------|
| `NavNode` | Navigation state hierarchy | `core/NavNode.kt` |
| `StackNode` | Stack-specific node type | `core/NavNode.kt` |
| `NavRenderScope` | Rendering context and services | `renderer/NavRenderScope.kt` |
| `AnimatedNavContent` | Animated content transitions | `renderer/AnimatedNavContent.kt` |
| `NavTreeRenderer` | Recursive child rendering | `renderer/NavTreeRenderer.kt` |
| `AnimationCoordinator` | Transition resolution | `renderer/AnimationCoordinator.kt` |

### Usage in NavTreeRenderer

```kotlin
@Composable
internal fun NavTreeRenderer(
    node: NavNode,
    previousNode: NavNode?,
    scope: NavRenderScope,
    modifier: Modifier = Modifier
) {
    when (node) {
        is ScreenNode -> ScreenRenderer(node, scope, modifier)
        is StackNode -> StackRenderer(
            node = node,
            previousNode = previousNode as? StackNode,
            scope = scope,
            modifier = modifier
        )
        is TabNode -> TabRenderer(node, previousNode as? TabNode, scope, modifier)
        is PaneNode -> PaneRenderer(node, previousNode as? PaneNode, scope, modifier)
    }
}
```

### Predictive Back Integration

```kotlin
// In QuoVadisHost
PredictiveBackHandler(enabled = canGoBack) { backEvent ->
    // StackRenderer with parentKey == null will handle this
    predictiveBackController.handleGesture(backEvent) {
        navigator.navigateBack()
    }
}
```

---

## Testing Requirements

### Unit Tests

```kotlin
class StackRendererTest {
    
    @Test
    fun `detectBackNavigation returns false for initial render`() {
        val stack = StackNode(
            key = "root",
            parentKey = null,
            children = listOf(
                ScreenNode("s1", "root", BasicDestination("home"))
            )
        )
        
        val result = detectBackNavigation(stack, previousStack = null)
        
        assertFalse(result)
    }
    
    @Test
    fun `detectBackNavigation returns true when children count decreases`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("detail"))
        
        val previousStack = StackNode("root", null, listOf(screen1, screen2))
        val currentStack = StackNode("root", null, listOf(screen1))
        
        val result = detectBackNavigation(currentStack, previousStack)
        
        assertTrue(result)
    }
    
    @Test
    fun `detectBackNavigation returns false for push operation`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("detail"))
        
        val previousStack = StackNode("root", null, listOf(screen1))
        val currentStack = StackNode("root", null, listOf(screen1, screen2))
        
        val result = detectBackNavigation(currentStack, previousStack)
        
        assertFalse(result)
    }
    
    @Test
    fun `detectBackNavigation returns true for popTo operation`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("list"))
        val screen3 = ScreenNode("s3", "root", BasicDestination("detail"))
        
        val previousStack = StackNode("root", null, listOf(screen1, screen2, screen3))
        val currentStack = StackNode("root", null, listOf(screen1))
        
        val result = detectBackNavigation(currentStack, previousStack)
        
        assertTrue(result)
    }
    
    @Test
    fun `StackDiff correctly identifies PUSH operation`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("detail"))
        
        val previousStack = StackNode("root", null, listOf(screen1))
        val currentStack = StackNode("root", null, listOf(screen1, screen2))
        
        val diff = currentStack.diffFrom(previousStack)
        
        assertEquals(StackDiffType.PUSH, diff.type)
        assertEquals(listOf("s2"), diff.addedKeys)
        assertTrue(diff.removedKeys.isEmpty())
    }
    
    @Test
    fun `StackDiff correctly identifies POP operation`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("detail"))
        
        val previousStack = StackNode("root", null, listOf(screen1, screen2))
        val currentStack = StackNode("root", null, listOf(screen1))
        
        val diff = currentStack.diffFrom(previousStack)
        
        assertEquals(StackDiffType.POP, diff.type)
        assertEquals(1, diff.poppedCount)
        assertEquals(listOf("s2"), diff.removedKeys)
    }
    
    @Test
    fun `predictive back enabled only for root stacks`() {
        val rootStack = StackNode("root", parentKey = null, children = emptyList())
        val nestedStack = StackNode("nested", parentKey = "tabs", children = emptyList())
        
        // Root stack should enable predictive back
        assertTrue(rootStack.parentKey == null)
        
        // Nested stack should not enable predictive back
        assertFalse(nestedStack.parentKey == null)
    }
}
```

### Compose UI Tests

```kotlin
class StackRendererComposeTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun `renders active child only`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("profile"))
        val stack = StackNode("root", null, listOf(screen1, screen2))
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            StackRenderer(
                node = stack,
                previousNode = null,
                scope = scope
            )
        }
        
        // Only profile (active) should be visible
        composeRule.onNodeWithTag("screen-profile").assertIsDisplayed()
        composeRule.onNodeWithTag("screen-home").assertDoesNotExist()
    }
    
    @Test
    fun `animates transition on push`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("detail"))
        
        var stack by mutableStateOf(StackNode("root", null, listOf(screen1)))
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            StackRenderer(
                node = stack,
                previousNode = null,
                scope = scope
            )
        }
        
        // Push new screen
        stack = StackNode("root", null, listOf(screen1, screen2))
        
        // Animation should be in progress
        composeRule.mainClock.advanceTimeBy(150) // Half animation
        // Both screens may be visible during animation
        
        composeRule.mainClock.advanceTimeBy(200) // Complete animation
        composeRule.onNodeWithTag("screen-detail").assertIsDisplayed()
    }
    
    @Test
    fun `empty stack renders nothing`() {
        val stack = StackNode("root", null, children = emptyList())
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            StackRenderer(
                node = stack,
                previousNode = null,
                scope = scope
            )
        }
        
        // Should render nothing
        composeRule.onRoot().onChildren().assertCountEquals(0)
    }
}
```

---

## Acceptance Criteria

- [ ] `StackRenderer` composable function implemented with correct signature
- [ ] Renders only the active child (last in children list)
- [ ] Returns early without rendering for empty stacks
- [ ] `detectBackNavigation` correctly identifies pop operations
- [ ] `detectBackNavigation` handles popTo (multi-pop) scenarios
- [ ] Predictive back enabled only for root stacks (`parentKey == null`)
- [ ] Integrates with `AnimatedNavContent` for transitions
- [ ] Passes appropriate `NavTransition` based on navigation direction
- [ ] Recursively delegates child rendering to `NavTreeRenderer`
- [ ] `StackDiff` utility correctly computes stack differences
- [ ] `StackTransitions` provides standard transition presets
- [ ] Unit tests cover all navigation detection scenarios
- [ ] Compose UI tests verify rendering behavior
- [ ] KDoc documentation on all public APIs
- [ ] Code compiles on all target platforms (Android, iOS, Desktop, Web)

---

## References

- [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md) - Architecture design
- [CORE-001-navnode-hierarchy.md](../phase1-core/CORE-001-navnode-hierarchy.md) - NavNode definitions
- [HIER-019-animated-nav-content.md](HIER-019-animated-nav-content.md) - AnimatedNavContent component
- [Compose AnimatedContent](https://developer.android.com/jetpack/compose/animation/composables-modifiers#animatedcontent) - Foundation API

````