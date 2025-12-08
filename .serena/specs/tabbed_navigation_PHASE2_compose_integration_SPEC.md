# Phase 2: Compose UI Integration

## Phase Overview

**Objective**: Create Compose Multiplatform UI components that integrate `TabNavigatorState` with the visual tabbed navigation experience.

**Scope**: 
- Composable functions for tab navigation
- State management with `remember` and `rememberSaveable`
- Animation support for tab switching
- Integration with existing `GraphNavHost`
- Predictive back gesture support for tabs
- Shared element transitions across tab content

**Timeline**: 3-4 days

**Dependencies**: 
- Phase 1 (Core Foundation) - `TabNavigatorState`, `TabDefinition`, `BackPressHandler`

## Architectural Principles

### 1. Compose Best Practices
- State hoisting (state in ViewModel/remember, not in Composables)
- Side effects in `LaunchedEffect`
- Stable state with `rememberSaveable`
- No business logic in Composables

### 2. Animation Consistency
- Follow Material Design 3 guidelines
- Consistent with existing `NavigationTransitions`
- GPU-accelerated with `graphicsLayer`
- Interruptible animations

### 3. Integration
- Works seamlessly with existing `GraphNavHost`
- Compatible with predictive back gestures
- Supports shared element transitions
- Follows quo-vadis composition patterns

## Detailed Implementation Plan

### Step 1: Remember Functions for Tab Navigation

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/RememberTabNavigation.kt`

**Purpose**: Lifecycle-aware state management for tab navigation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import com.jermey.quo.vadis.core.navigation.core.*

/**
 * Remember a [TabNavigatorState] that survives configuration changes.
 * 
 * @param config Tab navigator configuration
 * @return Stable [TabNavigatorState] instance
 * 
 * @sample
 * ```kotlin
 * val tabState = rememberTabNavigatorState(
 *     config = TabNavigatorConfig(
 *         allTabs = listOf(HomeTab, ProfileTab, SettingsTab),
 *         initialTab = HomeTab
 *     )
 * )
 * ```
 */
@Composable
fun rememberTabNavigatorState(
    config: TabNavigatorConfig
): TabNavigatorState {
    return rememberSaveable(
        saver = TabNavigatorStateSaver(config)
    ) {
        TabNavigatorState(config)
    }
}

/**
 * Saver for [TabNavigatorState] that preserves:
 * - Currently selected tab
 * - All tab stacks
 * 
 * Survives configuration changes and process death.
 */
private fun TabNavigatorStateSaver(
    config: TabNavigatorConfig
): Saver<TabNavigatorState, List<Any>> {
    return Saver(
        save = { state ->
            // Save format: [selectedTabId, Map<tabId, List<destinationRoute>>]
            listOf(
                state.selectedTab.value.id,
                state.tabStacks.value.mapKeys { it.key.id }
                    .mapValues { (_, stack) -> stack.map { dest -> dest.route } }
            )
        },
        restore = { saved ->
            val selectedTabId = saved[0] as String
            @Suppress("UNCHECKED_CAST")
            val stacksData = saved[1] as Map<String, List<String>>
            
            val state = TabNavigatorState(config)
            
            // Restore selected tab
            config.allTabs.find { it.id == selectedTabId }?.let { tab ->
                state.selectTab(tab)
            }
            
            // Restore stacks (simplified - actual implementation would deserialize destinations)
            // Note: Full implementation requires destination serialization from Phase 1
            
            state
        }
    )
}

/**
 * Remember a tab navigator that integrates with a parent [Navigator].
 * 
 * Automatically registers this tab navigator as a child of the parent
 * for back press delegation.
 * 
 * @param config Tab navigator configuration
 * @param parentNavigator The parent navigator to integrate with
 * @return Stable [TabNavigatorState] instance
 */
@Composable
fun rememberTabNavigator(
    config: TabNavigatorConfig,
    parentNavigator: Navigator
): TabNavigatorState {
    val tabState = rememberTabNavigatorState(config)
    
    // Register as child for back press delegation
    LaunchedEffect(tabState, parentNavigator) {
        parentNavigator.setActiveChild(tabState)
    }
    
    // Cleanup on disposal
    DisposableEffect(tabState, parentNavigator) {
        onDispose {
            parentNavigator.setActiveChild(null)
        }
    }
    
    return tabState
}
```

