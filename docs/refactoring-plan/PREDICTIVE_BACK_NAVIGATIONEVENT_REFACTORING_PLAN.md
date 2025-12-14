# Predictive Back Migration to NavigationEvent API

## Overview

This document outlines the plan to implement predictive back handling using the new AndroidX `NavigationEvent` library for the **new architecture** navigation components. The current implementation provides back gesture handling but lacks proper predictive back animations, both for in-app navigation and system-level transitions (closing app).

> **Scope:** This plan applies ONLY to the new architecture components (`NavigationHost`, `NavTreeRenderer`, etc.). Legacy components like `GraphNavHost` will be marked as deprecated but NOT modified, to facilitate easier cleanup after the architecture refactor is complete.

### Problem Statement

**Current State:**
- Back handling works (navigation occurs)
- **NO** predictive back animations during gesture
- **NO** system-level predictive back animation when closing app
- Using deprecated `androidx.compose.ui.backhandler.PredictiveBackHandler` from Compose 1.10

**Root Cause:**
The `PredictiveBackHandler` from `org.jetbrains.compose.ui:ui-backhandler` is a simplified cross-platform API that:
1. Only provides a Flow of progress values
2. Does **NOT** integrate with Android's `OnBackInvokedDispatcher`
3. Does **NOT** register callbacks with the correct priority for system animation
4. Does **NOT** provide `NavigationEventInfo` for system-to-animation coordination

**Target State:**
- Smooth predictive back animations during in-app navigation
- System-level predictive back animations when closing app (Android 14+)
- Native iOS swipe-to-back gesture integration
- No-op implementations for Web/Desktop

---

## Technology Analysis

### Current Dependencies

```toml
# gradle/libs.versions.toml
composeMultiplatform = "1.10.0-rc02"
compose-backhandler = { module = "org.jetbrains.compose.ui:ui-backhandler", version.ref = "composeMultiplatform" }
```

### New NavigationEvent Library

The NavigationEvent library (`androidx.navigationevent`) is now stable (1.0.1) and available for Compose Multiplatform via JetBrains fork:

```toml
# To add:
navigationevent-compose = { module = "org.jetbrains.androidx.navigationevent:navigationevent-compose", version = "1.0.0-rc02" }
```

### Key API Differences

| Feature | Old (`PredictiveBackHandler`) | New (`NavigationEvent`) |
|---------|------------------------------|-------------------------|
| Progress events | `Flow<BackEventCompat>` | `NavigationEvent` with `progress`, `touchX`, `touchY`, `swipeEdge` |
| Lifecycle | `onBackStarted` implied | Explicit `onBackStarted()`, `onBackProgressed()`, `onBackCompleted()`, `onBackCancelled()` |
| System integration | None | Full `OnBackInvokedDispatcher` integration via `OnBackInvokedDefaultInput` |
| Info passing | None | `NavigationEventInfo` for passing context between screens |
| Multiplatform | Simplified noop | Full KMP support with platform-specific inputs |
| Transition state | Manual | `TransitionState` (Idle/InProgress) |
| Compose API | `PredictiveBackHandler { flow -> }` | `NavigationBackHandler(state, onBackCompleted = {})` |

### NavigationEvent Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    NavigationEventDispatcher                     │
│  - Manages handlers and inputs                                   │
│  - Coordinates between system and navigation                     │
│  - Provides transitionState and history StateFlows              │
├─────────────────────────────────────────────────────────────────┤
│                     NavigationEventHandler                       │
│  - onBackStarted(), onBackProgressed()                          │
│  - onBackCompleted(), onBackCancelled()                         │
│  - currentInfo, backInfo, forwardInfo                           │
├─────────────────────────────────────────────────────────────────┤
│                    NavigationEventInput                          │
│  - OnBackInvokedDefaultInput (Android)                          │
│  - DirectNavigationEventInput (manual events)                   │
│  - Custom inputs for other platforms                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Current Implementation Analysis

### Files Using PredictiveBackHandler

