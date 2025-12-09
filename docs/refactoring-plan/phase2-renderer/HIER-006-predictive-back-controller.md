# HIER-006: PredictiveBackController

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-006 |
| **Task Name** | Create PredictiveBackController Class |
| **Phase** | Phase 1: Core Components |
| **Complexity** | Medium |
| **Estimated Time** | 2-3 days |
| **Dependencies** | None |
| **Blocked By** | - |
| **Blocks** | HIER-019, HIER-020, HIER-024 |

---

## Overview

The `PredictiveBackController` centralizes predictive back gesture handling for the hierarchical rendering system. It tracks gesture state and progress, manages animation on completion/cancellation, and provides a clean API for renderers to apply transforms.

### Purpose

- Centralize gesture state management
- Track progress with appropriate clamping
- Animate smoothly on gesture completion or cancellation
- Provide reactive state for composables to observe

### Design Decisions

1. **@Stable**: Allows Compose to skip when controller hasn't changed
2. **Progress clamping**: Visual movement limited to 25% for better UX
3. **Spring animations**: Natural feel on completion/cancellation
4. **Coroutine-based**: Suspend functions for gesture handling

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/gesture/PredictiveBackController.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Controller for predictive back gesture handling.
 *
 * Centralizes the management of predictive back gesture state, progress tracking,
 * and animations. Renderers observe [isActive] and [progress] to apply appropriate
 * transforms during the gesture.
 *
 * ## Gesture Flow
 *
 * 1. User starts back gesture → [handleGesture] called
 * 2. [isActive] becomes true, [progress] updates with gesture
 * 3. User completes gesture → [animateToCompletion], then [onNavigateBack]
 * 4. User cancels gesture → [animateToCancel]
 * 5. [isActive] becomes false, [progress] resets to 0
 *
 * ## Progress Behavior
 *
 * - Raw gesture progress: 0.0 to 1.0
 * - Clamped visual progress: 0.0 to [PROGRESS_VISUAL_CLAMP] (0.25)
 *
 * The clamping provides better UX by limiting visual movement while still
 * detecting the full gesture. This matches platform behavior on Android.
 *
 * ## Usage
 *
 * ```kotlin
 * val controller = remember { PredictiveBackController() }
 *
 * // In composable
 * Box(
 *     modifier = Modifier.graphicsLayer {
 *         if (controller.isActive) {
 *             translationX = size.width * controller.progress
 *             val scale = 1f - (controller.progress * 0.1f)
 *             scaleX = scale
 *             scaleY = scale
 *         }
 *     }
 * )
 *
 * // Handle gesture
 * PredictiveBackHandler(enabled = canGoBack) { backEvent ->
 *     controller.handleGesture(backEvent) {
 *         navigator.navigateBack()
 *     }
 * }
 * ```
 *
 * @see PredictiveBackContent
 * @see AnimatedNavContent
 */
@Stable
class PredictiveBackController {
    
    /**
     * Whether a predictive back gesture is currently active.
     *
     * When true:
     * - Renderers should show both current and previous content
     * - Transforms should be applied based on [progress]
     * - Standard animations should be suppressed
     */
    var isActive: Boolean by mutableStateOf(false)
        private set
    
    /**
     * Current gesture progress, clamped for visual purposes.
     *
     * Range: 0.0 to [PROGRESS_VISUAL_CLAMP] (0.25)
     *
     * Use this value for visual transforms like translation and scale.
     * The clamping ensures the visual effect is subtle while the full
     * gesture can still be detected.
     */
    var progress: Float by mutableFloatStateOf(0f)
        private set
    
    /**
     * Raw gesture progress without clamping.
     *
     * Range: 0.0 to 1.0
     *
     * Use this for detecting gesture completion (e.g., > 0.5 means likely to complete).
     */
    var rawProgress: Float by mutableFloatStateOf(0f)
        private set
    
    // Animation controller for smooth transitions
    private val animatable = Animatable(0f)
    
    /**
     * Handles a predictive back gesture flow.
     *
     * This suspend function should be called from within a [PredictiveBackHandler].
     * It manages the gesture lifecycle:
     *
     * 1. Sets [isActive] to true
     * 2. Collects progress updates from [backEvent]
     * 3. On completion: animates to finish, calls [onNavigateBack]
     * 4. On cancellation: animates back to start
     * 5. Resets state
     *
     * @param backEvent Flow of back gesture events from [PredictiveBackHandler]
     * @param onNavigateBack Callback to perform actual navigation
     */
    suspend fun handleGesture(
        backEvent: Flow<BackEventCompat>,
        onNavigateBack: () -> Unit
    ) {
        isActive = true
        rawProgress = 0f
        progress = 0f
        
        try {
            // Collect gesture progress
            backEvent.collect { event ->
                rawProgress = event.progress
                progress = event.progress.coerceAtMost(PROGRESS_VISUAL_CLAMP)
            }
            
            // Gesture completed - animate to finish
            animateToCompletion()
            onNavigateBack()
            
        } catch (e: CancellationException) {
            // Gesture cancelled - animate back
            animateToCancel()
            throw e
            
        } finally {
            reset()
        }
    }
    
