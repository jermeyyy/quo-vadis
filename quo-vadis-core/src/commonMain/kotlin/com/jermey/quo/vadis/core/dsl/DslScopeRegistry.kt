package com.jermey.quo.vadis.core.dsl

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.node.ScopeKey
import com.jermey.quo.vadis.core.registry.ScopeRegistry
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import kotlin.reflect.KClass

/**
 * DSL-based implementation of [ScopeRegistry] that determines scope membership
 * for destinations.
 *
 * This registry is created by [DslNavigationConfig] from scope definitions
 * collected by [NavigationConfigBuilder], including both explicit scope
 * declarations and auto-inferred scopes from container registrations.
 *
 * ## Scope Membership
 *
 * Scopes are used by the navigation system to determine whether a destination
 * belongs to a container's scope (TabNode/PaneNode). This enables scope-aware
 * navigation where out-of-scope destinations navigate outside the container.
 *
 * ## Usage
 *
 * Scopes can be defined explicitly or inferred from containers:
 *
 * ```kotlin
 * val config = navigationConfig {
 *     // Explicit scope definition
 *     scope("main-stack") {
 *         +HomeScreen::class
 *         +DetailScreen::class
 *     }
 *
 *     // Auto-inferred from container
 *     tabs<MainTabs>("main-tabs") {
 *         tab(HomeTab, title = "Home")  // Added to "main-tabs" scope
 *         tab(ProfileTab, title = "Profile")  // Added to "main-tabs" scope
 *     }
 * }
 * ```
 *
 * @param scopes Map of scope keys to sets of destination classes in each scope
 *
 * @see ScopeRegistry
 * @see NavigationConfigBuilder.scope
 */
@InternalQuoVadisApi
internal class DslScopeRegistry(
    private val scopes: Map<ScopeKey, Set<KClass<out NavDestination>>>
) : ScopeRegistry {

    /**
     * Reverse mapping from destination class to its scope key.
     *
     * Built lazily to optimize lookups. If a destination belongs to
     * multiple scopes, the first one encountered is used.
     */
    private val destinationToScope: Map<KClass<out NavDestination>, ScopeKey> by lazy {
        buildDestinationToScopeMap()
    }

    /**
     * Checks if a destination belongs to the scope identified by [scopeKey].
     *
     * @param scopeKey The scope identifier to check
     * @param destination The destination to check
     * @return true if the destination's class is in the specified scope
     */
    override fun isInScope(scopeKey: ScopeKey, destination: NavDestination): Boolean {
        val scopeMembers = scopes[scopeKey] ?: return false
        return scopeMembers.contains(destination::class)
    }

    /**
     * Gets the scope key for a destination, if it belongs to any registered scope.
     *
     * @param destination The destination to lookup
     * @return The scope key if found, null otherwise
     */
    override fun getScopeKey(destination: NavDestination): ScopeKey? {
        return destinationToScope[destination::class]
    }

    /**
     * Builds the reverse mapping from destination classes to scope keys.
     *
     * @return Map of destination classes to their primary scope keys
     */
    private fun buildDestinationToScopeMap(): Map<KClass<out NavDestination>, ScopeKey> {
        val result = mutableMapOf<KClass<out NavDestination>, ScopeKey>()

        scopes.forEach { (scopeKey, members) ->
            members.forEach { memberClass ->
                // First scope wins if destination is in multiple scopes
                if (!result.containsKey(memberClass)) {
                    result[memberClass] = scopeKey
                }
            }
        }

        return result
    }
}
