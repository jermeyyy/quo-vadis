@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.core.NavigationGraph
import com.jermey.quo.vadis.core.navigation.core.Navigator

// =============================================================================
// QuoVadisHostScope Interface
// =============================================================================

/**
 * Scope provided to content lambdas within [QuoVadisHost].
 *
 * This scope provides access to:
 * - [SharedTransitionScope] for shared element transitions
 * - [Navigator] for programmatic navigation
 *
 * ## Usage with Shared Elements
 *
 * The scope extends [SharedTransitionScope], making shared element APIs
 * available directly in the content lambda:
 *
 * ```kotlin
 * QuoVadisHost(navigator = navigator) { destination ->
 *     // sharedElement, sharedBounds, etc. available here
 *     ProfileScreen(
 *         modifier = Modifier.sharedElement(
 *             state = rememberSharedContentState(key = "profile-${destination.id}"),
 *             animatedVisibilityScope = this@AnimatedVisibility
 *         )
 *     )
 * }
 * ```
 *
 * @see QuoVadisHost
 * @see SharedTransitionScope
 */
@Stable
public interface QuoVadisHostScope : SharedTransitionScope {

    /**
     * The navigator instance for programmatic navigation.
     *
     * Use this to navigate from within screen content or to access
     * navigation state.
     */
    public val navigator: Navigator
}

// =============================================================================
// Main QuoVadisHost Composable
// =============================================================================

/**
 * The unified navigation host that renders any NavNode tree structure.
 *
 * QuoVadisHost is the **single rendering component** that replaces all previous
 * navigation hosts (NavHost, GraphNavHost, TabbedNavHost). It:
 *
 * 1. Observes the Navigator's state flow
 * 2. Renders the NavNode tree hierarchically
 * 3. Coordinates enter/exit animations
 * 4. Provides SharedTransitionScope for shared element transitions
 * 5. Preserves tab state via SaveableStateHolder
 *
 * ## Basic Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberNavigator(initialGraph)
 *
 *     QuoVadisHost(
 *         navigator = navigator,
 *         modifier = Modifier.fillMaxSize()
 *     )
 * }
 * ```
 *
 * @param navigator The Navigator instance managing navigation state
 * @param modifier Modifier for the root container
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview the back navigation result while performing
 *   a back gesture (swipe on Android/iOS, system back on supported platforms).
 *   Defaults to `true`. Set to `false` to disable gesture-based back previews.
 *
 * @see QuoVadisHostScope
 * @see NavigationHost
 */
@Composable
public fun QuoVadisHost(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true
) {
    // Delegate to hierarchical rendering
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
    )
}

// =============================================================================
// Alternative APIs
// =============================================================================

/**
 * QuoVadisHost variant that uses a [NavigationGraph] for content resolution.
 *
 * This is the most type-safe approach, using KSP-generated graphs.
 *
 * ## Usage
 *
 * ```kotlin
 * QuoVadisHost(
 *     navigator = navigator,
 *     graph = AppNavGraph // KSP-generated
 * )
 * ```
 *
 * @param navigator The Navigator instance managing navigation state
 * @param graph The navigation graph containing destination-to-content mappings
 * @param modifier Modifier for the root container
 * @param enablePredictiveBack Whether to enable predictive back gesture handling.
 *   When enabled, users can preview the back navigation result while performing
 *   a back gesture. Defaults to `true`.
 */
@Composable
public fun QuoVadisHost(
    navigator: Navigator,
    graph: NavigationGraph,
    modifier: Modifier = Modifier,
    enablePredictiveBack: Boolean = true
) {
    // Delegate to hierarchical rendering
    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
    )
}