    /**
     * Handles gesture with explicit completion/cancel callbacks.
     *
     * Alternative API for more control over the gesture lifecycle.
     *
     * @param backEvent Flow of back gesture events
     * @param onComplete Called when gesture completes (before navigation)
     * @param onCancel Called when gesture is cancelled
     * @param onNavigateBack Called to perform navigation after completion animation
     */
    suspend fun handleGestureWithCallbacks(
        backEvent: Flow<BackEventCompat>,
        onComplete: suspend () -> Unit = {},
        onCancel: suspend () -> Unit = {},
        onNavigateBack: () -> Unit
    ) {
        isActive = true
        rawProgress = 0f
        progress = 0f
        
        try {
            backEvent.collect { event ->
                rawProgress = event.progress
                progress = event.progress.coerceAtMost(PROGRESS_VISUAL_CLAMP)
            }
            
            onComplete()
            animateToCompletion()
            onNavigateBack()
            
        } catch (e: CancellationException) {
            onCancel()
            animateToCancel()
            throw e
            
        } finally {
            reset()
        }
    }
    
    /**
     * Animates progress to completion (1.0).
     *
     * Uses a medium-stiffness spring for a responsive but natural feel.
     */
    private suspend fun animateToCompletion() {
        animatable.snapTo(progress)
        animatable.animateTo(
            targetValue = PROGRESS_COMPLETION,
            animationSpec = spring(
                stiffness = Spring.StiffnessMedium,
                dampingRatio = Spring.DampingRatioNoBouncy
            )
        ) {
            progress = value
        }
    }
    
    /**
     * Animates progress back to start (0.0).
     *
     * Uses a lower-stiffness spring for a gentle return.
     */
    private suspend fun animateToCancel() {
        animatable.snapTo(progress)
        animatable.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioLowBouncy
            )
        ) {
            progress = value
        }
    }
    
    /**
     * Resets controller state.
     */
    private fun reset() {
        isActive = false
        rawProgress = 0f
        progress = 0f
    }
    
    companion object {
        /**
         * Maximum visual progress during gesture.
         *
         * The visual movement is clamped to this value to prevent
         * excessive displacement while maintaining gesture detection.
         */
        const val PROGRESS_VISUAL_CLAMP = 0.25f
        
        /**
         * Progress value indicating completion.
         */
        const val PROGRESS_COMPLETION = 1f
        
        /**
         * Scale factor applied at maximum progress.
         *
         * At progress = 0.25, scale = 1 - (0.25 * 0.1) = 0.975
         */
        const val SCALE_FACTOR = 0.1f
        
        /**
         * Parallax factor for previous content.
         *
         * Previous content moves at this fraction of the current content's speed.
         */
        const val PARALLAX_FACTOR = 0.3f
    }
}

/**
 * Compatibility class for back gesture events.
 *
 * Abstracts platform-specific back event APIs.
 *
 * @property progress Gesture progress from 0.0 to 1.0
 * @property touchX X coordinate of touch (platform-specific)
 * @property touchY Y coordinate of touch (platform-specific)
 * @property swipeEdge Which edge the swipe started from
 */
data class BackEventCompat(
    val progress: Float,
    val touchX: Float = 0f,
    val touchY: Float = 0f,
    val swipeEdge: SwipeEdge = SwipeEdge.Left
)

/**
 * Edge from which the back gesture was initiated.
 */
enum class SwipeEdge {
    Left,
    Right
}
```

---

## Integration Points

### Providers

- **HierarchicalQuoVadisHost** (HIER-024): Creates and manages controller

### Consumers

- **AnimatedNavContent** (HIER-019): Checks `isActive` to switch rendering mode
- **PredictiveBackContent** (HIER-020): Uses `progress` for transforms
- **StackRenderer** (HIER-018): May enable/disable based on context

### Related Components

| Component | Relationship |
|-----------|--------------|
| `PredictiveBackHandler` | Platform gesture source |
| `PredictiveBackContent` | Applies transforms (HIER-020) |
| `ComposableCache` | Locks entries during gesture (HIER-007) |

---

## Testing Requirements

### Unit Tests

```kotlin
class PredictiveBackControllerTest {
    
