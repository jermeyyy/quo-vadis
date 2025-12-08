`n
# RENDER-009: WindowSizeClass Integration for PaneNode

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | RENDER-009 |
| **Task Name** | WindowSizeClass Integration for PaneNode |
| **Phase** | Phase 2: Unified Renderer |
| **Complexity** | Medium |
| **Estimated Time** | 2 days |
| **Dependencies** | RENDER-002C |
| **Blocked By** | RENDER-002C |
| **Blocks** | RENDER-004 |

---

## Overview

Integrate window size observation for PaneNode adaptive rendering. PaneNode should render as StackNode on small screens and multi-pane on large screens.

### Purpose

This task implements window size observation and classification to enable adaptive rendering behavior for `PaneNode`. The system must:

1. **Observe window size** across all platforms (Android, iOS, Desktop, Web)
2. **Classify dimensions** into semantic size classes
3. **React to changes** with smooth transitions
4. **Preserve state** during resize operations

### Why WindowSizeClass?

Modern applications run on diverse screen sizes:

- **Phones** - Compact width, sequential navigation preferred
- **Tablets** - Medium width, two-pane layouts possible
- **Desktops** - Expanded width, full multi-pane layouts
- **Foldables** - Dynamic size changes based on fold state

By classifying window size into semantic categories, the rendering system can make intelligent layout decisions without hardcoding pixel values.

### Integration Point

```
Window Size Changed → calculateWindowSizeClass() → QuoVadisHost → TreeFlattener → PaneNode Adaptive Rendering
       │                        │                       │                │
       │                        │                       │                └── PANE_AS_STACK or PANE_WRAPPER
       │                        │                       └── Passes to flattener
       │                        └── Platform-specific calculation
       └── Platform window resize event
```

---

## File Locations

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.kt
quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.android.kt
quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.ios.kt
quo-vadis-core/src/desktopMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.desktop.kt
quo-vadis-core/src/jsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.js.kt
quo-vadis-core/src/wasmJsMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/WindowSizeClass.wasmJs.kt
```

---

## Implementation

### Core Data Structures

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Classification of window width into semantic size categories.
 * 
 * Based on Material Design 3 breakpoints:
 * - [Compact]: < 600dp - phones in portrait, small tablets
 * - [Medium]: 600dp - 840dp - tablets, foldables, large phones in landscape
 * - [Expanded]: > 840dp - large tablets, desktops
 * 
 * @see <a href="https://m3.material.io/foundations/layout/applying-layout/window-size-classes">Material Design Window Size Classes</a>
 */
enum class WindowWidthSizeClass {
    /**
     * Compact width class for screens < 600dp wide.
     * 
     * Typical devices: phones in portrait orientation, small tablets.
     * Recommended layout: Single-column, sequential navigation.
     */
    Compact,
    
    /**
     * Medium width class for screens 600dp - 840dp wide.
     * 
     * Typical devices: tablets, foldables, large phones in landscape.
     * Recommended layout: Two-column layouts, list-detail patterns.
     */
    Medium,
    
    /**
     * Expanded width class for screens > 840dp wide.
     * 
     * Typical devices: large tablets, desktops, laptops.
     * Recommended layout: Multi-column layouts, all panes visible.
     */
    Expanded;
    
    companion object {
        /**
         * Breakpoint between Compact and Medium (600dp).
         */
        val CompactMaxWidth = 600.dp
        
        /**
         * Breakpoint between Medium and Expanded (840dp).
         */
        val MediumMaxWidth = 840.dp
        
        /**
         * Calculates the width size class from a width value.
         * 
         * @param width The window width in Dp
         * @return The corresponding [WindowWidthSizeClass]
         */
        fun fromWidth(width: Dp): WindowWidthSizeClass = when {
            width < CompactMaxWidth -> Compact
            width < MediumMaxWidth -> Medium
            else -> Expanded
        }
    }
}

