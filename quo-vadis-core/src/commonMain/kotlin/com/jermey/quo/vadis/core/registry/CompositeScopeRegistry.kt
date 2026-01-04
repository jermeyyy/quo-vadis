package com.jermey.quo.vadis.core.registry

import com.jermey.quo.vadis.core.InternalQuoVadisApi
import com.jermey.quo.vadis.core.navigation.destination.NavDestination

/**
 * Composite scope registry that delegates to secondary first, then primary.
 *
 * **Internal API** - This is an internal implementation detail of Quo Vadis.
 * Composite registries are managed internally by the navigation system.
 */
@InternalQuoVadisApi
internal class CompositeScopeRegistry(
    private val primary: ScopeRegistry,
    private val secondary: ScopeRegistry
) : ScopeRegistry {

    override fun isInScope(scopeKey: String, destination: NavDestination): Boolean {
        // Check secondary first for an explicit registration
        val secondaryScopeKey = secondary.getScopeKey(destination)
        if (secondaryScopeKey != null) {
            return secondary.isInScope(scopeKey, destination)
        }
        // Fall back to primary
        return primary.isInScope(scopeKey, destination)
    }

    override fun getScopeKey(destination: NavDestination): String? {
        return secondary.getScopeKey(destination) ?: primary.getScopeKey(destination)
    }
}