| File | Purpose | Action |
|------|---------|--------|
| `GraphNavHost.kt` | Legacy navigation host | **DEPRECATE** - Mark as deprecated, no functional changes |
| `PredictiveBackNavigation.kt` | Legacy standalone component | **DEPRECATE** - Mark as deprecated, no functional changes |
| `NavigationHost.kt` | New hierarchical navigation host | **UPDATE** - Integrate new API |
| `NavTreeRenderer.kt` | New tree-based renderer | **UPDATE** - May need back handling |
| `PredictiveBackIntegration.kt` | Custom expect/actual definitions | **DEPRECATE** - Mark as deprecated |
| `PredictiveBackHandler.android.kt` | Android implementation | **DEPRECATE** - Mark as deprecated |
| `PredictiveBackHandler.ios.kt` | iOS implementation | **DEPRECATE** - Mark as deprecated |
| `PredictiveBackHandler.desktop.kt` | Desktop noop | **DEPRECATE** - Mark as deprecated |
| `PredictiveBackHandler.js.kt` | JS noop | **DEPRECATE** - Mark as deprecated |
| `PredictiveBackHandler.wasmJs.kt` | WASM noop | **DEPRECATE** - Mark as deprecated |

### Components to Update (New Architecture Only)

| Component | Location | Purpose |
|-----------|----------|---------|
| `NavigationHost` | `compose/` | Main entry point for new navigation |
| `NavTreeRenderer` | `compose/hierarchical/` | Renders navigation tree |
| `StackRenderer` | `compose/hierarchical/` | Renders stack navigation |
| `TabRenderer` | `compose/hierarchical/` | Renders tab navigation |
| `PaneRenderer` | `compose/hierarchical/` | Renders pane layouts |

### Current Animation Flow (Broken)

```
User swipe gesture
     ↓
PredictiveBackHandler { flow -> }  ← Only receives progress values
     ↓
Custom animation via graphicsLayer   ← Works but NO system animation
     ↓
flow.collect { progress }
     ↓
On complete: navigator.navigateBack()
     ↓
Screen pops (no system transition)   ← System doesn't animate
```

### Why System Animation Doesn't Work

1. **Missing `OnBackInvokedCallback` registration** - The current Android implementation wraps `androidx.activity.compose.PredictiveBackHandler` but doesn't register with proper priority for system animation
2. **No `NavigationEventInfo`** - System needs to know "what comes next" to render preview
3. **Custom compositor** - The library uses its own `graphicsLayer` transforms instead of system-managed transitions
4. **No activity-level integration** - The dispatcher isn't connected to `ComponentActivity.navigationEventDispatcher`

---

## Target Architecture

### New Hierarchy

```
ComponentActivity (Android)
     ↓
NavigationEventDispatcherOwner
     ↓
LocalNavigationEventDispatcherOwner (CompositionLocal)
     ↓
NavigationEventDispatcher (per-host)
     ↓
NavigationBackHandler composables (in screens)
     ↓
NavigationEventState (observable)
     ↓
TransitionState for animations
```

### Platform-Specific Behavior

| Platform | Behavior |
|----------|----------|
| **Android 14+** | Full predictive back with system animation |
| **Android 13** | Gesture detection, no system animation preview |
| **Android <13** | Falls back to immediate back |
| **iOS** | Edge swipe gesture with custom animation |
| **Desktop** | No gesture, back via keyboard/UI |
| **Web** | Browser back button, no gesture |

---

## Implementation Plan

### Phase 1: Add NavigationEvent Dependency

#### Task 1.1: Update gradle/libs.versions.toml

Add the NavigationEvent library dependency:

```toml
[versions]
# Add:
navigationevent = "1.0.0-rc02"

[libraries]
# Add:
navigationevent-compose = { module = "org.jetbrains.androidx.navigationevent:navigationevent-compose", version.ref = "navigationevent" }
```

**Effort:** 15 minutes

#### Task 1.2: Update quo-vadis-core/build.gradle.kts

Add dependency to the library:

```kotlin
commonMain {
    dependencies {
        // Add:
        implementation(libs.navigationevent.compose)
    }
}
```

**Effort:** 15 minutes

### Phase 2: Create NavigationEvent Abstraction Layer

#### Task 2.1: Create Platform-Agnostic Navigation Event Types

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/navback/BackNavigationEvent.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Immutable

/**
 * Platform-agnostic representation of a back navigation event.
 * 
 * This provides a consistent API across platforms while allowing
 * platform-specific implementations to leverage native capabilities.
 */