    @Test
    fun `initial state is inactive with zero progress`() {
        val controller = PredictiveBackController()
        
        assertFalse(controller.isActive)
        assertEquals(0f, controller.progress)
        assertEquals(0f, controller.rawProgress)
    }
    
    @Test
    fun `handleGesture sets isActive during gesture`() = runTest {
        val controller = PredictiveBackController()
        val events = MutableSharedFlow<BackEventCompat>()
        
        val job = launch {
            controller.handleGesture(events) { }
        }
        
        // Give time for collection to start
        advanceTimeBy(10)
        
        assertTrue(controller.isActive)
        
        job.cancel()
    }
    
    @Test
    fun `progress is clamped to PROGRESS_VISUAL_CLAMP`() = runTest {
        val controller = PredictiveBackController()
        val events = MutableSharedFlow<BackEventCompat>()
        
        launch {
            controller.handleGesture(events) { }
        }
        
        advanceTimeBy(10)
        
        // Emit high progress
        events.emit(BackEventCompat(progress = 0.8f))
        advanceTimeBy(10)
        
        // Progress should be clamped
        assertEquals(PredictiveBackController.PROGRESS_VISUAL_CLAMP, controller.progress)
        assertEquals(0.8f, controller.rawProgress)
    }
    
    @Test
    fun `handleGesture calls onNavigateBack on completion`() = runTest {
        val controller = PredictiveBackController()
        var navigateCalled = false
        
        val events = flow {
            emit(BackEventCompat(progress = 0.5f))
            // Flow completes = gesture completed
        }
        
        controller.handleGesture(events) {
            navigateCalled = true
        }
        
        assertTrue(navigateCalled)
        assertFalse(controller.isActive)
    }
    
    @Test
    fun `handleGesture does not call onNavigateBack on cancellation`() = runTest {
        val controller = PredictiveBackController()
        var navigateCalled = false
        
        val events = flow<BackEventCompat> {
            emit(BackEventCompat(progress = 0.3f))
            throw CancellationException("User cancelled")
        }
        
        try {
            controller.handleGesture(events) {
                navigateCalled = true
            }
        } catch (e: CancellationException) {
            // Expected
        }
        
        assertFalse(navigateCalled)
        assertFalse(controller.isActive)
    }
    
    @Test
    fun `state resets after gesture completes`() = runTest {
        val controller = PredictiveBackController()
        
        val events = flow {
            emit(BackEventCompat(progress = 0.5f))
        }
        
        controller.handleGesture(events) { }
        
        assertFalse(controller.isActive)
        // Progress animates to completion then resets
        // In real test, would need to wait for animation
    }
    
    @Test
    fun `SCALE_FACTOR constant is correct`() {
        assertEquals(0.1f, PredictiveBackController.SCALE_FACTOR)
    }
    
    @Test
    fun `PARALLAX_FACTOR constant is correct`() {
        assertEquals(0.3f, PredictiveBackController.PARALLAX_FACTOR)
    }
}
```

### Integration Tests

- Test gesture flow with real compose hierarchy
- Verify animation timing feels natural
- Test cancellation at various progress levels

---

## Acceptance Criteria

- [ ] `PredictiveBackController` class with `@Stable` annotation
- [ ] `isActive` state property
- [ ] `progress` state property (clamped to 0.25)
- [ ] `rawProgress` state property (unclamped)
- [ ] `handleGesture(backEvent, onNavigateBack)` suspend function
- [ ] `handleGestureWithCallbacks` alternative API
- [ ] Spring animation on completion (medium stiffness)
- [ ] Spring animation on cancellation (low stiffness)
- [ ] `BackEventCompat` compatibility class
- [ ] Constants: `PROGRESS_VISUAL_CLAMP`, `SCALE_FACTOR`, `PARALLAX_FACTOR`
- [ ] Full KDoc documentation
- [ ] Unit tests pass

---

## Notes

### Open Questions

1. Should we expose animation customization (spring parameters)?
2. Should we track swipe edge for directional animations?

### Design Rationale

- **Progress clamping**: 25% matches Android predictive back behavior, feels natural
- **Spring animations**: Provide organic feel matching platform conventions
- **Separate raw/visual progress**: Enables gesture detection while limiting visual displacement
- **BackEventCompat**: Abstracts platform differences (Android BackEvent vs custom)
