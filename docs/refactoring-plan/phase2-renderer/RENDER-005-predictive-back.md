# RENDER-005: Integrate Predictive Back with Speculative Pop

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-005 |
| **Task Name** | Integrate Predictive Back with Speculative Pop |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | High |
| **Estimated Time** | 4-5 days |
| **Dependencies** | RENDER-004 |
| **Blocked By** | RENDER-004 |
| **Blocks** | - |

---

## Overview

Predictive back is a modern navigation pattern where users can preview the result of a back action while performing the gesture, before committing to it. This task integrates predictive back into QuoVadisHost with **speculative pop** - computing the pop result ahead of time to render both current and previous states during the gesture.

### What is Speculative Pop?

Traditional pop:
```
User releases gesture → Pop executed → UI updates
```

Speculative pop:
```
User starts gesture → Pop result computed (speculative)
                    → Both states rendered with progress
User releases       → Animation completes to speculative result
                    - OR -
User cancels        → Animation reverses to original
```

### Platform Support

| Platform | Gesture Type | API |
|----------|-------------|-----|
| Android 14+ | System back gesture | `PredictiveBackHandler` |
| Android <14 | Back button only | Fall back to immediate pop |
| iOS | Edge swipe gesture | Custom gesture recognizer |
| Desktop | Escape key or back button | Immediate pop |
| Web | Browser back button | `popstate` event |

---

## File Locations

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/QuoVadisHost.kt (updates)
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/PredictiveBackIntegration.kt (new)
quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/AndroidPredictiveBack.kt (new/update)
quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/IosPredictiveBack.kt (new)
```

---

## Implementation

### Core Predictive Back Handler

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import com.jermey.quo.vadis.core.navigation.core.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Callback interface for predictive back gesture events.
 */
interface PredictiveBackCallback {
    /**
     * Called when a back gesture starts.
     * @return True if the gesture should be handled, false to pass through
     */
    fun onBackStarted(): Boolean
    
    /**
     * Called as the gesture progresses.
     * @param progress Progress from 0.0 (start) to 1.0 (threshold)
     */
    fun onBackProgress(progress: Float)
    
    /**
     * Called when the gesture is cancelled (user swipes back).
     */
    fun onBackCancelled()
    
    /**
     * Called when the gesture is committed (user releases past threshold).
     */
    fun onBackCommitted()
}

/**
 * Coordinator for predictive back gestures within QuoVadisHost.
 * 
 * This class manages the speculative pop computation and coordinates
 * the TransitionState updates during gesture-driven navigation.
 * 
 * ## Speculative Pop Algorithm
 * 
 * 1. On gesture start:
 *    - Compute what pop() would return (speculative state)
 *    - If no pop possible, ignore gesture
 *    - Enter TransitionState.Proposed with current + speculative
 * 
 * 2. On gesture progress:
 *    - Update TransitionState.Proposed.progress
 *    - TreeFlattener renders both states with progress-based animation
 * 
 * 3. On gesture commit:
 *    - Transition to TransitionState.Animating
 *    - Complete animation to speculative state
 *    - Update Navigator with speculative state
 * 
 * 4. On gesture cancel:
 *    - Animate back to original state
 *    - Return to TransitionState.Idle
 */
class PredictiveBackCoordinator(
    private val navigator: Navigator,
    private val transitionManager: TransitionStateManager
) : PredictiveBackCallback {
    
    private var speculativeState: NavNode? = null
    
    override fun onBackStarted(): Boolean {
        // Check if we can pop
        val currentState = transitionManager.currentState
        if (!currentState.isIdle) return false
        
        val current = currentState.current
        
        // Compute speculative pop result
        val speculative = TreeMutator.pop(current) ?: return false
        speculativeState = speculative
        
        // Enter Proposed state
        transitionManager.startProposed(speculative)
        return true
    }
    
    override fun onBackProgress(progress: Float) {
        transitionManager.updateProgress(progress)
    }
    
    override fun onBackCancelled() {
        // Animate back to original
        speculativeState = null
        transitionManager.cancelProposed()
    }
    
    override fun onBackCommitted() {
        val speculative = speculativeState ?: return
        
        // Commit the pop
        transitionManager.commitProposed()
        
        // Complete the navigation
        navigator.replaceState(speculative)
        speculativeState = null
    }
}

/**
 * Composable that provides predictive back handling for QuoVadisHost.
 */
@Composable
expect fun PredictiveBackHandler(
    enabled: Boolean = true,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
)
```