**Key Design Decisions**:
- `rememberSaveable` for process death survival
- Automatic parent registration for back press
- Clean disposal of child relationship

---

### Step 2: Tab Content Container with Visibility Management

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabNavigationContainer.kt`

**Purpose**: Manage visibility of multiple tab content screens with animations

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.TabDefinition

/**
 * Container that manages multiple tab content screens with visibility control.
 * 
 * Each tab's content is kept in composition (preserving state) but only the
 * selected tab is visible. Provides smooth fade animations when switching tabs.
 * 
 * @param selectedTab Currently selected tab
 * @param tabs Map of tab definitions to their content composables
 * @param modifier Modifier for the container
 * @param transitionSpec Custom transition specification for tab switches
 * 
 * @sample
 * ```kotlin
 * TabNavigationContainer(
 *     selectedTab = tabState.selectedTab.collectAsState().value,
 *     tabs = mapOf(
 *         HomeTab to { HomeTabContent(navigator) },
 *         ProfileTab to { ProfileTabContent(navigator) }
 *     )
 * )
 * ```
 */
@Composable
fun TabNavigationContainer(
    selectedTab: TabDefinition,
    tabs: Map<TabDefinition, @Composable () -> Unit>,
    modifier: Modifier = Modifier,
    transitionSpec: TabTransitionSpec = TabTransitionSpec.Default
) {
    Box(modifier = modifier) {
        tabs.forEach { (tab, content) ->
            // Keep all tabs in composition for state preservation
            TabContent(
                visible = tab == selectedTab,
                transitionSpec = transitionSpec,
                content = content
            )
        }
    }
}

/**
 * Individual tab content with visibility animation.
 */
@Composable
private fun TabContent(
    visible: Boolean,
    transitionSpec: TabTransitionSpec,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = transitionSpec.enter,
        exit = transitionSpec.exit,
        modifier = Modifier.fillMaxSize()
    ) {
        content()
    }
}

/**
 * Specification for tab transition animations.
 * 
 * @property enter Enter animation when tab becomes visible
 * @property exit Exit animation when tab becomes hidden
 */
data class TabTransitionSpec(
    val enter: EnterTransition,
    val exit: ExitTransition
) {
    companion object {
        /**
         * Default fade transition (Material Design 3 recommended).
         */
        val Default = TabTransitionSpec(
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 200,
                    easing = FastOutSlowInEasing
                )
            )
        )
        
        /**
         * Crossfade transition (simultaneous fade in/out).
         */
        val Crossfade = TabTransitionSpec(
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        )
        
        /**
         * No transition (instant switch).
         */
        val None = TabTransitionSpec(
            enter = EnterTransition.None,
            exit = ExitTransition.None
        )
    }
}
```

**Key Design Decisions**:
- All tabs kept in composition (state preservation)
- `AnimatedVisibility` for smooth transitions
- Customizable transition specs
- Material Design 3 defaults

---

