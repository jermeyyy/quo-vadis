````markdown
# HIER-019: Animated Navigation Content

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-019 |
| **Task Name** | Animated Navigation Content |
| **Phase** | Phase 2: Hierarchical Rendering Engine |
| **Complexity** | High |
| **Estimated Time** | 3-4 days |
| **Dependencies** | CORE-001 (NavNode hierarchy), HIER-005 (AnimationCoordinator) |
| **Blocked By** | HIER-005 |
| **Blocks** | HIER-018 (StackRenderer), HIER-020 (TabRenderer) |

---

## Overview

`AnimatedNavContent` is a custom composable that wraps navigation content transitions. It extends Compose's `AnimatedContent` with navigation-specific features including predictive back gesture support, state tracking for animation direction, and integration with the navigation render scope.

### Key Responsibilities

| Responsibility | Description |
|---------------|-------------|
| **State Tracking** | Maintains `displayedState` and `previousState` for animation direction |
| **Transition Management** | Applies `NavTransition` specs for enter/exit animations |
| **Predictive Back Support** | Switches to `PredictiveBackContent` when gesture is active |
| **Scope Provisioning** | Provides `AnimatedVisibilityScope` to content for shared elements |
| **Back Detection** | Detects back navigation by comparing target with previous state |

### Design Principle

`AnimatedNavContent` serves as the animation boundary in the hierarchical rendering system. Each container node (Stack, Tab, Pane) uses its own `AnimatedNavContent` instance, enabling:
- Independent animation timelines at each navigation level
- Proper `AnimatedVisibilityScope` propagation for shared element transitions
- Seamless switching between standard animations and predictive back gestures

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/renderer/AnimatedNavContent.kt
```

---

## Implementation

### Core Component

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * Custom AnimatedContent variant optimized for navigation transitions.
 *
 * AnimatedNavContent provides:
 * - State tracking for determining animation direction (forward vs back)
 * - Integration with [PredictiveBackContent] for gesture-driven transitions
 * - [AnimatedVisibilityScope] propagation for shared element support
 * - Automatic transition spec selection based on navigation direction
 *
 * ## State Tracking
 *
 * The component maintains two internal states:
 * - [displayedState]: The currently displayed (or animating-to) target
 * - [previousState]: The state that was displayed before the current transition
 *
 * These states enable back navigation detection by checking if the new target
 * matches a previously displayed state.
 *
 * ## Predictive Back Integration
 *
 * When [predictiveBackEnabled] is true and the [PredictiveBackController] is active,
 * the component switches from standard [AnimatedContent] to [PredictiveBackContent],
 * which renders both current and previous content with gesture-driven transforms.
 *
 * @param T The type of navigation node being animated (must extend [NavNode])
 * @param targetState The target node state to animate to
 * @param transition The transition specification for enter/exit animations
 * @param scope The navigation render scope providing services and context
 * @param predictiveBackEnabled Whether predictive back gestures should be handled
 * @param modifier Modifier to apply to the container
 * @param content Composable content to render for each state
 */
@Composable
internal fun <T : NavNode> AnimatedNavContent(
    targetState: T,
    transition: NavTransition,
    scope: NavRenderScope,
    predictiveBackEnabled: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    // Track displayed and previous states for back navigation detection
    var displayedState by remember { mutableStateOf(targetState) }
    var previousState by remember { mutableStateOf<T?>(null) }
    
    // Detect back navigation: target matches a previous state
    val isBackNavigation = remember(targetState.key, displayedState.key, previousState?.key) {
        targetState.key != displayedState.key && previousState?.key == targetState.key
    }
    
    // Handle predictive back gesture mode
    val predictiveBackActive = predictiveBackEnabled && scope.predictiveBackController.isActive
    
    if (predictiveBackActive) {
        // Gesture-driven transition
        PredictiveBackContent(
            current = displayedState,
            previous = previousState,
            progress = scope.predictiveBackController.progress,
            scope = scope,
            modifier = modifier,
            content = content
        )
    } else {
        // Standard animated transition
        StandardAnimatedContent(
            targetState = targetState,
            displayedState = displayedState,
            previousState = previousState,
            transition = transition,
            isBackNavigation = isBackNavigation,
            modifier = modifier,
            onStateChange = { newDisplayed, newPrevious ->
                displayedState = newDisplayed
                previousState = newPrevious
            },
            scope = scope,
            content = content
        )
    }
}

/**
 * Standard AnimatedContent wrapper with state tracking.
 */
@Composable
private fun <T : NavNode> StandardAnimatedContent(
    targetState: T,
    displayedState: T,
    previousState: T?,
    transition: NavTransition,
    isBackNavigation: Boolean,
    modifier: Modifier,
    onStateChange: (displayed: T, previous: T?) -> Unit,
    scope: NavRenderScope,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            transition.createTransitionSpec(isBackNavigation)
        },
        modifier = modifier,
        label = "NavAnimatedContent"
    ) { animatingState ->
        // Update state tracking when animation target changes
        LaunchedEffect(animatingState.key) {
            if (animatingState.key == targetState.key && animatingState.key != displayedState.key) {
                onStateChange(targetState, displayedState)
            }
        }
        
        // Provide AnimatedVisibilityScope to content
        scope.withAnimatedVisibilityScope(this) {
            content(animatingState)
        }
    }
}
```