### Android Implementation

```kotlin
// androidMain
package com.jermey.quo.vadis.core.navigation.compose

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler as AndroidPredictiveBackHandler
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.Flow

/**
 * Android implementation of PredictiveBackHandler.
 * 
 * Uses the AndroidX PredictiveBackHandler API available on Android 14+.
 * Falls back to immediate pop on older versions.
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
) {
    var isHandling by remember { mutableStateOf(false) }
    
    AndroidPredictiveBackHandler(enabled = enabled) { progress: Flow<BackEventCompat> ->
        // Gesture started
        isHandling = callback.onBackStarted()
        if (!isHandling) return@AndroidPredictiveBackHandler
        
        try {
            progress.collect { backEvent ->
                // Map Android progress to our 0-1 range
                val normalizedProgress = backEvent.progress
                callback.onBackProgress(normalizedProgress)
            }
            
            // Gesture completed (released past threshold)
            callback.onBackCommitted()
        } catch (e: CancellationException) {
            // Gesture cancelled (swiped back)
            callback.onBackCancelled()
        } finally {
            isHandling = false
        }
    }
    
    content()
}
```

### iOS Implementation

```kotlin
// iosMain
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * iOS implementation of PredictiveBackHandler.
 * 
 * Uses a custom edge swipe gesture recognizer that mimics iOS system behavior.
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var isHandling by remember { mutableStateOf(false) }
    var startX by remember { mutableStateOf(0f) }
    
    val edgeThreshold = with(density) { 20.dp.toPx() }
    val completeThreshold = with(density) { 100.dp.toPx() }
    
    Box(
        modifier = Modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            
            detectHorizontalDragGestures(
                onDragStart = { offset ->
                    // Only handle gestures starting from left edge
                    if (offset.x <= edgeThreshold) {
                        startX = offset.x
                        isHandling = callback.onBackStarted()
                    }
                },
                onDragEnd = {
                    if (isHandling) {
                        // Check if passed threshold
                        // This is simplified - real implementation tracks final position
                        callback.onBackCommitted()
                        isHandling = false
                    }
                },
                onDragCancel = {
                    if (isHandling) {
                        callback.onBackCancelled()
                        isHandling = false
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    if (isHandling) {
                        val currentX = change.position.x
                        val progress = ((currentX - edgeThreshold) / completeThreshold).coerceIn(0f, 1f)
                        callback.onBackProgress(progress)
                    }
                }
            )
        }
    ) {
        content()
    }
}
```

### Desktop/Web Implementation

```kotlin
// desktopMain / jsMain / wasmJsMain
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable

/**
 * Desktop/Web implementation of PredictiveBackHandler.
 * 
 * Predictive back is not supported on these platforms.
 * Back actions result in immediate pop without preview.
 */
@Composable
actual fun PredictiveBackHandler(
    enabled: Boolean,
    callback: PredictiveBackCallback,
    content: @Composable () -> Unit
) {
    // No predictive back on desktop/web
    // Back is handled via keyboard (Escape) or explicit back button
    content()
}
```

### QuoVadisHost Integration

```kotlin
/**
 * Updated QuoVadisHost with predictive back support.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    animationRegistry: AnimationRegistry = AnimationRegistry.Default,
    enablePredictiveBack: Boolean = true,
    content: @Composable QuoVadisHostScope.(Destination) -> Unit
) {
    // State management
    val navState by navigator.stateFlow.collectAsState()
    val transitionManager = remember { TransitionStateManager(navState) }
    val transitionState by transitionManager.state.collectAsState()
    
    // Predictive back coordinator
    val backCoordinator = remember(navigator, transitionManager) {
        PredictiveBackCoordinator(navigator, transitionManager)
    }
    
    // Can go back check
    val canGoBack by remember(navState) {
        derivedStateOf { TreeMutator.pop(navState) != null }
    }
    
    // ... rest of QuoVadisHost implementation ...
    
    // Wrap content with PredictiveBackHandler
    PredictiveBackHandler(
        enabled = enablePredictiveBack && canGoBack,
        callback = backCoordinator
    ) {
        SharedTransitionLayout(modifier = modifier) {
            QuoVadisHostContent(
                surfaces = surfaces,
                transitionState = transitionState,
                saveableStateHolder = saveableStateHolder,
                sharedTransitionScope = this,
                animationRegistry = animationRegistry
            )
        }
    }
}
```

