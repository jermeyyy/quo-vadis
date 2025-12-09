# HIER-004: NavTransition Data Class

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-004 |
| **Task Name** | Create NavTransition Animation Type |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Small |
| **Estimated Time** | 0.5-1 day |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-003, HIER-005, HIER-019 |

---

## Overview

The `NavTransition` data class encapsulates the four animation states required for navigation transitions: enter, exit, pop-enter, and pop-exit. It provides a clean API for defining and composing transitions, with built-in presets for common patterns.

### Purpose

- Encapsulate all animation states in a single immutable object
- Provide factory method for creating `ContentTransform`
- Include standard presets for common navigation patterns
- Support transition reversal for bidirectional navigation

### Design Decisions

1. **Immutable**: Data class with val properties for thread safety
2. **Direction-aware**: Separate animations for forward and back navigation
3. **ContentTransform factory**: Easy integration with AnimatedContent
4. **Built-in presets**: Common patterns available as companion properties

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/animation/NavTransition.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.animation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Immutable

/**
 * Configuration for navigation transition animations.
 *
 * Encapsulates the four animation states required for bidirectional navigation:
 * - [enter]: Animation when navigating forward to this destination
 * - [exit]: Animation when navigating away from this destination (forward)
 * - [popEnter]: Animation when returning to this destination (back)
 * - [popExit]: Animation when leaving this destination via back navigation
 *
 * ## Forward Navigation (A → B)
 * - A uses [exit]
 * - B uses [enter]
 *
 * ## Back Navigation (B → A)
 * - B uses [popExit]
 * - A uses [popEnter]
 *
 * ## Usage
 *
 * ```kotlin
 * // Using presets
 * AnimatedNavContent(
 *     targetState = destination,
 *     transition = NavTransition.SlideHorizontal
 * )
 *
 * // Custom transition
 * val myTransition = NavTransition(
 *     enter = fadeIn() + slideInHorizontally { it },
 *     exit = fadeOut() + slideOutHorizontally { -it / 3 },
 *     popEnter = fadeIn() + slideInHorizontally { -it / 3 },
 *     popExit = fadeOut() + slideOutHorizontally { it }
 * )
 * ```
 *
 * @property enter Animation when entering via forward navigation
 * @property exit Animation when exiting via forward navigation
 * @property popEnter Animation when entering via back navigation
 * @property popExit Animation when exiting via back navigation
 *
 * @see AnimatedNavContent
 * @see AnimationCoordinator
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
     * @return ContentTransform with appropriate enter/exit pair
     */
    fun createTransitionSpec(isBack: Boolean): ContentTransform {
        return if (isBack) {
            popEnter togetherWith popExit
        } else {
            enter togetherWith exit
        }
    }
    
    /**
     * Returns a new transition with enter/exit and popEnter/popExit swapped.
     *
     * Useful for creating the reverse of a directional transition.
     *
     * ## Example
     *
     * ```kotlin
     * // Original: slides in from right
     * val slideRight = NavTransition.SlideHorizontal
     *
     * // Reversed: slides in from left
     * val slideLeft = slideRight.reversed()
     * ```
     */
    fun reversed(): NavTransition = NavTransition(
        enter = popEnter,
        exit = popExit,
        popEnter = enter,
        popExit = exit
    )
    
    /**
     * Combines this transition with another using + operator.
     *
     * Animations are combined additively (both play simultaneously).
     *
     * ## Example
     *
     * ```kotlin
     * val combined = NavTransition.SlideHorizontal + NavTransition.Fade
     * ```
     */
    operator fun plus(other: NavTransition): NavTransition = NavTransition(
        enter = enter + other.enter,
        exit = exit + other.exit,
        popEnter = popEnter + other.popEnter,
        popExit = popExit + other.popExit
    )
    
    companion object {
        
        /**
         * Default animation duration in milliseconds.
         */
        const val DEFAULT_DURATION_MS = 300
        
        /**
         * No animation - instant transition.
         *
         * Useful for:
         * - Modal dialogs with custom animations
         * - Instant tab switches
         * - Testing
         */
        val None = NavTransition(
            enter = EnterTransition.None,
            exit = ExitTransition.None,
            popEnter = EnterTransition.None,
            popExit = ExitTransition.None
        )
        
        /**
         * Horizontal slide transition.
         *
         * - Forward: New screen slides in from right, old slides left (parallax)
         * - Back: Previous screen slides in from left, current slides right
         *
         * Matches platform navigation patterns on Android and iOS.
         */
        val SlideHorizontal = NavTransition(
            enter = slideInHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                initialOffsetX = { fullWidth -> fullWidth }
            ),
            exit = slideOutHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                targetOffsetX = { fullWidth -> -fullWidth / 3 }
            ),
            popEnter = slideInHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                initialOffsetX = { fullWidth -> -fullWidth / 3 }
            ),
            popExit = slideOutHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                targetOffsetX = { fullWidth -> fullWidth }
            )
        )
        
        /**
         * Vertical slide transition.
         *
         * - Forward: New screen slides up from bottom
         * - Back: Current screen slides down
         *
         * Ideal for modal presentations and bottom sheets.
         */
        val SlideVertical = NavTransition(
            enter = slideInVertically(
                animationSpec = tween(DEFAULT_DURATION_MS),
                initialOffsetY = { fullHeight -> fullHeight }
            ),
            exit = fadeOut(
                animationSpec = tween(DEFAULT_DURATION_MS / 2)
            ),
            popEnter = fadeIn(
                animationSpec = tween(DEFAULT_DURATION_MS / 2)
            ),
            popExit = slideOutVertically(
                animationSpec = tween(DEFAULT_DURATION_MS),
                targetOffsetY = { fullHeight -> fullHeight }
            )
        )
        
        /**
         * Crossfade transition.
         *
         * Simple fade between screens. Good for:
         * - Tab switches
         * - Settings screens
         * - Content updates
         */
        val Fade = NavTransition(
            enter = fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
            exit = fadeOut(animationSpec = tween(DEFAULT_DURATION_MS)),
            popEnter = fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
            popExit = fadeOut(animationSpec = tween(DEFAULT_DURATION_MS))
        )
        
        /**
         * Scale + fade transition.
         *
         * New screen scales up from center while fading in.
         * Good for:
         * - Modal dialogs
         * - Detail expansions
         * - Zoom-in effects
         */
        val Scale = NavTransition(
            enter = scaleIn(
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                initialScale = 0.92f
            ) + fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
            exit = scaleOut(
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                targetScale = 1.08f
            ) + fadeOut(animationSpec = tween(DEFAULT_DURATION_MS / 2)),
            popEnter = scaleIn(
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                initialScale = 1.08f
            ) + fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
            popExit = scaleOut(
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                targetScale = 0.92f
            ) + fadeOut(animationSpec = tween(DEFAULT_DURATION_MS / 2))
        )
        
        /**
         * Material 3 shared axis transition (horizontal).
         *
         * Combines slide, fade, and scale for a polished Material Design feel.
         */
        val SharedAxisX = NavTransition(
            enter = slideInHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                initialOffsetX = { it / 10 }
            ) + fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
            exit = slideOutHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                targetOffsetX = { -it / 10 }
            ) + fadeOut(animationSpec = tween(DEFAULT_DURATION_MS / 2)),
            popEnter = slideInHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                initialOffsetX = { -it / 10 }
            ) + fadeIn(animationSpec = tween(DEFAULT_DURATION_MS)),
            popExit = slideOutHorizontally(
                animationSpec = tween(DEFAULT_DURATION_MS),
                targetOffsetX = { it / 10 }
            ) + fadeOut(animationSpec = tween(DEFAULT_DURATION_MS / 2))
        )
    }
}
```

---

## Integration Points

### Consumers

- **AnimatedNavContent** (HIER-019): Uses `createTransitionSpec()` for AnimatedContent
- **AnimationCoordinator** (HIER-005): Returns NavTransition instances
- **TransitionRegistry** (HIER-003): Stores NavTransition configurations

### Related Components

| Component | Relationship |
|-----------|--------------|
| `AnimatedNavContent` | Primary consumer (HIER-019) |
| `AnimationCoordinator` | Provides default and resolved transitions (HIER-005) |
| `@Transition` annotation | Specifies transition type (HIER-011) |

---

## Testing Requirements

### Unit Tests

```kotlin
class NavTransitionTest {
    