@Immutable
public data class BackNavigationEvent(
    /** Progress of the back gesture, 0.0 to 1.0 */
    val progress: Float,
    /** X coordinate of touch, if available */
    val touchX: Float = 0f,
    /** Y coordinate of touch, if available */
    val touchY: Float = 0f,
    /** Edge from which the swipe started (0 = LEFT, 1 = RIGHT) */
    val swipeEdge: Int = EDGE_LEFT
) {
    public companion object {
        public const val EDGE_LEFT: Int = 0
        public const val EDGE_RIGHT: Int = 1
    }
}

/**
 * Represents the state of a back navigation transition.
 */
public sealed interface BackTransitionState {
    /** No back gesture in progress */
    public data object Idle : BackTransitionState
    
    /** Back gesture is in progress */
    public data class InProgress(
        val event: BackNavigationEvent,
        val currentScreenKey: String?,
        val previousScreenKey: String?
    ) : BackTransitionState
}
```

**Effort:** 1 hour

#### Task 2.2: Create NavigationEventInfo for Screens

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/navback/ScreenNavigationInfo.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.navigationevent.NavigationEventInfo

/**
 * NavigationEventInfo implementation for Quo Vadis screens.
 * 
 * This allows the system to know what screen is displayed and
 * what screen will be revealed during predictive back.
 */
public data class ScreenNavigationInfo(
    /** Unique identifier for the screen */
    val screenKey: String,
    /** Human-readable title for the screen (for accessibility) */
    val title: String? = null,
    /** Route of the screen destination */
    val route: String? = null
) : NavigationEventInfo()

/**
 * Represents "no screen" info for when we're at the root.
 */
public object NoScreenInfo : NavigationEventInfo()
```

**Effort:** 30 minutes

### Phase 3: Create New Back Handler Components

#### Task 3.1: Create Composable NavigationBackHandler Wrapper

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/navback/QuoVadisBackHandler.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.*
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * Quo Vadis back handler that integrates with the NavigationEvent API.
 * 
 * This composable provides predictive back gesture support with proper
 * system animation integration on Android 14+.
 * 
 * @param enabled Whether back handling is enabled
 * @param currentScreenInfo Info about the currently displayed screen
 * @param previousScreenInfo Info about the screen that will be revealed
 * @param onBackProgress Callback for progress updates during gesture (0.0 to 1.0)
 * @param onBackCancelled Callback when gesture is cancelled
 * @param onBackCompleted Callback when back navigation should occur
 * @param content The content to display
 */
@Composable
public fun QuoVadisBackHandler(
    enabled: Boolean = true,
    currentScreenInfo: ScreenNavigationInfo,
    previousScreenInfo: ScreenNavigationInfo? = null,
    onBackProgress: ((BackNavigationEvent) -> Unit)? = null,
    onBackCancelled: (() -> Unit)? = null,
    onBackCompleted: () -> Unit,
    content: @Composable () -> Unit
) {
    val navEventState = rememberNavigationEventState(
        currentInfo = currentScreenInfo,
        backInfo = previousScreenInfo ?: NoScreenInfo
    )
    
    // Observe transition state for animation progress
    val transitionState by navEventState.transitionState.collectAsState()
    
    // Convert NavigationEvent to BackNavigationEvent and notify
    LaunchedEffect(transitionState) {
        when (val state = transitionState) {
            is NavigationEventTransitionState.InProgress -> {
                val event = state.latestEvent
                onBackProgress?.invoke(
                    BackNavigationEvent(
                        progress = event.progress,
                        touchX = event.touchX,
                        touchY = event.touchY,
                        swipeEdge = event.swipeEdge
                    )
                )
            }
            is NavigationEventTransitionState.Idle -> {
                // No-op for idle
            }
        }
    }
    
    NavigationBackHandler(
        state = navEventState,
        isBackEnabled = enabled,
        onBackCancelled = { onBackCancelled?.invoke() },
        onBackCompleted = onBackCompleted
    )
    
    content()
}
```

**Effort:** 2 hours

#### Task 3.2: Create Platform Expect/Actual for Input Registration

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/navback/PlatformBackInput.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.runtime.Composable

/**
 * Platform-specific back input registration.
 * 
 * This ensures the NavigationEventDispatcher receives system back events
 * on platforms that support it.
 */
@Composable
internal expect fun RegisterPlatformBackInput()
```