### NavTransition Data Class

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable

/**
 * Defines enter and exit animations for navigation transitions.
 *
 * NavTransition encapsulates four animation specs:
 * - [enter]: Animation when navigating forward (push)
 * - [exit]: Animation for the outgoing screen on forward navigation
 * - [popEnter]: Animation when navigating back (pop)
 * - [popExit]: Animation for the outgoing screen on back navigation
 *
 * ## Usage
 *
 * ```kotlin
 * val slideTransition = NavTransition(
 *     enter = slideInHorizontally { it },
 *     exit = slideOutHorizontally { -it / 3 },
 *     popEnter = slideInHorizontally { -it / 3 },
 *     popExit = slideOutHorizontally { it }
 * )
 * ```
 *
 * @property enter Enter animation for forward navigation
 * @property exit Exit animation for forward navigation
 * @property popEnter Enter animation for back navigation
 * @property popExit Exit animation for back navigation
 */
@Immutable
data class NavTransition(
    val enter: EnterTransition,
    val exit: ExitTransition,
    val popEnter: EnterTransition,
    val popExit: ExitTransition
) {
    /**
     * Creates a [ContentTransform] for use with AnimatedContent.
     *
     * @param isBack Whether this is a back navigation
     * @return ContentTransform with appropriate enter/exit animations
     */
    fun createTransitionSpec(isBack: Boolean): ContentTransform {
        return if (isBack) {
            popEnter togetherWith popExit
        } else {
            enter togetherWith exit
        }
    }
    
    /**
     * Returns a reversed version of this transition (swap enter/exit with pop variants).
     */
    fun reversed(): NavTransition = NavTransition(
        enter = popEnter,
        exit = popExit,
        popEnter = enter,
        popExit = exit
    )
    
    /**
     * Combines this transition with another, using + operator on animations.
     */
    operator fun plus(other: NavTransition): NavTransition = NavTransition(
        enter = enter + other.enter,
        exit = exit + other.exit,
        popEnter = popEnter + other.popEnter,
        popExit = popExit + other.popExit
    )
    
    companion object {
        /**
         * Standard horizontal slide transition.
         *
         * - Forward: Slide in from right, current slides left (partial)
         * - Back: Slide in from left (partial), current slides right
         */
        val SlideHorizontal = NavTransition(
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300)
            ) + fadeOut(
                targetAlpha = 0.7f,
                animationSpec = tween(300)
            ),
            popEnter = slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 3 },
                animationSpec = tween(300)
            ) + fadeIn(
                initialAlpha = 0.7f,
                animationSpec = tween(300)
            ),
            popExit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(300)
            )
        )
        
        /**
         * Simple fade transition.
         */
        val Fade = NavTransition(
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            popEnter = fadeIn(animationSpec = tween(300)),
            popExit = fadeOut(animationSpec = tween(300))
        )
        
        /**
         * No animation - immediate switch.
         */
        val None = NavTransition(
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            popEnter = EnterTransition.None,
            popExit = ExitTransition.None
        )
        
        /**
         * Vertical slide for modal presentations.
         */
        val SlideVertical = NavTransition(
            enter = androidx.compose.animation.slideInVertically(
                initialOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(300)
            ),
            exit = fadeOut(targetAlpha = 0.9f, animationSpec = tween(300)),
            popEnter = fadeIn(initialAlpha = 0.9f, animationSpec = tween(300)),
            popExit = androidx.compose.animation.slideOutVertically(
                targetOffsetY = { fullHeight -> fullHeight },
                animationSpec = tween(300)
            )
        )
        
        /**
         * Scale + fade transition for emphasis.
         */
        val ScaleFade = NavTransition(
            enter = androidx.compose.animation.scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = androidx.compose.animation.scaleOut(
                targetScale = 1.08f,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300)),
            popEnter = androidx.compose.animation.scaleIn(
                initialScale = 1.08f,
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            popExit = androidx.compose.animation.scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        )
    }
}
```

### PredictiveBackContent Component

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.core.NavNode

/**
 * Renders current and previous content with gesture-driven transforms for predictive back.
 *
 * PredictiveBackContent displays both the current (exiting) content and previous (entering)
 * content simultaneously, applying transforms based on gesture progress:
 *
 * - **Previous content**: Positioned behind current, with parallax translation
 * - **Current content**: Translates right and scales down as gesture progresses
 *
 * When the gesture completes, the navigation occurs and normal rendering resumes.
 * When the gesture is cancelled, the transform animates back to the initial state.
 *
 * ## Transform Calculations
 *
 * - Current screen X translation: `width * progress`
 * - Current screen scale: `1 - (progress * SCALE_FACTOR)`
 * - Previous screen X translation: `-width * PARALLAX_FACTOR * (1 - progress)`
 *
 * @param T The type of navigation node
 * @param current The currently displayed (exiting) content
 * @param previous The previous (entering) content, may be null
 * @param progress Gesture progress from 0 (not started) to 1 (completed)
 * @param scope Navigation render scope for caching and context
 * @param modifier Modifier for the container
 * @param content Composable to render for each state
 */
@Composable
internal fun <T : NavNode> PredictiveBackContent(
    current: T,
    previous: T?,
    progress: Float,
    scope: NavRenderScope,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.(T) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Previous (incoming) content - rendered behind current
        if (previous != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Parallax effect: slight movement as gesture progresses
                        translationX = -size.width * PARALLAX_FACTOR * (1f - progress)
                        // Slight scale for depth effect
                        val scale = PREVIOUS_MIN_SCALE + (progress * (1f - PREVIOUS_MIN_SCALE))
                        scaleX = scale
                        scaleY = scale
                        // Fade in as gesture progresses
                        alpha = PREVIOUS_MIN_ALPHA + (progress * (1f - PREVIOUS_MIN_ALPHA))
                    }
            ) {
                // Use cache to preserve state
                scope.cache.CachedEntry(key = previous.key) {
                    StaticAnimatedVisibilityScope { visibilityScope ->
                        content(visibilityScope, previous)
                    }
                }
            }
        }
        
        // Current (exiting) content - transforms based on gesture
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Slide right as gesture progresses
                    translationX = size.width * progress
                    // Scale down slightly for depth
                    val scale = 1f - (progress * SCALE_FACTOR)
                    scaleX = scale
                    scaleY = scale
                    // Add shadow/elevation effect
                    shadowElevation = SHADOW_ELEVATION * (1f - progress)
                    // Rounded corners during gesture
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        (CORNER_RADIUS * progress).dp
                    )
                    clip = progress > 0f
                }
        ) {
            scope.cache.CachedEntry(key = current.key) {
                StaticAnimatedVisibilityScope { visibilityScope ->
                    content(visibilityScope, current)
                }
            }
        }
    }
}

/**
 * Parallax factor for the previous (background) screen.
 * 0.3 means it moves 30% of the distance the current screen moves.
 */
private const val PARALLAX_FACTOR = 0.3f

/**
 * Scale reduction factor for the current screen during gesture.
 * 0.1 means the screen scales to 90% at full gesture progress.
 */
private const val SCALE_FACTOR = 0.1f

/**
 * Minimum scale for the previous screen at gesture start.
 */
private const val PREVIOUS_MIN_SCALE = 0.95f

/**
 * Minimum alpha for the previous screen at gesture start.
 */
private const val PREVIOUS_MIN_ALPHA = 0.7f

/**
 * Shadow elevation for the current screen in dp.
 */
private const val SHADOW_ELEVATION = 8f

/**
 * Corner radius applied to current screen during gesture.
 */
private const val CORNER_RADIUS = 16f
```

