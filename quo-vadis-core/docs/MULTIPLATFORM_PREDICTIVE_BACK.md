# Multiplatform Predictive Back Implementation

## Overview

Successfully implemented a comprehensive predictive back navigation system that works on **both iOS and Android** with smooth animations and proper screen lifecycle management.

## Architecture

### Animation Coordinator Pattern

The implementation uses a coordinator to separate logical backstack state from visual rendering state:

```kotlin
@Stable
private class PredictiveBackAnimationCoordinator {
    var displayedCurrentEntry: BackStackEntry?
    var displayedPreviousEntry: BackStackEntry?
    var isAnimating: Boolean
    
    fun startAnimation(current, previous)  // Freezes entries
    fun finishAnimation()                   // Resumes normal rendering
    fun cancelAnimation()                   // Cleans up on gesture cancel
}
```

### Key Components

1. **PredictiveBackNavigation.kt** (~500 lines)
   - Main composable handling gesture and animation
   - Coordinates between gesture phase and exit phase
   - Manages cache locking and unlocking

2. **ComposableCache.kt** (~90 lines)
   - Caches composable screens during navigation
   - Locks entries during animation to prevent destruction
   - Automatically cleans up old entries

3. **Animation Modifiers**
   - Gesture animations: `material3BackAnimation()`, `scaleBackAnimation()`, `slideBackAnimation()`
   - Exit animations: `material3ExitAnimation()`, `scaleExitAnimation()`, `slideExitAnimation()`

## Animation Flow

### Two-Phase Animation System

**Phase 1: Gesture Animation** (User Dragging)
```
User drags back
→ gestureProgress updates (0 to ~1.0)
→ Current screen applies gesture animation
→ Previous screen rendered underneath
→ Scrim layer between screens (fading)
→ Uses selected animation type (Material3/Scale/Slide)
```

**Phase 2: Exit Animation** (After Gesture Completes)
```
User releases
→ isGesturing = false, isExitAnimating = true
→ exitProgress animates (0 to 1.0) with spring physics
→ Current screen applies exit animation (same type as gesture)
→ Animation completes fully
→ navigator.navigateBack() called
→ coordinator.finishAnimation()
→ New screen renders
```

### Timing Sequence

```
Before Fix (❌ Broken):
Gesture complete → Start animation + Immediate navigateBack()
→ Backstack updates → Screen destroyed → Animation cancelled

After Fix (✅ Working):
Gesture complete → Capture entries → Lock cache
→ Exit animation plays to completion
→ navigateBack() after animation
→ Unlock cache → Smooth transition
```

## Animation Types

### Material3 (Default)
**Gesture Phase:**
- Scale: 1.0 → 0.9
- Translate X: 0 → 80px
- Corner radius: 0dp → 16dp
- Shadow elevation: 0 → 16

**Exit Phase:**
- Scale: 0.9 → 0.7
- Translate X: 80px → 250px
- Corner radius: 16dp → 24dp
- Alpha: 1.0 → 0.0
- Shadow: 16 → 0

### Scale
**Gesture Phase:**
- Scale: 1.0 → 0.9
- Alpha: 1.0 → 0.8

**Exit Phase:**
- Scale: 0.9 → 0.6
- Alpha: 0.8 → 0.0

### Slide
**Gesture Phase:**
- Translate X: 0 → 100px
- Alpha: 1.0 → 1.0

**Exit Phase:**
- Translate X: 100px → 300px
- Alpha: 1.0 → 0.0

## Screen Rendering Strategy

### Display Logic
```kotlin
// Current screen always uses live entry (for animation updates)
val displayedCurrent = currentEntry

// Previous screen uses coordinator entry (frozen during animation)
val displayedPrevious = if (coordinator.isAnimating) {
    coordinator.displayedPreviousEntry
} else {
    null  // Don't render when not animating
}
```

### Z-Index Layering
```
Top (z-index 1.0):    Current screen with animation
Middle (z-index 0.5): Scrim layer (only during gesture)
Bottom (z-index 0.0): Previous screen (visible underneath)
```

### Cache Locking
```kotlin
LaunchedEffect(coordinator.isAnimating, currentEntry?.id, ...) {
    if (coordinator.isAnimating) {
        currentEntry?.let { cache.lockEntry(it.id) }
        previousEntry?.let { cache.lockEntry(it.id) }
    } else {
        // Unlock after animation completes
    }
}
```

## Platform Support

### Android 13+ (API 33+)
- Full predictive back gesture support
- Material 3 animations
- System back button integration
- Deep link support

### iOS
- Native iOS back gesture support
- Same animations as Android
- Navigation bar integration
- Universal link support

### Older Android (< API 33)
- Gracefully falls back to standard navigation
- No gesture animations (instant navigation)
- All other features work normally

## Usage

### Basic Usage
```kotlin
PredictiveBackNavigation(
    navigator = navigator,
    graph = appGraph,
    enabled = true
)
```