**Android Implementation:** `PlatformBackInput.android.kt`
```kotlin
@Composable
internal actual fun RegisterPlatformBackInput() {
    // On Android, the Activity already provides NavigationEventDispatcher
    // via NavigationEventDispatcherOwner (since Activity 1.12.0)
    // The NavigationBackHandler automatically connects to it
    // No additional registration needed
}
```

**iOS Implementation:** `PlatformBackInput.ios.kt`
```kotlin
@Composable
internal actual fun RegisterPlatformBackInput() {
    // iOS back gesture is handled via UIKit edge swipe
    // Need to create a custom input that translates iOS gestures
    // to NavigationEvents
}
```

**Desktop/JS/WASM Implementation:**
```kotlin
@Composable
internal actual fun RegisterPlatformBackInput() {
    // No platform back gesture - noop
}
```

**Effort:** 3 hours

### Phase 4: Integrate with New Architecture Navigation Host

#### Task 4.1: Update NavigationHost.kt

Update the new architecture navigation host to use `QuoVadisBackHandler`:

```kotlin
QuoVadisBackHandler(
    enabled = enablePredictiveBack && canGoBack,
    currentScreenInfo = ScreenNavigationInfo(
        screenKey = currentScreenNode?.key ?: "",
        route = currentScreenNode?.destination?.route
    ),
    previousScreenInfo = previousScreenNode?.let {
        ScreenNavigationInfo(
            screenKey = it.key,
            route = it.destination?.route
        )
    },
    onBackProgress = { event ->
        backCoordinator.onBackProgress(event.progress)
    },
    onBackCancelled = { backCoordinator.onBackCancelled() },
    onBackCompleted = { backCoordinator.onBackCommitted() }
) {
    // Content
}
```

**Effort:** 3 hours

#### Task 4.2: Update NavTreeRenderer Components

Ensure the tree renderer components properly propagate back handling state:

- `StackRenderer` - Handle back within stack context
- `TabRenderer` - Handle tab-specific back behavior
- `PaneRenderer` - Handle pane-specific back behavior

**Effort:** 4 hours

#### Task 4.3: Create Back Animation Controller for New Architecture

**New File:** `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/hierarchical/BackAnimationController.kt`

Create a dedicated controller for managing predictive back animations in the new architecture:

```kotlin
/**
 * Controller for predictive back animations in the hierarchical navigation system.
 * 
 * Manages animation state, progress tracking, and coordinates with the
 * NavigationEvent API for system-level predictive back support.
 */
@Stable
class BackAnimationController {
    var isAnimating by mutableStateOf(false)
        private set
    
    var progress by mutableFloatStateOf(0f)
        private set
    
    var currentScreenKey by mutableStateOf<String?>(null)
        private set
    
    var previousScreenKey by mutableStateOf<String?>(null)
        private set
    
    fun startAnimation(current: String?, previous: String?) { /* ... */ }
    fun updateProgress(progress: Float) { /* ... */ }
    fun completeAnimation() { /* ... */ }
    fun cancelAnimation() { /* ... */ }
}
```

**Effort:** 2 hours

### Phase 5: iOS Native Integration

#### Task 5.1: Create iOS NavigationEvent Input

**New File:** `quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/navback/IOSEdgeSwipeInput.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose.navback

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * iOS edge swipe gesture input that generates NavigationEvents.
 * 
 * Detects swipes from the left edge and converts them to NavigationEvent flow
 * for consistent handling with Android predictive back.
 */
@Composable
internal fun IOSEdgeSwipeGestureDetector(
    enabled: Boolean,
    onGestureStart: () -> Boolean,
    onProgress: (Float) -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var isHandling by remember { mutableStateOf(false) }
    var startX by remember { mutableStateOf(0f) }
    
    val edgeThreshold = with(density) { 20.dp.toPx() }
    val completeThreshold = with(density) { 100.dp.toPx() }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        if (offset.x <= edgeThreshold) {
                            startX = offset.x
                            isHandling = onGestureStart()
                        }
                    },
                    onDragEnd = {
                        if (isHandling) {
                            onComplete()
                            isHandling = false
                        }
                    },
                    onDragCancel = {
                        if (isHandling) {
                            onCancel()
                            isHandling = false
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        if (isHandling) {
                            val progress = ((change.position.x - edgeThreshold) / completeThreshold)
                                .coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                )
            }
    ) {
        content()
    }
}
```