### Step 3: Tabbed Navigation Host

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabbedNavHost.kt`

**Purpose**: High-level composable for tabbed navigation UI

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.*

/**
 * High-level composable for tabbed navigation with integrated back press handling.
 * 
 * Manages:
 * - Tab state and switching
 * - Independent navigation stacks per tab
 * - Hierarchical back press delegation
 * - Tab content rendering with animations
 * 
 * @param tabState State holder for tab navigation
 * @param tabGraphs Map of tab definitions to their navigation graphs
 * @param parentNavigator Optional parent navigator for delegation
 * @param modifier Modifier for the host
 * @param contentPadding Padding to apply to tab content (e.g., for bottom bar)
 * @param transitionSpec Animation specification for tab switches
 * 
 * @sample Complete tabbed navigation setup
 * ```kotlin
 * val navigator = rememberNavigator()
 * val tabConfig = TabNavigatorConfig(
 *     allTabs = listOf(HomeTab, ProfileTab),
 *     initialTab = HomeTab
 * )
 * val tabState = rememberTabNavigator(tabConfig, navigator)
 * 
 * TabbedNavHost(
 *     tabState = tabState,
 *     tabGraphs = mapOf(
 *         HomeTab to homeGraph,
 *         ProfileTab to profileGraph
 *     ),
 *     parentNavigator = navigator
 * )
 * ```
 */
@Composable
fun TabbedNavHost(
    tabState: TabNavigatorState,
    tabGraphs: Map<TabDefinition, NavigationGraph>,
    parentNavigator: Navigator? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    contentPadding: PaddingValues = PaddingValues(),
    transitionSpec: TabTransitionSpec = TabTransitionSpec.Default
) {
    // Register with parent for back press delegation
    if (parentNavigator != null) {
        LaunchedEffect(tabState, parentNavigator) {
            parentNavigator.setActiveChild(tabState)
        }
        
        DisposableEffect(tabState, parentNavigator) {
            onDispose {
                parentNavigator.setActiveChild(null)
            }
        }
    }
    
    val selectedTab by tabState.selectedTab.collectAsState()
    
    // Create navigators for each tab
    val tabNavigators = remember(tabGraphs) {
        tabGraphs.mapValues { (tab, _) ->
            // Each tab gets its own Navigator instance
            rememberNavigatorForTab(tab, tabState)
        }
    }
    
    // Register graphs with tab navigators
    LaunchedEffect(tabGraphs, tabNavigators) {
        tabGraphs.forEach { (tab, graph) ->
            tabNavigators[tab]?.registerGraph(graph)
            tabNavigators[tab]?.setStartDestination(tab.rootDestination)
        }
    }
    
    // Render tab content
    TabNavigationContainer(
        selectedTab = selectedTab,
        tabs = tabNavigators.mapValues { (tab, navigator) ->
            {
                val graph = tabGraphs[tab]
                if (graph != null && navigator != null) {
                    // Use GraphNavHost for each tab
                    GraphNavHost(
                        graph = graph,
                        navigator = navigator,
                        modifier = Modifier.fillMaxSize(),
                        enablePredictiveBack = true
                    )
                }
            }
        },
        modifier = modifier,
        transitionSpec = transitionSpec
    )
}

/**
 * Create a navigator for a specific tab that syncs with TabNavigatorState.
 */
@Composable
private fun rememberNavigatorForTab(
    tab: TabDefinition,
    tabState: TabNavigatorState
): Navigator {
    val navigator = rememberNavigator()
    
    // Sync navigator's backstack with tab state
    LaunchedEffect(tab, tabState, navigator) {
        snapshotFlow { tabState.getTabStack(tab) }.collect { stack ->
            // Update navigator to match tab state
            // (Implementation detail: sync backstack)
        }
    }
    
    // Intercept navigator operations to update tab state
    // (Implementation detail: wrap navigator with delegation)
    
    return navigator
}
```

**Key Design Decisions**:
- Each tab gets its own `Navigator` instance
- Tab navigators sync with `TabNavigatorState`
- Automatic parent registration
- Reuses existing `GraphNavHost` for tab content

**Implementation Note**: The navigator syncing is complex and may require a wrapper class (`TabScopedNavigator`) that delegates to the main navigator while updating tab state.

---

### Step 4: Tab-Scoped Navigator Wrapper

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/TabScopedNavigator.kt`

**Purpose**: Navigator wrapper that syncs operations with tab state

```kotlin
package com.jermey.quo.vadis.core.navigation.core

import kotlinx.coroutines.flow.StateFlow

/**
 * Navigator wrapper that synchronizes with a specific tab's state.
 * 
 * All navigation operations are delegated to the tab state, ensuring
 * that the tab's stack is updated correctly.
 * 
 * @property tab The tab this navigator is scoped to
 * @property tabState The parent tab navigator state
 * @property delegate The underlying navigator for actual navigation
 */
