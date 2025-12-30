package com.jermey.quo.vadis.core.dsl.registry

import com.jermey.quo.vadis.core.navigation.NavDestination
import com.jermey.quo.vadis.core.navigation.pane.PaneRole
import kotlin.reflect.KClass

/**
 * Registry that maps destinations to their pane roles.
 *
 * This registry enables transparent pane navigation where `navigate(destination)`
 * automatically routes to the correct pane's stack based on the destination's
 * configured role.
 *
 * ## How It Works
 *
 * When inside a [PaneNode] context, the navigation system consults this registry
 * to determine which pane a destination belongs to:
 *
 * 1. If destination has a registered role → push to that pane's stack
 * 2. If destination is a @PaneItem root → push to the root's pane stack
 * 3. If no role found → push to the currently active pane's stack
 *
 * ## Registration Sources
 *
 * Pane roles are registered from:
 * - [@PaneItem] annotations → maps root destinations to their pane roles
 * - [@Destination(paneRole = ...)] → maps non-root destinations to pane roles
 *
 * ## Example
 *
 * ```kotlin
 * @Pane(name = "messages")
 * sealed class MessagesPane : NavDestination {
 *     @PaneItem(role = PaneRole.PRIMARY)
 *     @Destination(route = "messages/list")
 *     data object List : MessagesPane()
 *
 *     @PaneItem(role = PaneRole.SECONDARY)
 *     @Destination(route = "messages/empty")
 *     data object Empty : MessagesPane()
 *
 *     // Uses paneRole parameter to associate with SECONDARY
 *     @Destination(route = "messages/detail/{id}", paneRole = PaneRole.SECONDARY)
 *     data class Detail(val id: String) : MessagesPane()
 * }
 *
 * // Navigation from List screen:
 * navigator.navigate(MessagesPane.Detail("123"))
 * // → Automatically pushes to SECONDARY pane's stack
 * ```
 *
 * @see ScopeRegistry
 * @see com.jermey.quo.vadis.core.navigation.PaneNode
 * @see com.jermey.quo.vadis.core.navigation.tree.TreeMutator
 */
interface PaneRoleRegistry {

    /**
     * Gets the pane role for a destination, if registered.
     *
     * @param scopeKey The pane container's scope key (e.g., "messagesPane")
     * @param destination The destination to look up
     * @return The [PaneRole] if registered, null otherwise
     */
    fun getPaneRole(scopeKey: String, destination: NavDestination): PaneRole?

    /**
     * Gets the pane role for a destination class, if registered.
     *
     * @param scopeKey The pane container's scope key (e.g., "messagesPane")
     * @param destinationClass The destination class to look up
     * @return The [PaneRole] if registered, null otherwise
     */
    fun getPaneRole(scopeKey: String, destinationClass: KClass<out NavDestination>): PaneRole?

    /**
     * Checks if a destination has a registered pane role.
     *
     * @param scopeKey The pane container's scope key
     * @param destination The destination to check
     * @return true if the destination has a registered pane role
     */
    fun hasPaneRole(scopeKey: String, destination: NavDestination): Boolean =
        getPaneRole(scopeKey, destination) != null

    companion object {
        /**
         * Empty registry that returns null for all lookups.
         */
        val Empty: PaneRoleRegistry = object : PaneRoleRegistry {
            override fun getPaneRole(scopeKey: String, destination: NavDestination): PaneRole? = null
            override fun getPaneRole(
                scopeKey: String,
                destinationClass: KClass<out NavDestination>
            ): PaneRole? = null
        }
    }
}