/**
 * Classification of window height into semantic size categories.
 * 
 * Based on Material Design 3 breakpoints:
 * - [Compact]: < 480dp - phones in landscape
 * - [Medium]: 480dp - 900dp - tablets, phones in portrait
 * - [Expanded]: > 900dp - large tablets, desktops
 */
enum class WindowHeightSizeClass {
    /**
     * Compact height class for screens < 480dp tall.
     * 
     * Typical devices: phones in landscape orientation.
     * Recommended layout: Minimize vertical chrome, prioritize content.
     */
    Compact,
    
    /**
     * Medium height class for screens 480dp - 900dp tall.
     * 
     * Typical devices: tablets, phones in portrait.
     * Recommended layout: Standard layouts with navigation elements.
     */
    Medium,
    
    /**
     * Expanded height class for screens > 900dp tall.
     * 
     * Typical devices: large tablets, desktops.
     * Recommended layout: Full layouts with all navigation visible.
     */
    Expanded;
    
    companion object {
        /**
         * Breakpoint between Compact and Medium (480dp).
         */
        val CompactMaxHeight = 480.dp
        
        /**
         * Breakpoint between Medium and Expanded (900dp).
         */
        val MediumMaxHeight = 900.dp
        
        /**
         * Calculates the height size class from a height value.
         * 
         * @param height The window height in Dp
         * @return The corresponding [WindowHeightSizeClass]
         */
        fun fromHeight(height: Dp): WindowHeightSizeClass = when {
            height < CompactMaxHeight -> Compact
            height < MediumMaxHeight -> Medium
            else -> Expanded
        }
    }
}

/**
 * Represents the semantic classification of a window's dimensions.
 * 
 * This class combines width and height size classes to provide a complete
 * picture of the available screen real estate. It's used by the rendering
 * system to make adaptive layout decisions.
 * 
 * ## Usage with PaneNode
 * 
 * The [widthSizeClass] determines how [PaneNode] renders:
 * - [WindowWidthSizeClass.Compact] → Single pane (stack-like behavior)
 * - [WindowWidthSizeClass.Medium] → Two panes visible
 * - [WindowWidthSizeClass.Expanded] → All panes visible
 * 
 * ## Example
 * 
 * ```kotlin
 * @Composable
 * fun MyApp() {
 *     val windowSizeClass = calculateWindowSizeClass()
 *     
 *     when (windowSizeClass.widthSizeClass) {
 *         WindowWidthSizeClass.Compact -> CompactLayout()
 *         WindowWidthSizeClass.Medium -> MediumLayout()
 *         WindowWidthSizeClass.Expanded -> ExpandedLayout()
 *     }
 * }
 * ```
 * 
 * @property widthSizeClass The semantic classification of window width
 * @property heightSizeClass The semantic classification of window height
 */
@Immutable
data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
) {
    companion object {
        /**
         * Calculates [WindowSizeClass] from a [DpSize].
         * 
         * @param size The window size in Dp units
         * @return The corresponding [WindowSizeClass]
         */
        fun calculateFromSize(size: DpSize): WindowSizeClass {
            return WindowSizeClass(
                widthSizeClass = WindowWidthSizeClass.fromWidth(size.width),
                heightSizeClass = WindowHeightSizeClass.fromHeight(size.height)
            )
        }
        
        /**
         * Calculates [WindowSizeClass] from width and height values.
         * 
         * @param width The window width in Dp
         * @param height The window height in Dp
         * @return The corresponding [WindowSizeClass]
         */
        fun calculateFromSize(width: Dp, height: Dp): WindowSizeClass {
            return calculateFromSize(DpSize(width, height))
        }
        
        /**
         * Default compact window size class (for phones).
         */
        val Compact = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Compact,
            heightSizeClass = WindowHeightSizeClass.Medium
        )
        
        /**
         * Default medium window size class (for tablets).
         */
        val Medium = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Medium,
            heightSizeClass = WindowHeightSizeClass.Medium
        )
        
        /**
         * Default expanded window size class (for desktops).
         */
        val Expanded = WindowSizeClass(
            widthSizeClass = WindowWidthSizeClass.Expanded,
            heightSizeClass = WindowHeightSizeClass.Expanded
        )
    }
    
    /**
     * Returns true if this represents a compact width (phone-like).
     */
    val isCompactWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.Compact
    
    /**
     * Returns true if this represents a medium or larger width.
     */
    val isAtLeastMediumWidth: Boolean
        get() = widthSizeClass != WindowWidthSizeClass.Compact
    
    /**
     * Returns true if this represents an expanded width (desktop-like).
     */
    val isExpandedWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.Expanded
}