### StaticAnimatedVisibilityScope

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.rememberTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Provides a static [AnimatedVisibilityScope] for content that needs the scope
 * but is not actually animating (e.g., during predictive back).
 *
 * This is necessary because shared element modifiers require an [AnimatedVisibilityScope],
 * but during predictive back gestures we're manually controlling transforms rather than
 * using Compose's animation system.
 *
 * The scope reports as "visible" with no active animations, allowing shared elements
 * to render correctly without participating in automatic transitions.
 *
 * @param content Composable that receives the static visibility scope
 */
@Composable
internal fun StaticAnimatedVisibilityScope(
    content: @Composable (AnimatedVisibilityScope) -> Unit
) {
    // Create a stable "visible" transition state
    val transitionState = remember {
        MutableTransitionState(true).apply {
            targetState = true
        }
    }
    
    val transition = rememberTransition(transitionState, label = "StaticVisibility")
    
    // Create AnimatedVisibilityScope implementation
    val scope = remember(transition) {
        AnimatedVisibilityScopeImpl(transition)
    }
    
    content(scope)
}

/**
 * Implementation of [AnimatedVisibilityScope] backed by a transition.
 */
private class AnimatedVisibilityScopeImpl(
    override val transition: Transition<Boolean>
) : AnimatedVisibilityScope {
    // AnimatedVisibilityScope is an interface with only the transition property
    // All animation modifiers will use this transition
}
```

### LocalAnimatedVisibilityScope Provider

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal providing the current [AnimatedVisibilityScope].
 *
 * This local is set by [AnimatedNavContent] and [PredictiveBackContent] to provide
 * the correct visibility scope to screen content for shared element transitions.
 *
 * ## Usage
 *
 * Screen composables can access this to set up shared elements:
 *
 * ```kotlin
 * @Screen(DetailDestination::class)
 * @Composable
 * fun DetailScreen(destination: DetailDestination) {
 *     val visibilityScope = LocalAnimatedVisibilityScope.current
 *     
 *     // Use with SharedTransitionScope
 *     with(sharedTransitionScope) {
 *         Image(
 *             modifier = Modifier.sharedElement(
 *                 state = rememberSharedContentState("image"),
 *                 animatedVisibilityScope = visibilityScope
 *             )
 *         )
 *     }
 * }
 * ```
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
```