internal class TabScopedNavigator(
    private val tab: TabDefinition,
    private val tabState: TabNavigatorState,
    private val delegate: Navigator
) : Navigator {
    
    override val backStack: BackStack
        get() = TabBackStackView(tab, tabState, delegate.backStack)
    
    override val currentDestination: StateFlow<Destination?>
        get() = delegate.currentDestination
    
    override val previousDestination: StateFlow<Destination?>
        get() = delegate.previousDestination
    
    override val currentTransition: StateFlow<NavigationTransition?>
        get() = delegate.currentTransition
    
    override val activeChild: BackPressHandler?
        get() = null // Tab navigators don't have children
    
    override fun navigate(destination: Destination, transition: NavigationTransition?) {
        // Navigate within tab
        tabState.navigateInTab(destination)
        delegate.navigate(destination, transition)
    }
    
    override fun navigateBack(): Boolean {
        val result = tabState.navigateBackInTab()
        if (result) {
            delegate.navigateBack()
        }
        return result
    }
    
    override fun navigateUp(): Boolean {
        return navigateBack()
    }
    
    override fun navigateAndClearTo(
        destination: Destination,
        upTo: String,
        inclusive: Boolean,
        transition: NavigationTransition?
    ) {
        // Clear tab stack and navigate
        // (Implementation: modify tab stack, then delegate)
        delegate.navigateAndClearTo(destination, upTo, inclusive, transition)
    }
    
    override fun navigateAndReplace(destination: Destination, transition: NavigationTransition?) {
        // Replace in tab stack
        // (Implementation: modify tab stack, then delegate)
        delegate.navigateAndReplace(destination, transition)
    }
    
    override fun navigateAndClearAll(destination: Destination, transition: NavigationTransition?) {
        tabState.resetTabTo(tab, destination)
        delegate.navigateAndClearAll(destination, transition)
    }
    
    override fun handleDeepLink(deepLink: DeepLink): Boolean {
        return delegate.handleDeepLink(deepLink)
    }
    
    override fun registerGraph(graph: NavigationGraph) {
        delegate.registerGraph(graph)
    }
    
    override fun setStartDestination(destination: Destination) {
        delegate.setStartDestination(destination)
    }
    
    override fun getDeepLinkHandler(): ((DeepLink) -> Boolean)? {
        return delegate.getDeepLinkHandler()
    }
    
    override fun setActiveChild(child: BackPressHandler?) {
        // Tab navigators don't support children
    }
    
    override fun onBack(): Boolean {
        return navigateBack()
    }
}

/**
 * BackStack view that reflects a tab's stack state.
 */
private class TabBackStackView(
    private val tab: TabDefinition,
    private val tabState: TabNavigatorState,
    private val delegate: BackStack
) : BackStack by delegate {
    // Override stack operations to sync with tab state
    // (Implementation detail)
}
```

**Key Design Decisions**:
- Transparent delegation pattern
- All operations sync with `TabNavigatorState`
- Encapsulates tab-specific logic
- Internal visibility (implementation detail)

---

### Step 5: Predictive Back Integration for Tabs

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabPredictiveBack.kt`

**Purpose**: Extend predictive back gestures to work with tab navigation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState
import kotlinx.coroutines.launch

/**
 * Modifier that applies predictive back animation to tab content.
 * 
 * When user performs a back gesture within a tab, the content animates
 * according to the gesture progress before completing navigation.
 * 
 * @param tabState The tab navigator state
 * @param enabled Whether predictive back is enabled
 */
@Composable
fun Modifier.tabPredictiveBack(
    tabState: TabNavigatorState,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }
    
    // Listen for back gesture events
    // (Implementation: platform-specific gesture detection)
    
    return this.graphicsLayer(
        scaleX = scale.value,
        scaleY = scale.value,
        alpha = alpha.value
    )
}

/**
 * State holder for tab predictive back animations.
 * 
 * Manages the two-phase animation:
 * 1. Gesture phase: Follow user's finger
 * 2. Exit phase: Complete animation after release
 */