/**
 * Calculates the current window size class.
 * 
 * This is the main entry point for obtaining window size information.
 * Each platform provides its own implementation that observes the
 * actual window dimensions and converts them to a [WindowSizeClass].
 * 
 * The returned value will automatically recompose when the window
 * size changes (e.g., device rotation, window resize).
 * 
 * ## Platform Implementations
 * 
 * - **Android**: Uses `calculateWindowSizeClass()` from Material3
 * - **iOS**: Calculates from `UIScreen.main.bounds`
 * - **Desktop**: Calculates from window size state
 * - **Web (JS/Wasm)**: Calculates from `window.innerWidth/innerHeight`
 * 
 * @return The current [WindowSizeClass] for the window
 */
@Composable
expect fun calculateWindowSizeClass(): WindowSizeClass
```

### Android Implementation

```kotlin
// androidMain/kotlin/.../WindowSizeClass.android.kt
package com.jermey.quo.vadis.core.navigation.compose

import android.app.Activity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass as calculateMaterial3WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Android implementation using Material3 WindowSizeClass API.
 * 
 * Leverages the official Material3 implementation which correctly
 * handles configuration changes, multi-window mode, and foldables.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    val activity = LocalContext.current as Activity
    val m3SizeClass = calculateMaterial3WindowSizeClass(activity)
    
    return WindowSizeClass(
        widthSizeClass = when (m3SizeClass.widthSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact -> 
                WindowWidthSizeClass.Compact
            androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Medium -> 
                WindowWidthSizeClass.Medium
            else -> WindowWidthSizeClass.Expanded
        },
        heightSizeClass = when (m3SizeClass.heightSizeClass) {
            androidx.compose.material3.windowsizeclass.WindowHeightSizeClass.Compact -> 
                WindowHeightSizeClass.Compact
            androidx.compose.material3.windowsizeclass.WindowHeightSizeClass.Medium -> 
                WindowHeightSizeClass.Medium
            else -> WindowHeightSizeClass.Expanded
        }
    )
}
```

### iOS Implementation

```kotlin
// iosMain/kotlin/.../WindowSizeClass.ios.kt
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import platform.UIKit.UIApplication
import platform.UIKit.UIScreen
import platform.UIKit.UIWindowScene
import platform.darwin.NSObject
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotification
import platform.UIKit.UIDeviceOrientationDidChangeNotification

/**
 * iOS implementation calculating from UIScreen bounds.
 * 
 * Observes device orientation changes to update size class.
 * Uses UIScreen.main.bounds scaled by density for dp conversion.
 */
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    var windowSizeClass by remember { mutableStateOf(calculateCurrentSizeClass()) }
    
    DisposableEffect(Unit) {
        val observer = object : NSObject() {
            @Suppress("UNUSED_PARAMETER")
            @ObjCAction
            fun orientationDidChange(notification: NSNotification?) {
                windowSizeClass = calculateCurrentSizeClass()
            }
        }
        
        NSNotificationCenter.defaultCenter.addObserver(
            observer = observer,
            selector = NSSelectorFromString("orientationDidChange:"),
            name = UIDeviceOrientationDidChangeNotification,
            `object` = null
        )
        
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }
    
    return windowSizeClass
}

private fun calculateCurrentSizeClass(): WindowSizeClass {
    val screen = UIScreen.mainScreen
    val bounds = screen.bounds
    val scale = screen.scale
    
    // Convert points to dp (iOS points are already density-independent)
    val widthDp = bounds.size.width.dp
    val heightDp = bounds.size.height.dp
    
    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}