### NavRenderScope Extension for Visibility Scope

```kotlin
package com.jermey.quo.vadis.core.navigation.renderer

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Extension for [NavRenderScope] to provide [AnimatedVisibilityScope] to content.
 */
@Stable
interface NavRenderScope {
    val navigator: Navigator
    val cache: ComposableCache
    val animationCoordinator: AnimationCoordinator
    val predictiveBackController: PredictiveBackController
    val sharedTransitionScope: SharedTransitionScope
    val screenRegistry: ScreenRegistry
    val wrapperRegistry: WrapperRegistry
    
    /**
     * Provides [AnimatedVisibilityScope] to the content via [LocalAnimatedVisibilityScope].
     *
     * @param visibilityScope The scope to provide
     * @param content Composable content that can access the scope
     */
    @Composable
    fun withAnimatedVisibilityScope(
        visibilityScope: AnimatedVisibilityScope,
        content: @Composable () -> Unit
    ) {
        CompositionLocalProvider(
            LocalAnimatedVisibilityScope provides visibilityScope
        ) {
            content()
        }
    }
}
```

---

## Integration Points

### Dependencies

| Component | Purpose | Location |
|-----------|---------|----------|
| `NavNode` | Navigation state type constraint | `core/NavNode.kt` |
| `NavRenderScope` | Rendering context and services | `renderer/NavRenderScope.kt` |
| `PredictiveBackController` | Gesture state management | `renderer/PredictiveBackController.kt` |
| `ComposableCache` | State preservation | `renderer/ComposableCache.kt` |
| `AnimatedContent` | Compose animation foundation | `androidx.compose.animation` |

### Used By

| Component | Usage |
|-----------|-------|
| `StackRenderer` | Animates between stack children |
| `TabRenderer` | Animates between tab stacks |
| `PaneRenderer` | Animates pane visibility changes |

### Example Usage in StackRenderer

```kotlin
@Composable
internal fun StackRenderer(
    node: StackNode,
    previousNode: StackNode?,
    scope: NavRenderScope,
    modifier: Modifier
) {
    val activeChild = node.activeChild ?: return
    val previousActiveChild = previousNode?.activeChild
    
    val isBackNavigation = detectBackNavigation(node, previousNode)
    val transition = scope.animationCoordinator.getTransition(
        from = previousActiveChild,
        to = activeChild,
        isBack = isBackNavigation
    )
    
    AnimatedNavContent(
        targetState = activeChild,
        transition = transition,
        scope = scope,
        predictiveBackEnabled = node.parentKey == null,
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

---

## Testing Requirements

### Unit Tests

```kotlin
class AnimatedNavContentTest {
    