    @Test
    fun `createTransitionSpec returns enter+exit for forward navigation`() {
        val transition = NavTransition.SlideHorizontal
        val spec = transition.createTransitionSpec(isBack = false)
        
        // Verify it's a ContentTransform (can't easily inspect internals)
        assertNotNull(spec)
    }
    
    @Test
    fun `createTransitionSpec returns popEnter+popExit for back navigation`() {
        val transition = NavTransition.SlideHorizontal
        val spec = transition.createTransitionSpec(isBack = true)
        
        assertNotNull(spec)
    }
    
    @Test
    fun `reversed swaps enter-exit with popEnter-popExit`() {
        val original = NavTransition(
            enter = fadeIn(),
            exit = fadeOut(),
            popEnter = slideInHorizontally { it },
            popExit = slideOutHorizontally { it }
        )
        
        val reversed = original.reversed()
        
        // After reversal, enter should be original popEnter
        assertEquals(original.popEnter, reversed.enter)
        assertEquals(original.popExit, reversed.exit)
        assertEquals(original.enter, reversed.popEnter)
        assertEquals(original.exit, reversed.popExit)
    }
    
    @Test
    fun `None preset has no animations`() {
        assertEquals(EnterTransition.None, NavTransition.None.enter)
        assertEquals(ExitTransition.None, NavTransition.None.exit)
        assertEquals(EnterTransition.None, NavTransition.None.popEnter)
        assertEquals(ExitTransition.None, NavTransition.None.popExit)
    }
    
