@file:Suppress("TooManyFunctions")

package com.jermey.quo.vadis.core.navigation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.jermey.quo.vadis.core.navigation.NavigationConfig
import com.jermey.quo.vadis.core.navigation.compose.wrapper.WindowSizeClass
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.Navigator
import com.jermey.quo.vadis.core.navigation.core.TreeNavigator
import kotlin.reflect.KClass

// =============================================================================
// rememberQuoVadisNavigator
// =============================================================================

/**
 * Remember and create a Navigator with the given configuration.
 *
 * This is the recommended way to create a Navigator in Compose. It handles:
 * - Proper memoization: Navigator is recreated only when [rootDestination], [config], or [key] changes
 * - Coroutine scope: Automatically provides a scope that follows the Composable lifecycle
 * - Initial state: Builds the initial NavNode from the config's container definitions
 *
 * ## Basic Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(MainTabs::class, GeneratedNavigationConfig)
 *
 *     NavigationHost(navigator)  // Config is read from navigator
 * }
 * ```
 *
 * ## With Custom Key
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(
 *         rootDestination = MainTabs::class,
 *         config = GeneratedNavigationConfig + FeatureModuleConfig,
 *         key = "main-navigator"
 *     )
 *     // ...
 * }
 * ```
 *
 * ## Multi-Module Composition
 *
 * ```kotlin
 * val combinedConfig = AppConfig + FeatureAConfig + FeatureBConfig
 * val navigator = rememberQuoVadisNavigator(
 *     rootDestination = MainTabs::class,
 *     config = combinedConfig
 * )
 * ```
 *
 * @param rootDestination The destination class for the root container (e.g., your main tabs class).
 *   This must be a registered container in the config.
 * @param config The NavigationConfig providing registry access and initial state building.
 * @param key Optional custom key for the root navigator node. If null, uses the destination's
 *   default key (typically the class simple name).
 *
 * @return A [Navigator] instance that is stable across recompositions (unless inputs change).
 *
 * @throws IllegalStateException if [rootDestination] is not a registered container in the config.
 *
 * @see NavigationHost for displaying navigation content
 * @see QuoVadisNavigation for a one-liner setup combining navigator + host
 * @see NavigationConfig for configuration details
 */
@Composable
fun rememberQuoVadisNavigator(
    rootDestination: KClass<out NavDestination>,
    config: NavigationConfig,
    key: String? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()

    return remember(rootDestination, config, key, coroutineScope) {
        val initialState = config.buildNavNode(
            destinationClass = rootDestination,
            key = key,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination.simpleName}. " +
                "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                "or manually registered in the NavigationConfig."
        )

        TreeNavigator(
            config = config,
            initialState = initialState,
            coroutineScope = coroutineScope
        )
    }
}

/**
 * Overload of [rememberQuoVadisNavigator] that accepts a destination instance
 * instead of a class, for cases where the root destination has constructor parameters.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     val navigator = rememberQuoVadisNavigator(
 *         rootDestination = MainTabs(initialTab = 1),
 *         config = GeneratedNavigationConfig
 *     )
 *     // ...
 * }
 * ```
 *
 * @param rootDestination The destination instance for the root container.
 * @param config The NavigationConfig providing registry access.
 * @param key Optional custom key for the root navigator node.
 *
 * @return A [Navigator] instance.
 *
 * @see rememberQuoVadisNavigator for the main overload with full documentation
 */
@Composable
fun rememberQuoVadisNavigator(
    rootDestination: NavDestination,
    config: NavigationConfig,
    key: String? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()

    return remember(rootDestination, config, key, coroutineScope) {
        val initialState = config.buildNavNode(
            destinationClass = rootDestination::class,
            key = key,
            parentKey = null
        ) ?: error(
            "No container registered for ${rootDestination::class.simpleName}. " +
                "Make sure the destination is annotated with @Tabs, @Stack, or @Pane, " +
                "or manually registered in the NavigationConfig."
        )

        TreeNavigator(
            config = config,
            initialState = initialState,
            coroutineScope = coroutineScope
        )
    }
}

// =============================================================================
// QuoVadisNavigation One-Liner
// =============================================================================

