package com.jermey.quo.vadis.core.navigation

import com.jermey.quo.vadis.core.navigation.compose.registry.ContainerRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScreenRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.compose.registry.TransitionRegistry
import com.jermey.quo.vadis.core.navigation.core.NavDestination
import com.jermey.quo.vadis.core.navigation.core.GeneratedDeepLinkHandler
import com.jermey.quo.vadis.core.navigation.core.NavNode
import kotlin.reflect.KClass

/**
 * Unified configuration for all navigation registries.
 *
 * NavigationConfig consolidates all navigation-related registries into a single
 * composable interface. This enables:
 * - Single-point configuration for NavigationHost
 * - Multi-module composition via the [plus] operator
 * - Type-safe initial state building via [buildNavNode]
 *
 * ## Purpose
 *
 * NavigationConfig serves as the central configuration point for the navigation system,
 * aggregating all registry interfaces needed for rendering, transitions, scoping,
 * wrappers, containers, and deep links.
 *
 * ## Multi-Module Support
 *
 * NavigationConfig supports composition via the [plus] operator, allowing multiple
 * feature modules to contribute their navigation configurations:
 *
 * ```kotlin
 * // In app module
 * val appConfig = featureAConfig + featureBConfig + featureCConfig
 *
 * NavigationHost(
 *     config = appConfig,
 *     navigator = navigator
 * )
 * ```
 *
 * When combining configs, the second (right-hand) config takes priority for
 * duplicate registrations.
 *
 * ## KSP Integration
 *
 * The KSP processor generates implementations of this interface that aggregate
 * all registries from annotated destinations, wrappers, and transitions.
 *
 * @see ScreenRegistry for screen content rendering
 * @see ScopeRegistry for navigation scope membership
 * @see TransitionRegistry for destination-specific transitions
 * @see ContainerRegistry for container node builders and wrapper composables
 * @see GeneratedDeepLinkHandler for deep link handling
 */
interface NavigationConfig {

    /**
     * Registry for mapping destinations to composable screen content.
     *
     * Used by the navigation renderer to display the appropriate content
     * for each destination in the navigation tree.
     */
    val screenRegistry: ScreenRegistry

    /**
     * Registry for navigation scope membership.
     *
     * Used to determine whether a destination belongs to a container's
     * scope (TabNode/PaneNode) for scope-aware navigation.
     */
    val scopeRegistry: ScopeRegistry

    /**
     * Registry for destination-specific transition animations.
     *
     * Provides custom transitions defined via annotations on destination classes.
     */
    val transitionRegistry: TransitionRegistry

    /**
     * Registry for container node builders and wrapper composables.
     *
     * Maps destinations to their container structures (TabNode, PaneNode)
     * for automatic container creation during navigation, and provides
     * custom wrapper composables for tab bars, bottom navigation, and
     * multi-pane layouts.
     */
    val containerRegistry: ContainerRegistry

    /**
     * Handler for deep link URI parsing and destination creation.
     *
     * Nullable because not all applications require deep link support.
     * When present, enables URI-based navigation and destination creation.
     */
    val deepLinkHandler: GeneratedDeepLinkHandler?

    /**
     * Builds the initial NavNode for the given destination class.
     *
     * This method creates the appropriate navigation node structure for a destination,
     * including any required container nodes (TabNode, PaneNode) based on the
     * destination's container annotations.
     *
     * ## Usage
     *
     * ```kotlin
     * // Build a simple screen node
     * val node = config.buildNavNode(HomeScreen::class)
     *
     * // Build with explicit key
     * val node = config.buildNavNode(
     *     destinationClass = DetailScreen::class,
     *     key = "detail-123",
     *     parentKey = "root"
     * )
     * ```
     *
     * @param destinationClass The [KClass] of the destination to build a node for
     * @param key Optional explicit key for the node (auto-generated if null)
     * @param parentKey Optional key of the parent node
     * @return The constructed [NavNode], or null if the destination class is not registered
     */
    fun buildNavNode(
        destinationClass: KClass<out NavDestination>,
        key: String? = null,
        parentKey: String? = null
    ): NavNode?

    /**
     * Combines this config with another, returning a composite config.
     *
     * The composite config merges all registries from both configs, with the
     * [other] (right-hand) config taking priority for any duplicate registrations.
     *
     * ## Usage
     *
     * ```kotlin
     * val combinedConfig = baseConfig + featureConfig
     *
     * // Chaining multiple configs
     * val appConfig = config1 + config2 + config3
     * ```
     *
     * ## Priority Rules
     *
     * When both configs provide values for the same lookup:
     * - [other]'s registry is checked first
     * - [this]'s registry is checked as fallback
     *
     * @param other The config to combine with this one
     * @return A new [NavigationConfig] combining both configs
     */
    operator fun plus(other: NavigationConfig): NavigationConfig

    /**
     * Companion object providing default implementations.
     */
    companion object {
        /**
         * Empty configuration with no registrations.
         *
         * All registries return their respective [Empty] implementations.
         * Useful as a starting point or for testing.
         *
         * ## Identity Element
         *
         * [Empty] acts as an identity element for the [plus] operator:
         * - `Empty + config` returns `config`
         * - `config + Empty` returns `config`
         */
        val Empty: NavigationConfig = EmptyNavigationConfig
    }
}
