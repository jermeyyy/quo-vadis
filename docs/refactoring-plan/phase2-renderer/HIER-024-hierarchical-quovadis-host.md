# HIER-024: HierarchicalQuoVadisHost

## Task Metadata

| Property | Value |
|----------|-------|
| **Task ID** | HIER-024 |
| **Task Name** | Create HierarchicalQuoVadisHost |
| **Phase** | Phase 4: Integration |
| **Complexity** | Large |
| **Estimated Time** | 3-4 days |
| **Dependencies** | HIER-001 through HIER-023 |
| **Blocked By** | All Phase 1-3 tasks |
| **Blocks** | HIER-025, HIER-027, HIER-028 |

---

## Overview

`HierarchicalQuoVadisHost` is the main entry point for the hierarchical rendering system. It creates the rendering scope, handles predictive back at the root level, wraps everything in `SharedTransitionLayout`, and invokes `NavTreeRenderer`.

---

## File Location

```
quo-vadis-core/src/commonMain/kotlin/com/jermey/quo/vadis/core/navigation/compose/HierarchicalQuoVadisHost.kt
```

---

## Implementation

```kotlin
package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.Navigator
import com.jermey.quo.vadis.core.navigation.compose.animation.AnimationCoordinator
import com.jermey.quo.vadis.core.navigation.compose.animation.NavTransition
import com.jermey.quo.vadis.core.navigation.compose.cache.ComposableCache
import com.jermey.quo.vadis.core.navigation.compose.cache.rememberComposableCache
import com.jermey.quo.vadis.core.navigation.compose.gesture.PredictiveBackController
import com.jermey.quo.vadis.core.navigation.compose.hierarchical.*
import com.jermey.quo.vadis.core.navigation.compose.registry.*

/**
 * Main entry point for hierarchical navigation rendering.
 *
 * This host renders the navigation tree hierarchically, preserving
 * wrapper/content relationships for proper animations and caching.
 *
 * ## Features
 * - Single host for all navigation (tabs, panes, stacks)
 * - Shared element transitions across any screens
 * - Predictive back with proper subtree transforms
 * - KSP-generated screen and wrapper bindings
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberNavigator(startDestination = HomeScreen)
 *     
 *     HierarchicalQuoVadisHost(
 *         navigator = navigator,
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * @param navigator The navigator managing navigation state
 * @param modifier Modifier to apply to the host
 * @param screenRegistry Registry for destination-to-screen bindings
 * @param wrapperRegistry Registry for wrapper bindings
 * @param transitionRegistry Registry for destination transitions
 * @param defaultTransition Default transition when none specified
 * @param cacheSize Maximum composables to cache
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun HierarchicalQuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    screenRegistry: ScreenRegistry = GeneratedScreenRegistry,
    wrapperRegistry: WrapperRegistry = DefaultWrapperRegistry,
    transitionRegistry: TransitionRegistry = DefaultTransitionRegistry,
    defaultTransition: NavTransition = NavTransition.SlideHorizontal,
    cacheSize: Int = ComposableCache.DEFAULT_MAX_SIZE
) {
    // Collect navigation state
    val currentState by navigator.state.collectAsState()
    var previousState by remember { mutableStateOf(currentState) }
    
    // Update previous state after small delay for animation
    LaunchedEffect(currentState) {
        if (currentState != previousState) {
            // Keep previous for animation duration
            kotlinx.coroutines.delay(16)
            previousState = currentState
        }
    }
    
    // Create core components
    val cache = rememberComposableCache(maxSize = cacheSize)
    val animationCoordinator = remember(transitionRegistry, defaultTransition) {
        AnimationCoordinator(transitionRegistry, defaultTransition)
    }
    val predictiveBackController = remember { PredictiveBackController() }
    
    // Check if back navigation is possible
    val canGoBack = remember(currentState) {
        navigator.canNavigateBack()
    }
    
    // Handle predictive back gesture at root
    PredictiveBackHandler(enabled = canGoBack) { backEvent ->
        predictiveBackController.handleGesture(backEvent) {
            navigator.navigateBack()
        }
    }
    
    // Render with shared transition support
    SharedTransitionLayout(modifier = modifier) {
        // Create render scope
        val scope = remember(
            navigator, cache, animationCoordinator, 
            predictiveBackController, this, screenRegistry, wrapperRegistry
        ) {
            NavRenderScopeImpl(
                navigator = navigator,
                cache = cache,
                animationCoordinator = animationCoordinator,
                predictiveBackController = predictiveBackController,
                sharedTransitionScope = this,
                screenRegistry = screenRegistry,
                wrapperRegistry = wrapperRegistry
            )
        }
        
        // Provide scope via CompositionLocal
        CompositionLocalProvider(LocalNavRenderScope provides scope) {
            // Render navigation tree
            NavTreeRenderer(
                node = currentState,
                previousNode = previousState,
                scope = scope
            )
        }
    }
}

/**
 * Implementation of [NavRenderScope].
 */
@Stable
private class NavRenderScopeImpl(
    override val navigator: Navigator,
    override val cache: ComposableCache,
    override val animationCoordinator: AnimationCoordinator,
    override val predictiveBackController: PredictiveBackController,
    override val sharedTransitionScope: SharedTransitionScope,
    override val screenRegistry: ScreenRegistry,
    override val wrapperRegistry: WrapperRegistry
) : NavRenderScope {
    
    @Composable
    override fun withAnimatedVisibilityScope(
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

- **Navigator**: Provides navigation state
- **NavTreeRenderer**: Renders the navigation tree
- **All registries**: Screen, wrapper, transition bindings
- **PredictiveBackHandler**: Platform back gesture handling
- **SharedTransitionLayout**: Shared element support

---

## Acceptance Criteria

- [ ] `HierarchicalQuoVadisHost` composable with all parameters
- [ ] Collects and tracks current/previous navigator state
- [ ] Creates `ComposableCache`, `AnimationCoordinator`, `PredictiveBackController`
- [ ] Handles `PredictiveBackHandler` at root
- [ ] Wraps in `SharedTransitionLayout`
- [ ] Creates and provides `NavRenderScopeImpl`
- [ ] Provides `LocalNavRenderScope`
- [ ] Invokes `NavTreeRenderer` with state
- [ ] `NavRenderScopeImpl` implements `withAnimatedVisibilityScope`
- [ ] Full KDoc documentation