### With Custom Configuration
```kotlin
PredictiveBackNavigation(
    navigator = navigator,
    graph = appGraph,
    enabled = true,
    animationType = PredictiveBackAnimationType.Material3,
    sensitivity = 1.2f,      // 20% more sensitive
    maxCacheSize = 5         // Cache up to 5 screens
)
```

### Integration Example
```kotlin
@Composable
fun App() {
    val navigator = rememberNavigator()
    
    PredictiveBackNavigation(
        navigator = navigator,
        graph = mainNavGraph(),
        modifier = Modifier.fillMaxSize(),
        animationType = PredictiveBackAnimationType.Material3
    )
}
```

## Nested Navigation and Animation Skipping

### LocalPredictiveBackInProgress CompositionLocal

To coordinate animations across nested navigation structures (like tabs within a navigable screen), the library provides a `LocalPredictiveBackInProgress` CompositionLocal:

```kotlin
val LocalPredictiveBackInProgress = staticCompositionLocalOf { false }
```

**When it's `true`:**
- During the predictive back gesture (user is dragging)
- During the exit animation (after gesture release, before navigation completes)

**When it's `false`:**
- Normal navigation (no predictive back in progress)
- After the exit animation completes

### Why Animation Skipping is Needed

When a predictive back gesture is in progress, nested navigators (like tab containers) should skip their own animations to prevent visual glitches:

1. **Double Animation Problem**: Without skipping, both the outer predictive back animation AND inner tab switch animations would play simultaneously, creating jarring visuals
2. **State Synchronization**: The outer animation expects the inner content to remain stable during the gesture
3. **Performance**: Skipping unnecessary animations reduces rendering overhead during the gesture

### Automatic Handling in Tab Navigation

`TabbedNavHost` and `TabNavigationContainer` automatically check `LocalPredictiveBackInProgress` and skip tab switch animations when a predictive back is in progress:

- Tab switches happen instantly (no crossfade or slide)
- Ensures smooth visual experience during back gestures
- No additional configuration needed

### Custom Component Integration

If you're building custom components with animations that should respect predictive back state, you can check the CompositionLocal:

```kotlin
@Composable
fun MyNestedContent() {
    val isPredictiveBack = LocalPredictiveBackInProgress.current
    
    // Use instant transitions during predictive back
    val transitionSpec = if (isPredictiveBack) {
        TransitionSpec.None  // Instant, no animation
    } else {
        TransitionSpec.Default  // Normal animation
    }
    
    // Apply transitionSpec to your animations
    AnimatedContent(
        targetState = currentState,
        transitionSpec = {
            if (isPredictiveBack) {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn() togetherWith fadeOut()
            }
        }
    ) { state ->
        // Content
    }
}
```

### Best Practices

1. **Don't fight the gesture**: Let the outer predictive back animation drive the visual experience
2. **Skip, don't delay**: Instant state changes are better than queued animations during gestures
3. **Restore after completion**: Normal animations resume automatically when `LocalPredictiveBackInProgress` becomes `false`

## Technical Implementation Details

### Gesture Handler
```kotlin
PredictiveBackHandler(
    enabled = enabled && canGoBack && !isExitAnimating
) { backEvent ->
    // Capture entries before any state changes
    coordinator.startAnimation(currentEntry, previousEntry)
    isGesturing = true
    
    try {
        backEvent.collect { event ->
            gestureProgress = event.progress * sensitivity
        }
        // ... exit animation
    } catch {
        // ... cancellation handling
    }
}
```

### Spring Animation Spec
```kotlin
exitAnimProgress.animateTo(
    targetValue = 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
)
```

### Composable Cache
```kotlin
class ComposableCache(maxCacheSize: Int = 3) {
    private val accessTimeMap = mutableStateMapOf<String, Long>()
    private val lockedEntries = mutableStateSetOf<String>()
    
    fun lockEntry(entryId: String) {
        lockedEntries.add(entryId)
    }
    
    fun unlockEntry(entryId: String) {
        lockedEntries.remove(entryId)
    }
    
    // Cache cleanup respects locked entries
    val oldestEntry = accessTimeMap.entries
        .filter { it.key !in lockedEntries }
        .minByOrNull { it.value }
}
```

## Performance Considerations

- **Minimal Overhead**: Coordinator adds negligible memory/CPU cost
- **Efficient Animations**: Uses `graphicsLayer` for GPU-accelerated rendering
- **Smart Caching**: Only keeps necessary screens in memory
- **Reactive Updates**: StateFlow prevents unnecessary recompositions

## Testing

The implementation has been tested with:
- ✅ Multiple rapid back gestures
- ✅ Gesture cancellation (drag back then forward)
- ✅ Different animation types
- ✅ Various sensitivity settings
- ✅ Screen rotation
- ✅ Deep link navigation during gestures
- ✅ Memory leak prevention

## Future Enhancements

Potential improvements:
- Configurable animation duration
- Custom animation callbacks
- Haptic feedback integration
- Animation performance metrics
- Platform-specific animation tuning