    @Test
    fun `plus operator combines transitions`() {
        val combined = NavTransition.SlideHorizontal + NavTransition.Fade
        
        // Combined transition should not be equal to either original
        assertNotEquals(NavTransition.SlideHorizontal, combined)
        assertNotEquals(NavTransition.Fade, combined)
    }
    
    @Test
    fun `double reversed equals original`() {
        val original = NavTransition.SlideHorizontal
        val doubleReversed = original.reversed().reversed()
        
        assertEquals(original, doubleReversed)
    }
}
```

### Visual Tests

- Verify SlideHorizontal animates correctly on Android/iOS
- Verify Fade crossfades smoothly
- Verify Scale expands from center
- Verify predictive back gesture respects transition

---

## Acceptance Criteria

- [ ] `NavTransition` data class with enter, exit, popEnter, popExit
- [ ] `@Immutable` annotation for Compose stability
- [ ] `createTransitionSpec(isBack: Boolean)` method
- [ ] `reversed()` method for direction swapping
- [ ] `plus` operator for combining transitions
- [ ] Companion presets: `None`, `SlideHorizontal`, `SlideVertical`, `Fade`, `Scale`, `SharedAxisX`
- [ ] Full KDoc documentation with usage examples
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should we add more Material 3 transitions (SharedAxisY, SharedAxisZ)?
2. Should transition duration be configurable via parameter?

### Design Rationale

- **@Immutable**: Ensures Compose can safely skip recomposition
- **Data class**: Enables easy comparison and copy-with-modification
- **Presets as properties**: Cleaner API than factory functions
- **Parallax effect**: Exit animations move 1/3 distance for depth effect (matches iOS)