---

## Animation Coordination

### Visual Effects During Gesture

| Effect | Exiting Screen | Entering Screen (Below) |
|--------|----------------|-------------------------|
| **Scale** | 1.0 → 0.9 | 0.9 → 1.0 |
| **Translation** | 0 → +10% width (parallax) | Static |
| **Opacity** | 1.0 → 0.8 | 0.8 → 1.0 |
| **Corner radius** | 0 → 16dp (Android) | N/A |

### Implementation

```kotlin
/**
 * Applies predictive back visual transformations.
 */
@Composable
fun Modifier.predictiveBackModifier(
    progress: Float,
    isExiting: Boolean,
    screenWidth: Float
): Modifier {
    return this.graphicsLayer {
        if (isExiting) {
            // Exiting screen (current, on top)
            val scale = lerp(1f, 0.9f, progress)
            scaleX = scale
            scaleY = scale
            
            // Parallax translation (moves right as user swipes)
            translationX = progress * screenWidth * 0.1f
            
            // Slight fade
            alpha = lerp(1f, 0.8f, progress)
            
            // Corner radius (Android Material)
            shape = RoundedCornerShape(
                lerp(0f, 16f, progress).dp
            )
            clip = true
        } else {
            // Entering screen (previous, below)
            val scale = lerp(0.9f, 1f, progress)
            scaleX = scale
            scaleY = scale
            
            // Fade in
            alpha = lerp(0.8f, 1f, progress)
        }
    }
}

/**
 * Linear interpolation helper.
 */
private fun lerp(start: Float, end: Float, fraction: Float): Float {
    return start + (end - start) * fraction
}
```

---

## State Flow During Gesture

### Sequence Diagram

```
User          QuoVadisHost      BackCoordinator    TransitionManager    Navigator
  │                │                  │                   │                │
  ├─ swipe start ─►│                  │                   │                │
  │                ├─ onBackStarted() ►│                   │                │
  │                │                  ├─ computePop() ───►│                │
  │                │                  │◄── specState ────│                │
  │                │                  ├─ startProposed() ►│                │
  │                │                  │                   ├─ Proposed ────►│
  │                │◄─ surfaces ──────┤◄──────────────────┤                │
  │◄─ preview UI ──┤                  │                   │                │
  │                │                  │                   │                │
  ├─ swipe drag ──►│                  │                   │                │
  │                ├─ onBackProgress() ►│                   │                │
  │                │                  ├─ updateProgress() ►│                │
  │                │◄─ surfaces ──────┤◄──────────────────┤                │
  │◄─ update UI ───┤                  │                   │                │
  │                │                  │                   │                │
  ├─ swipe end ───►│                  │                   │                │
  │                ├─ onBackCommitted() ►│                   │                │
  │                │                  ├─ commitProposed() ►│                │
  │                │                  │                   ├─ Animating ───►│
  │                │                  ├─ replaceState() ─────────────────►│
  │                │                  │                   │◄── updated ────│
  │                │◄─ final surfaces ┤◄──────────────────┤                │
  │◄─ final UI ────┤                  │                   │                │
```

---

## Implementation Steps

### Step 1: Core Infrastructure

1. Create `PredictiveBackCallback` interface
2. Implement `PredictiveBackCoordinator` class
3. Define expect `PredictiveBackHandler` composable

### Step 2: Platform Implementations

1. Implement Android version with `AndroidPredictiveBackHandler`
2. Implement iOS version with edge swipe gesture
3. Implement desktop/web stub

### Step 3: Visual Transformations

1. Create `predictiveBackModifier` extension
2. Implement scale, translation, opacity effects
3. Add corner radius for Material (Android)

### Step 4: QuoVadisHost Integration

1. Add `enablePredictiveBack` parameter
2. Wire up `PredictiveBackCoordinator`
3. Update surface rendering for predictive state

### Step 5: Testing