@Stable
class TabPredictiveBackState internal constructor(
    private val tabState: TabNavigatorState
) {
    private val _gestureProgress = Animatable(0f)
    val gestureProgress: State<Float> = derivedStateOf { _gestureProgress.value }
    
    private var _isGestureActive by mutableStateOf(false)
    val isGestureActive: Boolean get() = _isGestureActive
    
    suspend fun startGesture() {
        _isGestureActive = true
        _gestureProgress.snapTo(0f)
    }
    
    suspend fun updateGesture(progress: Float) {
        _gestureProgress.snapTo(progress.coerceIn(0f, 1f))
    }
    
    suspend fun completeGesture() {
        // Animate to completion
        _gestureProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring()
        )
        
        // Trigger back navigation
        tabState.onBack()
        
        // Reset
        _gestureProgress.snapTo(0f)
        _isGestureActive = false
    }
    
    suspend fun cancelGesture() {
        // Animate back to 0
        _gestureProgress.animateTo(
            targetValue = 0f,
            animationSpec = spring()
        )
        _isGestureActive = false
    }
}

/**
 * Remember a predictive back state for tab navigation.
 */
@Composable
fun rememberTabPredictiveBackState(
    tabState: TabNavigatorState
): TabPredictiveBackState {
    return remember(tabState) {
        TabPredictiveBackState(tabState)
    }
}
```

**Key Design Decisions**:
- Reuses existing predictive back architecture
- Platform-specific gesture detection (Android/iOS)
- Two-phase animation (gesture + exit)
- Works alongside existing screen transitions

---

### Step 6: Platform-Specific Back Gesture Handling

**File**: `quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabPredictiveBackAndroid.kt`

**Purpose**: Android-specific predictive back for tabs (API 33+)

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * Android-specific back handler for tab navigation with predictive animations.
 * 
 * For API 33+, uses system predictive back gestures.
 * For older APIs, uses standard back button.
 */
@Composable
actual fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean,
    onBack: () -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Use predictive back (API 33+)
        val predictiveBackState = rememberTabPredictiveBackState(tabState)
        
        // System gesture callbacks
        // (Implementation: OnBackPressedCallback with progress)
        
    } else {
        // Standard back handler
        BackHandler(enabled = enabled && tabState.canHandleBack()) {
            if (tabState.onBack()) {
                onBack()
            }
        }
    }
}
```

**File**: `quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabPredictiveBackIOS.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.*
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * iOS-specific back handler for tab navigation with swipe gestures.
 */
@Composable
actual fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean,
    onBack: () -> Unit
) {
    // iOS swipe-back gesture handling
    // (Implementation: UIKit gesture recognizer bridge)
}
```

**File**: `quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/TabBackHandler.kt`

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import com.jermey.quo.vadis.core.navigation.core.TabNavigatorState

/**
 * Platform-agnostic back handler for tab navigation.
 * 
 * Expect/actual pattern for platform-specific implementations.
 */
@Composable
expect fun TabBackHandler(
    tabState: TabNavigatorState,
    enabled: Boolean = true,
    onBack: () -> Unit = {}
)
```

---

### Step 7: Shared Element Transitions for Tab Content

**Integration Note**: Tab content can use existing shared element support from `GraphNavHost`. No additional implementation needed - just ensure `destinationWithScopes()` works within tab graphs.

**File**: `quo-vadis-core/docs/TABBED_NAVIGATION.md` (documentation update)

n
## Shared Elements Across Tab Content

Shared element transitions work seamlessly within tab content:

```kotlin
val homeGraph = navigationGraph("home_tab") {
    startDestination(HomeDestination.List)
    
    // Use destinationWithScopes for shared elements
    destinationWithScopes(HomeDestination.List) { _, nav, shared, animated ->
        ListScreen(nav, shared, animated)
    }
    
    destinationWithScopes(HomeDestination.Detail) { _, nav, shared, animated ->
        DetailScreen(nav, shared, animated)
    }
}
```

Shared elements animate normally within each tab. Switching tabs uses fade transition (no shared elements across tabs).
```