**Effort:** 3 hours

#### Task 5.2: Update iOS PlatformBackInput

```kotlin
@Composable
internal actual fun RegisterPlatformBackInput() {
    // The iOS gesture detection is handled by IOSEdgeSwipeGestureDetector
    // which wraps content in QuoVadisBackHandler
    // No additional platform registration needed
}
```

**Effort:** 1 hour

### Phase 6: Testing

#### Task 6.1: Unit Tests for BackNavigationEvent

Test scenarios:
- Event creation with all parameters
- Edge constants
- State transitions

**Effort:** 1 hour

#### Task 6.2: Integration Tests for QuoVadisBackHandler

Test scenarios:
- Progress callback receives events
- Cancelled callback on gesture cancel
- Completed callback triggers navigation
- Disabled state doesn't handle events

**Effort:** 2 hours

#### Task 6.3: Manual Testing Checklist

| Platform | Test | Expected Result |
|----------|------|-----------------|
| Android 14+ | Swipe back in app | System preview animation + custom animation |
| Android 14+ | Swipe back to home | System home preview animation |
| Android 13 | Swipe back in app | Custom animation (no system preview) |
| Android 13 | Swipe back to home | Immediate close (no preview) |
| iOS | Edge swipe back | Custom animation with native feel |
| Desktop | Escape key | Immediate back navigation |
| Web | Browser back | History navigation |

**Effort:** 3 hours

### Phase 7: Documentation

#### Task 7.1: Update API Documentation

Update the following docs:
- `quo-vadis-core/docs/API_REFERENCE.md` - Add QuoVadisBackHandler section
- `quo-vadis-core/docs/MULTIPLATFORM_PREDICTIVE_BACK.md` - Update for NavigationEvent

**Effort:** 2 hours

---

## Implementation Order & Timeline

```
Phase 1: Dependencies (30 min)
├── Task 1.1: Update libs.versions.toml
└── Task 1.2: Update build.gradle.kts

Phase 2: Abstraction Layer (1.5 hours)
├── Task 2.1: BackNavigationEvent types
└── Task 2.2: ScreenNavigationInfo

Phase 3: New Components (5 hours)
├── Task 3.1: QuoVadisBackHandler
└── Task 3.2: Platform expect/actual

Phase 4: New Architecture Integration (9 hours)
├── Task 4.1: NavigationHost
├── Task 4.2: NavTreeRenderer components
└── Task 4.3: BackAnimationController

Phase 5: iOS Integration (4 hours)
├── Task 5.1: IOSEdgeSwipeInput
└── Task 5.2: Update PlatformBackInput

Phase 6: Deprecate Legacy APIs (2 hours)
├── Task 6.1: Deprecate GraphNavHost
├── Task 6.2: Deprecate PredictiveBackNavigation
├── Task 6.3: Deprecate PredictiveBackHandler expect/actual
└── Task 6.4: Deprecate PredictiveBackCallback/Coordinator

Phase 7: Testing (6 hours)
├── Task 7.1: Unit tests
├── Task 7.2: Integration tests
└── Task 7.3: Manual testing

Phase 8: Documentation (2 hours)
└── Task 8.1: API docs

Total Estimated Effort: ~30 hours
```

---

## Risk Assessment

| Risk | Impact | Likelihood | Mitigation |
|------|--------|------------|------------|
| NavigationEvent API changes | HIGH | LOW | Library is stable 1.0.1 |
| Breaking existing navigation | HIGH | MEDIUM | Feature flag for rollback, extensive testing |
| iOS gesture conflicts | MEDIUM | MEDIUM | Careful gesture threshold tuning |
| Performance regression | MEDIUM | LOW | Profile animation frame rates |
| Activity 1.12.0 requirement | LOW | LOW | Already using 1.12.0 in dependencies |

---

## Dependencies & Requirements

### Minimum Versions

| Dependency | Required Version | Current Version |
|------------|------------------|-----------------|
| Compose Multiplatform | 1.10.0 | 1.10.0-rc02 ✅ |
| AndroidX Activity | 1.12.0 | 1.12.0 ✅ |
| Kotlin | 2.2.20+ | 2.2.21 ✅ |
| Android minSdk | 24 | 24 ✅ |