```

### Desktop Implementation

```kotlin
// desktopMain/kotlin/.../WindowSizeClass.desktop.kt
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * Desktop implementation calculating from window size.
 * 
 * Uses LocalWindowInfo to get current window dimensions and
 * LocalDensity to convert pixels to dp.
 */
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp() }
    
    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}
```

### Web (JS) Implementation

```kotlin
// jsMain/kotlin/.../WindowSizeClass.js.kt
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * JavaScript/Browser implementation calculating from window dimensions.
 * 
 * Observes browser resize events to update size class.
 * Uses window.innerWidth/innerHeight for viewport dimensions.
 */
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    var windowSizeClass by remember { mutableStateOf(calculateFromBrowser()) }
    
    DisposableEffect(Unit) {
        val resizeListener: (Event) -> Unit = {
            windowSizeClass = calculateFromBrowser()
        }
        
        window.addEventListener("resize", resizeListener)
        
        onDispose {
            window.removeEventListener("resize", resizeListener)
        }
    }
    
    return windowSizeClass
}

private fun calculateFromBrowser(): WindowSizeClass {
    // Browser dimensions are in CSS pixels (already density-independent)
    val widthDp = window.innerWidth.dp
    val heightDp = window.innerHeight.dp
    
    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}
```

### Web (Wasm) Implementation

```kotlin
// wasmJsMain/kotlin/.../WindowSizeClass.wasmJs.kt
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import org.w3c.dom.events.Event

/**
 * WebAssembly/Browser implementation calculating from window dimensions.
 * 
 * Same logic as JS implementation but for Wasm target.
 */
@Composable
actual fun calculateWindowSizeClass(): WindowSizeClass {
    var windowSizeClass by remember { mutableStateOf(calculateFromBrowser()) }
    
    DisposableEffect(Unit) {
        val resizeListener: (Event) -> Unit = {
            windowSizeClass = calculateFromBrowser()
        }
        
        window.addEventListener("resize", resizeListener)
        
        onDispose {
            window.removeEventListener("resize", resizeListener)
        }
    }
    
    return windowSizeClass
}

private fun calculateFromBrowser(): WindowSizeClass {
    val widthDp = window.innerWidth.dp
    val heightDp = window.innerHeight.dp
    
    return WindowSizeClass.calculateFromSize(widthDp, heightDp)
}
```

### QuoVadisHost Integration

```kotlin
/**
 * QuoVadisHost with window size integration.
 * 
 * Observes window size changes and passes WindowSizeClass to
 * the flattener for adaptive rendering decisions.
 */
@Composable
fun QuoVadisHost(
    navState: NavState,
    modifier: Modifier = Modifier,
    transitionState: TransitionState = rememberTransitionState(),
    flattener: TreeFlattener = remember { TreeFlattener() }
) {
    // Observe window size changes
    val windowSizeClass = calculateWindowSizeClass()
    
    // Flatten with window size awareness
    val flattenResult = remember(navState, transitionState, windowSizeClass) {
        flattener.flattenState(navState, transitionState, windowSizeClass)
    }
    
    // Render surfaces
    Box(modifier = modifier) {
        flattenResult.surfaces
            .sortedByZOrder()
            .renderable()
            .forEach { surface ->
                key(surface.id) {
                    SurfaceRenderer(
                        surface = surface,
                        transitionState = transitionState
                    )
                }
            }
    }
}
```

---

## PaneNode Behavior Table

| Width Class | PaneNode Rendering | Description |
|-------------|-------------------|-------------|
| **Compact** | As StackNode (single pane) | Only active pane visible, back navigation between panes |
| **Medium** | Multi-pane (2 visible) | Two panes side-by-side, typically list-detail pattern |
| **Expanded** | Multi-pane (all visible) | All panes visible with user-controlled layout |

### Transition Behavior

When window size class changes:

1. **Compact → Medium/Expanded**: Animate from single-pane to multi-pane layout
2. **Medium/Expanded → Compact**: Preserve active pane, animate others out
3. **Medium ↔ Expanded**: Smooth resize animation, no structural change

---

## Window Size Change Handling

### State Preservation

Navigation state must be preserved when window size changes:

```kotlin
// Before resize (Compact - showing detail pane)
PaneNode(
    activePaneIndex = 1,  // Detail pane
    panes = [listPane, detailPane]
)