---

## File Structure Summary

New files to create:

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/
├── RememberTabNavigation.kt          (NEW - ~150 lines)
├── TabNavigationContainer.kt         (NEW - ~180 lines)
├── TabbedNavHost.kt                  (NEW - ~250 lines)
├── TabPredictiveBack.kt              (NEW - ~200 lines)
└── TabBackHandler.kt                 (NEW - ~30 lines, expect)

quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/core/
└── TabScopedNavigator.kt             (NEW - ~200 lines)

quo-vadis-core/src/androidMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/
└── TabPredictiveBackAndroid.kt       (NEW - ~150 lines)

quo-vadis-core/src/iosMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/
└── TabPredictiveBackIOS.kt           (NEW - ~100 lines)

quo-vadis-core/src/commonTest/kotlin/com/jermey/quo/vadis/core/navigation/compose/
├── TabbedNavHostTest.kt              (NEW - ~300 lines)
├── TabNavigationContainerTest.kt     (NEW - ~200 lines)
└── TabPredictiveBackTest.kt          (NEW - ~250 lines)
```

**Total**: ~2,010 lines of new code

---

## Quality Checklist

Before completing Phase 2:

### Code Quality
- [ ] Follows Compose best practices (state hoisting, side effects)
- [ ] All public APIs documented with KDoc
- [ ] Animations use `graphicsLayer` for performance
- [ ] No business logic in Composables
- [ ] Proper `remember` and `rememberSaveable` usage

### Testing
- [ ] Unit tests for all composables
- [ ] Animation tests (verify transitions play)
- [ ] State persistence tests (configuration changes)
- [ ] Platform-specific gesture tests
- [ ] Test coverage ≥ 85%

### Integration
- [ ] Works with existing `GraphNavHost`
- [ ] Compatible with predictive back gestures
- [ ] Shared elements work within tabs
- [ ] No breaking changes to existing APIs

### Performance
- [ ] Tab switching <16ms (60fps)
- [ ] No dropped frames during animations
- [ ] Memory stable with 8 tabs in composition
- [ ] GPU profiler shows efficient rendering

### UX
- [ ] Smooth animations (Material Design 3)
- [ ] Predictive back gestures feel natural
- [ ] State preserved across tab switches
- [ ] Back press behavior intuitive

---

## Dependencies for Next Phases

**Phase 3 (KSP)** will generate code that uses:
- `TabbedNavHost` for rendering
- `rememberTabNavigator` for state management
- `TabNavigatorConfig` for configuration

**Phase 4 (Demo App)** will demonstrate:
- Complete tab navigation setup
- Custom animations
- Deep linking to tabs
- Predictive back gestures

---

## Risks & Mitigation

### Risk: Animation Performance
**Mitigation**:
- Use `graphicsLayer` exclusively
- Profile with GPU debugging tools
- Test on low-end devices
- Provide `TabTransitionSpec.None` option

### Risk: State Synchronization
**Mitigation**:
- Extensive testing of `TabScopedNavigator`
- Clear documentation of sync behavior
- Unit tests for edge cases (rapid switching)

### Risk: Platform Gesture Conflicts
**Mitigation**:
- Respect system gestures on each platform
- Configurable gesture detection sensitivity
- Fallback to standard back button

---

## Verification Steps

After implementation:

1. **Build**: `./gradlew :quo-vadis-core:build`
2. **Test**: `./gradlew :quo-vadis-core:test`
3. **Android Preview**: Create simple tab demo in `composeApp`
4. **Performance**: Profile with GPU debugger (target 60fps)
5. **Gestures**: Test predictive back on Android 13+ device
6. **Memory**: Verify no leaks with 8+ tabs

---

**Status**: 🔴 Not Started

**Next Phase**: Phase 3 - KSP Annotations & Code Generation

**Depends On**: Phase 1 (Core Foundation) ✅