    @Test
    fun `NavTransition createTransitionSpec returns forward animations when not back`() {
        val transition = NavTransition.SlideHorizontal
        
        val spec = transition.createTransitionSpec(isBack = false)
        
        // Verify enter animation is the forward variant
        assertEquals(transition.enter, spec.targetContentEnter)
        assertEquals(transition.exit, spec.initialContentExit)
    }
    
    @Test
    fun `NavTransition createTransitionSpec returns pop animations when back`() {
        val transition = NavTransition.SlideHorizontal
        
        val spec = transition.createTransitionSpec(isBack = true)
        
        // Verify enter animation is the pop variant
        assertEquals(transition.popEnter, spec.targetContentEnter)
        assertEquals(transition.popExit, spec.initialContentExit)
    }
    
    @Test
    fun `NavTransition reversed swaps forward and pop animations`() {
        val transition = NavTransition.SlideHorizontal
        val reversed = transition.reversed()
        
        assertEquals(transition.popEnter, reversed.enter)
        assertEquals(transition.popExit, reversed.exit)
        assertEquals(transition.enter, reversed.popEnter)
        assertEquals(transition.exit, reversed.popExit)
    }
    
    @Test
    fun `NavTransition None has no animations`() {
        val none = NavTransition.None
        
        assertEquals(EnterTransition.None, none.enter)
        assertEquals(ExitTransition.None, none.exit)
        assertEquals(EnterTransition.None, none.popEnter)
        assertEquals(ExitTransition.None, none.popExit)
    }
    
    @Test
    fun `NavTransition plus combines animations`() {
        val slide = NavTransition.SlideHorizontal
        val fade = NavTransition.Fade
        
        val combined = slide + fade
        
        // Combined should have both effects
        assertNotEquals(slide.enter, combined.enter)
        assertNotEquals(fade.enter, combined.enter)
    }
}
```

### Compose UI Tests

```kotlin
class AnimatedNavContentComposeTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun `displays target state content`() {
        val screen = ScreenNode("s1", "root", BasicDestination("home"))
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            AnimatedNavContent(
                targetState = screen,
                transition = NavTransition.None,
                scope = scope,
                predictiveBackEnabled = false
            ) { node ->
                TestScreen(node.key)
            }
        }
        
        composeRule.onNodeWithTag("screen-s1").assertIsDisplayed()
    }
    
    @Test
    fun `animates transition between states`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val screen2 = ScreenNode("s2", "root", BasicDestination("detail"))
        
        var target by mutableStateOf(screen1)
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            AnimatedNavContent(
                targetState = target,
                transition = NavTransition.Fade,
                scope = scope,
                predictiveBackEnabled = false
            ) { node ->
                TestScreen(node.key)
            }
        }
        
        // Initial state
        composeRule.onNodeWithTag("screen-s1").assertIsDisplayed()
        
        // Change target
        target = screen2
        
        // During animation, both may be visible
        composeRule.mainClock.advanceTimeBy(150)
        
        // After animation completes
        composeRule.mainClock.advanceTimeBy(200)
        composeRule.onNodeWithTag("screen-s2").assertIsDisplayed()
    }
    
    @Test
    fun `provides AnimatedVisibilityScope to content`() {
        val screen = ScreenNode("s1", "root", BasicDestination("home"))
        var receivedScope: AnimatedVisibilityScope? = null
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            AnimatedNavContent(
                targetState = screen,
                transition = NavTransition.None,
                scope = scope,
                predictiveBackEnabled = false
            ) { _ ->
                receivedScope = LocalAnimatedVisibilityScope.current
                Box(Modifier.testTag("content"))
            }
        }
        
        composeRule.waitForIdle()
        assertNotNull(receivedScope)
    }
    
    @Test
    fun `switches to PredictiveBackContent when gesture active`() {
        val screen1 = ScreenNode("s1", "root", BasicDestination("home"))
        val controller = FakePredictiveBackController(isActive = true, progress = 0.5f)
        
        composeRule.setContent {
            val scope = FakeNavRenderScope(predictiveBackController = controller)
            AnimatedNavContent(
                targetState = screen1,
                transition = NavTransition.SlideHorizontal,
                scope = scope,
                predictiveBackEnabled = true
            ) { node ->
                TestScreen(node.key)
            }
        }
        
        // Content should be transformed based on progress
        composeRule.onNodeWithTag("screen-s1")
            .assertIsDisplayed()
            // Transform assertions would go here
    }
}
```

### Predictive Back Tests

```kotlin
class PredictiveBackContentTest {
    