// After resize (Expanded - both visible)
// Same PaneNode, but rendered differently
// activePaneIndex still 1, but both panes now visible
```

### Animated Transitions

Size class changes trigger smooth transitions:

```kotlin
/**
 * Handles animated transitions when window size class changes.
 */
@Composable
private fun AnimatedPaneTransition(
    previousSizeClass: WindowWidthSizeClass,
    currentSizeClass: WindowWidthSizeClass,
    paneNode: PaneNode,
    content: @Composable () -> Unit
) {
    val transitionSpec = when {
        previousSizeClass == WindowWidthSizeClass.Compact && 
        currentSizeClass != WindowWidthSizeClass.Compact -> {
            // Expanding: fade in additional panes
            fadeIn() + expandHorizontally()
        }
        previousSizeClass != WindowWidthSizeClass.Compact && 
        currentSizeClass == WindowWidthSizeClass.Compact -> {
            // Collapsing: fade out additional panes
            fadeOut() + shrinkHorizontally()
        }
        else -> {
            // Same mode: no animation needed
            null
        }
    }
    
    if (transitionSpec != null) {
        AnimatedContent(
            targetState = currentSizeClass,
            transitionSpec = { transitionSpec }
        ) {
            content()
        }
    } else {
        content()
    }
}
```

### Reactive Flattening

The flattener automatically re-runs when window size changes:

```kotlin
// In QuoVadisHost
val flattenResult = remember(navState, transitionState, windowSizeClass) {
    // This re-computes when windowSizeClass changes
    flattener.flattenState(navState, transitionState, windowSizeClass)
}
```

---

## Implementation Steps

### Step 1: Create Core Types (commonMain)

1. Create `WindowSizeClass.kt` in compose package
2. Define `WindowWidthSizeClass` enum with breakpoints
3. Define `WindowHeightSizeClass` enum with breakpoints
4. Define `WindowSizeClass` data class with companion helpers
5. Declare `expect fun calculateWindowSizeClass()`

### Step 2: Android Implementation (androidMain)

1. Add Material3 window-size-class dependency
2. Implement `calculateWindowSizeClass()` using Material3 API
3. Map Material3 enums to our enums

### Step 3: iOS Implementation (iosMain)

1. Implement `calculateWindowSizeClass()` using UIScreen
2. Add orientation change observer
3. Handle proper cleanup in DisposableEffect

### Step 4: Desktop Implementation (desktopMain)

1. Implement `calculateWindowSizeClass()` using LocalWindowInfo
2. Convert pixels to dp using LocalDensity

### Step 5: Web Implementations (jsMain, wasmJsMain)

1. Implement `calculateWindowSizeClass()` using browser window
2. Add resize event listener
3. Handle proper cleanup in DisposableEffect

### Step 6: QuoVadisHost Integration

1. Call `calculateWindowSizeClass()` in QuoVadisHost
2. Pass WindowSizeClass to flattener
3. Add to remember key for reactive updates

### Step 7: Add Unit Tests

1. Test size class calculations from various dimensions
2. Test breakpoint boundaries
3. Test companion factory methods

### Step 8: Add UI Tests

1. Test PaneNode renders as stack on compact
2. Test PaneNode renders multi-pane on expanded
3. Test state preservation during resize

---

## Files Affected

| File | Action | Description |
|------|--------|-------------|
| `quo-vadis-core/.../compose/WindowSizeClass.kt` | Create | Common interface and data classes |
| `quo-vadis-core/.../compose/WindowSizeClass.android.kt` | Create | Android implementation |
| `quo-vadis-core/.../compose/WindowSizeClass.ios.kt` | Create | iOS implementation |
| `quo-vadis-core/.../compose/WindowSizeClass.desktop.kt` | Create | Desktop implementation |
| `quo-vadis-core/.../compose/WindowSizeClass.js.kt` | Create | JS/Browser implementation |
| `quo-vadis-core/.../compose/WindowSizeClass.wasmJs.kt` | Create | Wasm/Browser implementation |
| `quo-vadis-core/.../compose/QuoVadisHost.kt` | Modify | Add windowSizeClass observation |
| `quo-vadis-core/.../compose/TreeFlattener.kt` | Modify | Accept windowSizeClass parameter |
| `build.gradle.kts` | Modify | Add Material3 window-size-class dependency |

---

## Dependencies

| Dependency | Type | Status |
|------------|------|--------|
| RENDER-002C (PaneNode flattening) | Hard | Must complete first |
| RENDER-001 (RenderableSurface) | Reference | Uses PANE_AS_STACK mode |
| Material3 window-size-class (Android) | External | Add to dependencies |

---

## Acceptance Criteria

- [ ] `WindowSizeClass` data class defined with width and height classifications
- [ ] `WindowWidthSizeClass` enum with Compact (< 600dp), Medium (600-840dp), Expanded (> 840dp)
- [ ] `WindowHeightSizeClass` enum with Compact (< 480dp), Medium (480-900dp), Expanded (> 900dp)
- [ ] `calculateFromSize()` companion function correctly classifies dimensions
- [ ] Platform-specific `calculateWindowSizeClass()` implementations:
  - [ ] Android implementation using Material3 `calculateWindowSizeClass()`
  - [ ] iOS implementation using UIScreen bounds with orientation observer
  - [ ] Desktop implementation using LocalWindowInfo
  - [ ] JS/Browser implementation using window dimensions with resize listener
  - [ ] Wasm/Browser implementation using window dimensions with resize listener
- [ ] QuoVadisHost observes window size changes via `calculateWindowSizeClass()`
- [ ] WindowSizeClass passed to TreeFlattener for adaptive rendering
- [ ] PaneNode switches rendering mode based on width size class:
  - [ ] Compact → PANE_AS_STACK (single pane)
  - [ ] Medium → Multi-pane (2 visible)
  - [ ] Expanded → Multi-pane (all visible)
- [ ] Navigation state preserved during window resize
- [ ] Smooth animated transitions when size class changes
- [ ] Unit tests for size class calculations at boundaries
- [ ] Unit tests for each WindowWidthSizeClass value
- [ ] Unit tests for each WindowHeightSizeClass value
- [ ] UI tests for PaneNode adaptive behavior
- [ ] Code compiles on all target platforms

---

## Testing Notes

### Unit Tests

```kotlin
class WindowSizeClassTest {

