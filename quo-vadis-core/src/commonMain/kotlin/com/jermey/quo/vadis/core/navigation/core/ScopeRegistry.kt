package com.jermey.quo.vadis.core.navigation.core

/**
 * Registry for determining navigation scope membership.
 *
 * Used by [TreeMutator] to decide whether a destination belongs to
 * a container's scope (TabNode/PaneNode) or should navigate outside.
 *
 * ## Purpose
 *
 * When navigating from within a tab or pane container, the scope registry
 * determines whether the destination is "in scope" (part of that container's
 * sealed class hierarchy) or "out of scope" (should navigate to parent stack).
 *
 * ## Example
 *
 * ```kotlin
 * // Given a TabNode with scopeKey = "MainTabs"
 * // and MainTabs sealed class containing Home, Search, Profile
 *
 * registry.isInScope("MainTabs", HomeDestination) // true - stay in tab
 * registry.isInScope("MainTabs", DetailDestination) // false - navigate outside
 * ```
 *
 * ## Implementation
 *
 * KSP-generated code registers scope memberships automatically based on
 * sealed class hierarchies. Manual registration is also supported for
 * dynamic scenarios.
 *
 * @see TreeMutator.push for scope-aware navigation
 * @see StackNode.scopeKey for stack container scopes
 * @see TabNode.scopeKey for tab container scopes
 * @see PaneNode.scopeKey for pane container scopes
 */
public interface ScopeRegistry {
    /**
     * Checks if a destination belongs to the scope identified by [scopeKey].
     *
     * @param scopeKey The scope identifier (typically the sealed class simple name)
     * @param destination The destination to check
     * @return true if the destination is in scope, false otherwise
     */
    fun isInScope(scopeKey: String, destination: Destination): Boolean

    /**
     * Gets the scope key for a destination, if it belongs to any registered scope.
     *
     * @param destination The destination to lookup
     * @return The scope key if found, null otherwise
     */
    fun getScopeKey(destination: Destination): String?

    companion object {
        /**
         * Empty registry that allows all destinations in all scopes.
         * Use this for backward compatibility when scope checking is disabled.
         */
        val Empty: ScopeRegistry = object : ScopeRegistry {
            override fun isInScope(scopeKey: String, destination: Destination): Boolean = true
            override fun getScopeKey(destination: Destination): String? = null
        }
    }
}