    @get:Rule
    val composeRule = createComposeRule()
    
    @Test
    fun `renders current content with transform`() {
        val current = ScreenNode("s1", "root", BasicDestination("detail"))
        val previous = ScreenNode("s0", "root", BasicDestination("home"))
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            PredictiveBackContent(
                current = current,
                previous = previous,
                progress = 0.5f,
                scope = scope
            ) { node ->
                TestScreen(node.key)
            }
        }
        
        // Both screens should be visible
        composeRule.onNodeWithTag("screen-s1").assertIsDisplayed()
        composeRule.onNodeWithTag("screen-s0").assertIsDisplayed()
    }
    
    @Test
    fun `renders only current when no previous`() {
        val current = ScreenNode("s1", "root", BasicDestination("home"))
        
        composeRule.setContent {
            val scope = FakeNavRenderScope()
            PredictiveBackContent(
                current = current,
                previous = null,
                progress = 0.5f,
                scope = scope
            ) { node ->
                TestScreen(node.key)
            }
        }
        
        composeRule.onNodeWithTag("screen-s1").assertIsDisplayed()
        composeRule.onNodeWithTag("screen-*").assertCountEquals(1)
    }
    
    @Test
    fun `applies correct transforms at progress milestones`() {
        val current = ScreenNode("s1", "root", BasicDestination("detail"))
        val previous = ScreenNode("s0", "root", BasicDestination("home"))
        
        listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { progress ->
            composeRule.setContent {
                val scope = FakeNavRenderScope()
                PredictiveBackContent(
                    current = current,
                    previous = previous,
                    progress = progress,
                    scope = scope
                ) { node ->
                    TestScreen(node.key)
                }
            }
            
            composeRule.waitForIdle()
            // Verify screens are displayed at each progress level
            composeRule.onNodeWithTag("screen-s1").assertIsDisplayed()
        }
    }
}
```

---

## Acceptance Criteria

- [ ] `AnimatedNavContent` composable function implemented with correct generic signature
- [ ] Tracks `displayedState` and `previousState` for animation direction detection
- [ ] Correctly detects back navigation by comparing target with previous state
- [ ] Switches to `PredictiveBackContent` when `predictiveBackEnabled` and controller is active
- [ ] Uses standard `AnimatedContent` for non-gesture transitions
- [ ] Provides `AnimatedVisibilityScope` to content via `withAnimatedVisibilityScope`
- [ ] `NavTransition` data class with enter/exit/popEnter/popExit animations
- [ ] `NavTransition.createTransitionSpec()` returns correct animations based on direction
- [ ] `NavTransition` companion object provides standard presets (SlideHorizontal, Fade, None, etc.)
- [ ] `PredictiveBackContent` renders both current and previous with gesture transforms
- [ ] `PredictiveBackContent` applies parallax effect to previous screen
- [ ] `PredictiveBackContent` applies scale and translation to current screen
- [ ] `StaticAnimatedVisibilityScope` provides valid scope for non-animating content
- [ ] `LocalAnimatedVisibilityScope` composition local defined and documented
- [ ] Unit tests cover `NavTransition` behavior
- [ ] Compose UI tests verify animation transitions
- [ ] Predictive back tests verify gesture-driven rendering
- [ ] KDoc documentation on all public APIs
- [ ] Code compiles on all target platforms (Android, iOS, Desktop, Web)

---

## References

- [RENDER-011-hierarchical-engine.md](RENDER-011-hierarchical-engine.md) - Architecture design
- [HIER-018-stack-renderer.md](HIER-018-stack-renderer.md) - Primary consumer
- [HIER-005-animation-coordinator.md](HIER-005-animation-coordinator.md) - Transition resolution
- [Compose AnimatedContent](https://developer.android.com/jetpack/compose/animation/composables-modifiers#animatedcontent) - Foundation API
- [Predictive Back Design](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture) - Android gesture reference

````