    @Test
    fun `width below 600dp is Compact`() {
        assertEquals(
            WindowWidthSizeClass.Compact,
            WindowWidthSizeClass.fromWidth(599.dp)
        )
    }

    @Test
    fun `width at 600dp is Medium`() {
        assertEquals(
            WindowWidthSizeClass.Medium,
            WindowWidthSizeClass.fromWidth(600.dp)
        )
    }

    @Test
    fun `width at 840dp is Medium`() {
        assertEquals(
            WindowWidthSizeClass.Medium,
            WindowWidthSizeClass.fromWidth(840.dp)
        )
    }

    @Test
    fun `width above 840dp is Expanded`() {
        assertEquals(
            WindowWidthSizeClass.Expanded,
            WindowWidthSizeClass.fromWidth(841.dp)
        )
    }

    @Test
    fun `height below 480dp is Compact`() {
        assertEquals(
            WindowHeightSizeClass.Compact,
            WindowHeightSizeClass.fromHeight(479.dp)
        )
    }

    @Test
    fun `height at 480dp is Medium`() {
        assertEquals(
            WindowHeightSizeClass.Medium,
            WindowHeightSizeClass.fromHeight(480.dp)
        )
    }

    @Test
    fun `height above 900dp is Expanded`() {
        assertEquals(
            WindowHeightSizeClass.Expanded,
            WindowHeightSizeClass.fromHeight(901.dp)
        )
    }