/**
 * One-liner navigation setup combining navigator creation and NavigationHost.
 *
 * This is the simplest way to set up Quo Vadis navigation. It:
 * - Creates and remembers a Navigator instance
 * - Renders the NavigationHost with the navigator
 * - Applies sensible platform-specific defaults
 *
 * ## Basic Usage (Simplest)
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(MainTabs::class, GeneratedNavigationConfig)
 * }
 * ```
 *
 * ## With Configuration
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(
 *         rootDestination = MainTabs::class,
 *         config = GeneratedNavigationConfig,
 *         modifier = Modifier.fillMaxSize(),
 *         enablePredictiveBack = true
 *     )
 * }
 * ```
 *
 * ## Multi-Module Setup
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(
 *         rootDestination = MainTabs::class,
 *         config = GeneratedNavigationConfig + FeatureAConfig + FeatureBConfig
 *     )
 * }
 * ```
 *
 * ## When to Use This vs. Separate Navigator
 *
 * Use `QuoVadisNavigation` when:
 * - You want the simplest setup possible
 * - You don't need direct access to the Navigator outside NavigationHost
 * - Default behavior is sufficient
 *
 * Use [rememberQuoVadisNavigator] + [NavigationHost] when:
 * - You need to access the Navigator from other Composables
 * - You want to perform navigation from outside the navigation tree
 * - You need custom lifecycle handling
 *
 * @param rootDestination The destination class for the root container.
 *   Must be registered in the config (annotated with @Tabs, @Stack, or @Pane).
 * @param config The NavigationConfig providing all navigation registries.
 * @param modifier Modifier to apply to the NavigationHost container.
 * @param key Optional custom key for the root navigator node.
 * @param enablePredictiveBack Whether to enable predictive back gesture support.
 *   Defaults based on platform (true for Android, false for others).
 * @param predictiveBackMode The mode for predictive back behavior.
 *   Only applicable when [enablePredictiveBack] is true.
 * @param windowSizeClass Optional window size class for adaptive layouts.
 *   Pass this to enable responsive navigation patterns.
 *
 * @see rememberQuoVadisNavigator for creating a Navigator with manual control
 * @see NavigationHost for the underlying host implementation
 * @see NavigationConfig for configuration details
 */
@Composable
fun QuoVadisNavigation(
    rootDestination: KClass<out NavDestination>,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    key: String? = null,
    enablePredictiveBack: Boolean = platformDefaultPredictiveBack(),
    windowSizeClass: WindowSizeClass? = null
) {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = rootDestination,
        config = config,
        key = key
    )

    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        windowSizeClass = windowSizeClass
    )
}

/**
 * Overload accepting a destination instance for parameterized roots.
 *
 * ## Usage
 *
 * ```kotlin
 * @Composable
 * fun App() {
 *     QuoVadisNavigation(
 *         rootDestination = MainTabs(initialTab = 1),
 *         config = GeneratedNavigationConfig
 *     )
 * }
 * ```
 *
 * @param rootDestination The destination instance for the root container.
 * @param config The NavigationConfig providing all navigation registries.
 * @param modifier Modifier to apply to the NavigationHost container.
 * @param key Optional custom key for the root navigator node.
 * @param enablePredictiveBack Whether to enable predictive back gesture support.
 * @param predictiveBackMode The mode for predictive back behavior.
 * @param windowSizeClass Optional window size class for adaptive layouts.
 *
 * @see QuoVadisNavigation for the main overload with full documentation
 */
@Composable
fun QuoVadisNavigation(
    rootDestination: NavDestination,
    config: NavigationConfig,
    modifier: Modifier = Modifier,
    key: String? = null,
    enablePredictiveBack: Boolean = platformDefaultPredictiveBack(),
    windowSizeClass: WindowSizeClass? = null
) {
    val navigator = rememberQuoVadisNavigator(
        rootDestination = rootDestination,
        config = config,
        key = key
    )

    NavigationHost(
        navigator = navigator,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
        windowSizeClass = windowSizeClass
    )
}

// =============================================================================
// Platform Defaults
// =============================================================================

/**
 * Returns the platform-specific default for predictive back support.
 *
 * - Android: `true` (system back gesture integration)
 * - iOS: `false` (uses native swipe-back)
 * - Desktop: `false` (no native gesture)
 * - Web: `false` (browser handles back)
 */
@Composable
internal expect fun platformDefaultPredictiveBack(): Boolean