### New Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| `org.jetbrains.androidx.navigationevent:navigationevent-compose` | 1.0.0-rc02 | Multiplatform NavigationEvent API |

---

## Success Criteria

1. ✅ **System animation works** - Android 14+ shows system predictive back preview
2. ✅ **In-app animation works** - Custom transition animation plays during gesture
3. ✅ **iOS gesture works** - Edge swipe triggers back with animation
4. ✅ **New architecture only** - Only `NavigationHost` and related components use new API
5. ✅ **Legacy deprecated** - All legacy back handling APIs marked as `@Deprecated`
6. ✅ **Legacy functional** - Deprecated APIs still work for backward compatibility
7. ✅ **Desktop/Web unaffected** - No-op on unsupported platforms
8. ✅ **Demo app demonstrates** - Predictive back visible in demo (when using new host)
9. ✅ **Tests pass** - Unit and integration tests cover new code

---

## API Usage Examples

### Basic Usage (New Architecture)

```kotlin
@Composable
fun NavigationHost(
    navigator: TreeNavigator,
    // ...
) {
    val currentNode by navigator.currentScreenNode.collectAsState()
    val previousNode by navigator.previousScreenNode.collectAsState()
    
    QuoVadisBackHandler(
        enabled = navigator.canGoBack,
        currentScreenInfo = ScreenNavigationInfo(
            screenKey = currentNode?.key ?: "",
            title = "Current Screen"
        ),
        previousScreenInfo = previousNode?.let {
            ScreenNavigationInfo(screenKey = it.key, title = "Previous")
        },
        onBackCompleted = { navigator.navigateBack() }
    ) {
        // Hierarchical navigation content
        NavTreeRenderer(
            rootNode = navigator.rootNode,
            // ...
        )
    }
}
```

### With Custom Animation (New Architecture)

```kotlin
@Composable
fun NavigationHostWithAnimation(
    navigator: TreeNavigator,
    backAnimationController: BackAnimationController = remember { BackAnimationController() }
) {
    QuoVadisBackHandler(
        enabled = navigator.canGoBack,
        currentScreenInfo = /* ... */,
        previousScreenInfo = /* ... */,
        onBackProgress = { event ->
            backAnimationController.updateProgress(event.progress)
        },
        onBackCancelled = {
            backAnimationController.cancelAnimation()
        },
        onBackCompleted = {
            backAnimationController.completeAnimation()
            navigator.navigateBack()
        }
    ) {
        // Provide animation controller to children
        CompositionLocalProvider(
            LocalBackAnimationController provides backAnimationController
        ) {
            NavTreeRenderer(/* ... */)
        }
    }
}
```

---

## Deprecation Strategy

The following legacy components will be marked as **@Deprecated** to facilitate easier cleanup:

| Component | Deprecation Message |
|-----------|--------------------|
| `GraphNavHost.kt` | "Use NavigationHost instead. This will be removed in a future release." |
| `PredictiveBackNavigation.kt` | "Use NavigationHost with QuoVadisBackHandler instead." |
| `PredictiveBackIntegration.kt` | "Use QuoVadisBackHandler from navback package instead." |
| `PredictiveBackHandler` (expect/actual) | "Use QuoVadisBackHandler for predictive back with system animation support." |
| `PredictiveBackCallback` | "Use QuoVadisBackHandler callbacks instead." |
| `PredictiveBackCoordinator` | "Use BackAnimationController instead." |

### Deprecation Level

All legacy APIs will use `DeprecationLevel.WARNING` to allow gradual migration while clearly signaling intent to remove.

```kotlin
@Deprecated(
    message = "Use NavigationHost instead. This will be removed in a future release.",
    replaceWith = ReplaceWith("NavigationHost"),
    level = DeprecationLevel.WARNING
)
```

These deprecated components will be removed as part of the architecture refactor completion.

---

## References

- [AndroidX NavigationEvent Releases](https://developer.android.com/jetpack/androidx/releases/navigationevent)
- [Compose Multiplatform 1.10 What's New](https://kotlinlang.org/docs/multiplatform/whats-new-compose-110.html)
- [Android Predictive Back Gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture)
- [Navigation 3 Documentation](https://developer.android.com/guide/navigation/navigation-3)