    @Test
    fun `calculateFromSize creates correct class for phone dimensions`() {
        val sizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
        
        assertEquals(WindowWidthSizeClass.Compact, sizeClass.widthSizeClass)
        assertEquals(WindowHeightSizeClass.Medium, sizeClass.heightSizeClass)
    }

    @Test
    fun `calculateFromSize creates correct class for tablet dimensions`() {
        val sizeClass = WindowSizeClass.calculateFromSize(DpSize(800.dp, 1200.dp))
        
        assertEquals(WindowWidthSizeClass.Medium, sizeClass.widthSizeClass)
        assertEquals(WindowHeightSizeClass.Expanded, sizeClass.heightSizeClass)
    }

    @Test
    fun `calculateFromSize creates correct class for desktop dimensions`() {
        val sizeClass = WindowSizeClass.calculateFromSize(DpSize(1920.dp, 1080.dp))
        
        assertEquals(WindowWidthSizeClass.Expanded, sizeClass.widthSizeClass)
        assertEquals(WindowHeightSizeClass.Expanded, sizeClass.heightSizeClass)
    }

    @Test
    fun `isCompactWidth returns true only for Compact`() {
        assertTrue(WindowSizeClass.Compact.isCompactWidth)
        assertFalse(WindowSizeClass.Medium.isCompactWidth)
        assertFalse(WindowSizeClass.Expanded.isCompactWidth)
    }

    @Test
    fun `isAtLeastMediumWidth returns true for Medium and Expanded`() {
        assertFalse(WindowSizeClass.Compact.isAtLeastMediumWidth)
        assertTrue(WindowSizeClass.Medium.isAtLeastMediumWidth)
        assertTrue(WindowSizeClass.Expanded.isAtLeastMediumWidth)
    }
}
```

### UI Tests

```kotlin
class WindowSizeClassUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `PaneNode renders as stack on compact width`() {
        composeTestRule.setContent {
            // Simulate compact width
            CompositionLocalProvider(
                LocalWindowSizeClass provides WindowSizeClass.Compact
            ) {
                TestPaneNavigation()
            }
        }
        
        // Only one pane should be visible
        composeTestRule.onNodeWithTag("pane-0").assertDoesNotExist()
        composeTestRule.onNodeWithTag("pane-1").assertIsDisplayed()
    }

    @Test
    fun `PaneNode renders multi-pane on expanded width`() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalWindowSizeClass provides WindowSizeClass.Expanded
            ) {
                TestPaneNavigation()
            }
        }
        
        // Both panes should be visible
        composeTestRule.onNodeWithTag("pane-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pane-1").assertIsDisplayed()
    }

    @Test
    fun `navigation state preserved during resize`() {
        var windowSizeClass by mutableStateOf(WindowSizeClass.Compact)
        
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSizeClass
            ) {
                TestPaneNavigation(initialActivePane = 1)
            }
        }
        
        // Start compact - detail pane visible
        composeTestRule.onNodeWithTag("pane-1").assertIsDisplayed()
        
        // Expand - both visible, detail still active
        windowSizeClass = WindowSizeClass.Expanded
        composeTestRule.waitForIdle()
        
        composeTestRule.onNodeWithTag("pane-0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("pane-1").assertIsDisplayed()
        // Detail pane should still be the "active" one (e.g., highlighted)
        composeTestRule.onNodeWithTag("pane-1-active").assertExists()
    }
}
```

---

## References

- [INDEX](../INDEX.md) - Phase 2 Overview
- [RENDER-002C](./RENDER-002C-pane-flattening.md) - PaneNode adaptive flattening (consumer)
- [RENDER-004](./RENDER-004-quovadis-host.md) - QuoVadisHost integration point
- [Material Design 3 Window Size Classes](https://m3.material.io/foundations/layout/applying-layout/window-size-classes)
- [Material3 WindowSizeClass API](https://developer.android.com/reference/kotlin/androidx/compose/material3/windowsizeclass/package-summary)

````
