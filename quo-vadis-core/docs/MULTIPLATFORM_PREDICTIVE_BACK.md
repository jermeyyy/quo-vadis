# Multiplatform Predictive Back Implementation

## Overview

Quo Vadis provides comprehensive predictive back navigation support across all platforms via the modern NavigationEvent API.

**Recommended Approach**: Use `NavigationHost` with `predictiveBackMode` parameter for automatic predictive back handling, or `QuoVadisBackHandler` from the `navback` package for custom implementations.

---

## NavigationHost Built-in Support

Predictive back is built into `NavigationHost` via the `predictiveBackMode` parameter:

```kotlin
NavigationHost(
    navigator = navigator,
    screenRegistry = myScreenRegistry,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)
```

### PredictiveBackMode

| Mode | Description |
|------|-------------|
| `ROOT_ONLY` | Default. Only the root stack handles predictive back gestures. |
| `FULL_CASCADE` | All stacks handle predictive back, including animated cascade when popping containers. |

---

## NavigationEvent API (Recommended)

The new NavigationEvent-based API provides proper system integration on Android 14+ and consistent handling across platforms.

### Key Benefits

| Feature | Legacy API | NavigationEvent API |
|---------|-----------|---------------------|
| System predictive back animation (Android 14+) | ❌ No | ✅ Yes |
| In-app gesture animations | ✅ Yes | ✅ Yes |
| iOS edge swipe | ✅ Yes | ✅ Yes |
| System back preview (closing app) | ❌ No | ✅ Yes |
| OnBackInvokedDispatcher integration | ❌ No | ✅ Yes |

### Quick Start

```kotlin
QuoVadisBackHandler(
    enabled = canGoBack,
    currentScreenInfo = ScreenNavigationInfo(screenId = "detail"),
    previousScreenInfo = ScreenNavigationInfo(screenId = "list"),
    onBackProgress = { event -> /* Animate with event.progress */ },
    onBackCancelled = { /* Reset animation */ },
    onBackCompleted = { navigator.goBack() }
) {
    // Screen content
}
```

### API Components

#### ScreenNavigationInfo

Provides screen metadata for system animations:

```kotlin
data class ScreenNavigationInfo(
    val screenId: String,         // Unique identifier
    val displayName: String?,     // For accessibility
    val route: String?            // Route pattern
) : NavigationEventInfo()
```

#### BackNavigationEvent

Platform-agnostic gesture event:

```kotlin
data class BackNavigationEvent(
    val progress: Float,          // 0.0 to 1.0
    val touchX: Float,            // X coordinate
    val touchY: Float,            // Y coordinate
    val swipeEdge: Int            // EDGE_LEFT or EDGE_RIGHT
)
```

#### BackTransitionState

Observable transition state:

```kotlin
sealed interface BackTransitionState {
    data object Idle : BackTransitionState
    data class InProgress(val event: BackNavigationEvent) : BackTransitionState
}
```

### Usage Examples

#### Basic Usage (No Custom Animation)

```kotlin
QuoVadisBackHandler(
    enabled = canGoBack,
    onBack = { navigator.goBack() }
) {
    DetailScreen()
}
```

#### With Progress Animation

```kotlin
var animatedScale by remember { mutableFloatStateOf(1f) }
var animatedOffset by remember { mutableFloatStateOf(0f) }

QuoVadisBackHandler(
    enabled = canGoBack,
    currentScreenInfo = ScreenNavigationInfo(
        screenId = currentScreen.key,
        displayName = currentScreen.title
    ),
    previousScreenInfo = previousScreen?.let {
        ScreenNavigationInfo(screenId = it.key)
    },
    onBackProgress = { event ->
        // Scale down and slide right as user swipes
        animatedScale = 1f - (event.progress * 0.1f)
        animatedOffset = event.progress * 100f
    },
    onBackCancelled = {
        // Reset to original state
        animatedScale = 1f
        animatedOffset = 0f
    },
    onBackCompleted = {
        navigator.goBack()
    }
) {
    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
                translationX = animatedOffset
            }
    ) {
        CurrentScreen()
    }
}
```

#### With BackAnimationController

For hierarchical navigation systems:

```kotlin
@Composable
fun NavigationContainer(navigator: Navigator) {
    val backController = rememberBackAnimationController()
    
    CompositionLocalProvider(
        LocalBackAnimationController provides backController
    ) {
        QuoVadisBackHandler(
            enabled = navigator.canGoBack,
            currentScreenInfo = /* ... */,
            previousScreenInfo = /* ... */,
            onBackProgress = { event ->
                backController.updateProgress(event)
            },
            onBackCancelled = {
                backController.cancelAnimation()
            },
            onBackCompleted = {
                backController.completeAnimation()
                navigator.goBack()
            }
        ) {
            NavTreeRenderer(navigator = navigator)
        }
    }
}

// In child renderers
@Composable
fun StackItemRenderer(item: StackItem) {
    val backController = LocalBackAnimationController.current
    
    val modifier = if (backController?.isAnimating == true) {
        Modifier.graphicsLayer {
            alpha = 1f - (backController.progress * 0.3f)
            translationX = backController.progress * 50f
        }
    } else {
        Modifier
    }
    
    Box(modifier = modifier) {
        item.content()
    }
}
```

---

## Platform-Specific Behaviors

### Android 14+ (API 34+)

Full predictive back support with system integration:

- **System Preview Animation**: When closing the app (going to home), the system shows a preview of the home screen behind your app
- **In-App Gestures**: Both system and custom animations play during navigation
- **OnBackInvokedDispatcher**: Proper callback registration with correct priority
- **Nested Handlers**: Multiple handlers coordinate via priority system

```
User swipe from edge
     ↓
System starts preview animation
     ↓
QuoVadisBackHandler receives NavigationEvent
     ↓
onBackProgress called with progress (0.0 → 1.0)
     ↓
Custom animation updates
     ↓
User releases → onBackCompleted
     ↓
Navigation occurs
```

### Android 13 (API 33)

Gesture detection works, but no system preview animation:

- In-app gestures and custom animations work normally
- No system home preview when closing app
- Falls back to standard back behavior for system navigation

### Android < 13

Standard back button behavior:

- No gesture support
- Back button triggers immediate navigation
- `QuoVadisBackHandler` still works for intercepting back

### iOS

Native iOS-style edge swipe gesture:

- Swipe from left edge (within 20dp threshold)
- Progress calculated based on swipe distance
- Commit threshold at 50% of swipe distance
- Smooth gesture detection using `detectHorizontalDragGestures`

```kotlin
// iOS uses IOSEdgeSwipeGestureDetector internally
// Gesture parameters:
// - Edge threshold: 20dp (how close to edge to start)
// - Complete threshold: 150dp (full gesture distance)
// - Commit threshold: 50% (when to commit vs cancel)
```

### Desktop

No gesture support:

- Back navigation via keyboard (Escape, Back key) or UI buttons
- `QuoVadisBackHandler` intercepts back commands
- No visual gesture animations

### Web (JS/WASM)

Browser-based navigation:

- Browser back button triggers navigation
- No edge swipe gestures
- History API integration for browser navigation

---

## Migration Guide

### From PredictiveBackHandler (Compose UI)

**Before (Old API):**
```kotlin
PredictiveBackHandler(enabled = canGoBack) { backEvent ->
    backEvent.collect { event ->
        animateProgress(event.progress)
    }
    navigator.goBack()
}
```

**After (NavigationEvent API):**
```kotlin
QuoVadisBackHandler(
    enabled = canGoBack,
    currentScreenInfo = ScreenNavigationInfo(screenId = "current"),
    onBackProgress = { event -> animateProgress(event.progress) },
    onBackCompleted = { navigator.goBack() }
) {
    Content()
}
```

### From Previous API

**Before (If using separate predictive back setup):**
```kotlin
// Old approach with separate components
GraphNavHost(
    graph = graph,
    navigator = navigator,
    enablePredictiveBack = true
)
```

**After (Built into NavigationHost):**
```kotlin
// Predictive back is now built-in
NavigationHost(
    navigator = navigator,
    screenRegistry = myScreenRegistry,
    predictiveBackMode = PredictiveBackMode.FULL_CASCADE
)
```

**For Custom Animations (using QuoVadisBackHandler):**
```kotlin
QuoVadisBackHandler(
    enabled = navigator.canNavigateBack.collectAsState().value,
    currentScreenInfo = ScreenNavigationInfo(screenId = currentEntry?.key ?: ""),
    previousScreenInfo = previousEntry?.let { ScreenNavigationInfo(screenId = it.key) },
    onBackProgress = { event ->
        // Apply animation based on progress
        scale = 1f - (event.progress * 0.1f)
        offset = event.progress * 80f
    },
    onBackCancelled = { resetAnimation() },
    onBackCompleted = { navigator.navigateBack() }
) {
    // Screen content
}
```

---

## NavigationHost Animation Architecture

Predictive back animations in `NavigationHost` use a sophisticated coordination pattern:

### Key Components

1. **NavTreeRenderer** - Recursive renderer for NavNode tree
   - Dispatches to node-specific renderers (StackRenderer, TabRenderer, etc.)
   - Manages animation state across the tree

2. **PredictiveBackContent** - Gesture-aware content wrapper
   - Tracks gesture progress
   - Switches between gesture and exit animation phases
   - Coordinates with ComposableCache for screen preservation

3. **ComposableCache** (~90 lines)
   - Caches composable screens during navigation
   - Locks entries during animation to prevent destruction
   - LRU eviction respecting locked entries

4. **BackAnimationController** - Animation state management
   - Tracks `isAnimating`, `progress`, `currentEvent`
   - Methods: `startAnimation()`, `updateProgress()`, `completeAnimation()`, `cancelAnimation()`

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

---

## Summary: Which API to Use?

| Scenario | Recommended API |
|----------|-----------------|
| New project | `NavigationHost` with `predictiveBackMode` |
| Need custom gesture animations | `QuoVadisBackHandler` with `onBackProgress` |
| Android 14+ system animation | Built into `NavigationHost` |
| Hierarchical navigation with custom renderers | `BackAnimationController` |
| Simple back intercept | `QuoVadisBackHandler(onBack = {...})` |

### Quick Decision Tree

```
Standard navigation setup?
├── Yes → NavigationHost(predictiveBackMode = FULL_CASCADE)
│         ├── Built-in predictive back
│         └── Automatic animations
└── Need custom animations?
    └── QuoVadisBackHandler with onBackProgress
```

---

## References

- [AndroidX NavigationEvent Releases](https://developer.android.com/jetpack/androidx/releases/navigationevent)
- [Android Predictive Back Gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
- [API Reference - navback package](API_REFERENCE.md#navigationevent-back-handling-navigationcomposenavback)