1. Unit tests for speculative pop
2. UI tests for gesture handling
3. Platform-specific tests

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/PredictiveBackIntegration.kt` | Create | Common predictive back code |
| `quo-vadis-core/src/androidMain/.../AndroidPredictiveBack.kt` | Create | Android implementation |
| `quo-vadis-core/src/iosMain/.../IosPredictiveBack.kt` | Create | iOS implementation |
| `quo-vadis-core/src/desktopMain/.../DesktopPredictiveBack.kt` | Create | Desktop stub |
| `quo-vadis-core/src/jsMain/.../WebPredictiveBack.kt` | Create | Web stub |
| `quo-vadis-core/.../compose/QuoVadisHost.kt` | Modify | Add predictive back support |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-004 (QuoVadisHost) | Hard | Must complete first |
| RENDER-003 (TransitionState) | Hard | Used for Proposed state |
| CORE-002 (TreeMutator) | Hard | Used for speculative pop |

---

## Acceptance Criteria

- [ ] `PredictiveBackCallback` interface defined
- [ ] `PredictiveBackCoordinator` computes speculative pop correctly
- [ ] Android implementation uses `PredictiveBackHandler` API
- [ ] iOS implementation uses edge swipe gesture
- [ ] Desktop/Web stubs compile without errors
- [ ] Visual transformations match Material/iOS guidelines
- [ ] Scale effect: 1.0 → 0.9 for exiting screen
- [ ] Parallax shift applies during gesture
- [ ] Gesture cancellation animates back to original
- [ ] Gesture commit completes navigation
- [ ] QuoVadisHost `enablePredictiveBack` parameter works
- [ ] State flow is correct (Idle → Proposed → Animating/Idle)
- [ ] Comprehensive KDoc documentation
- [ ] Unit tests for coordinator logic
- [ ] UI tests for gesture handling

---

## Edge Cases

### Rapid Gestures

```kotlin
// User starts gesture, cancels, starts again quickly
if (!currentState.isIdle) return false // Ignore if already transitioning
```

### Nested Navigation

```kotlin
// Pop should affect deepest active stack
val current = transitionManager.currentState.current
val speculative = TreeMutator.pop(current) // Handles nested stacks
```

### Empty Stack

```kotlin
// Cannot pop if at root
val speculative = TreeMutator.pop(current) ?: return false
```

### Tab Navigation

```kotlin
// Consider: Should predictive back pop within tab or switch tabs?
// Default: Pop within active tab's stack
// If tab stack empty, gesture is not handled (system handles)
```

---

## Testing Notes

### Unit Tests

```kotlin
@Test
fun `speculative pop computed correctly`() {
    val screen1 = ScreenNode("s1", "stack", mockDest)
    val screen2 = ScreenNode("s2", "stack", mockDest)
    val stack = StackNode("stack", null, listOf(screen1, screen2))
    
    val coordinator = PredictiveBackCoordinator(mockNavigator, mockTransitionManager)
    
    // Simulate gesture start
    assertTrue(coordinator.onBackStarted())
    
    // Verify Proposed state was set
    verify(mockTransitionManager).startProposed(argThat { proposed ->
        (proposed as StackNode).children.size == 1
    })
}

@Test
fun `gesture cancel returns to original state`() {
    val coordinator = PredictiveBackCoordinator(mockNavigator, mockTransitionManager)
    coordinator.onBackStarted()
    
    coordinator.onBackCancelled()
    
    verify(mockTransitionManager).cancelProposed()
}

@Test
fun `gesture commit updates navigator`() {
    val coordinator = PredictiveBackCoordinator(mockNavigator, mockTransitionManager)
    coordinator.onBackStarted()
    
    coordinator.onBackCommitted()
    
    verify(mockTransitionManager).commitProposed()
    verify(mockNavigator).replaceState(any())
}
```

### UI Tests

```kotlin
@Test
fun `predictive back shows preview`() {
    // Platform-specific gesture simulation
    // Verify both screens visible during gesture
}

@Test
fun `cancel gesture returns to original`() {
    // Start gesture, drag, cancel
    // Verify original screen is shown
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost base implementation
- [RENDER-003](./RENDER-003-transition-state.md) - TransitionState.Proposed
- [CORE-002](../phase1-core/CORE-002-tree-mutator.md) - TreeMutator.pop
- [Original Architecture Plan](../../Refactoring%20Quo-Vadis%20Navigation%20Architecture.md) - Section 3.2.4 "Predictive Back with Speculative Pop"
- [Android Predictive Back](https://developer.android.com/guide/navigation/predictive-back-gesture)
- [Material Motion Guidelines](https://m3.material.io/styles/motion/overview)
