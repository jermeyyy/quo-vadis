package com.jermey.quo.vadis.core.navigation.navigator

import androidx.compose.runtime.Stable
import com.jermey.quo.vadis.core.navigation.destination.NavDestination
import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Extension interface for pane-specific navigation operations.
 *
 * This interface extends [Navigator] with capabilities for adaptive multi-pane layouts.
 * Use [Navigator.asPaneNavigator] to safely access these methods when available.
 *
 * @see Navigator
 * @see PaneRole
 */
@Stable
interface PaneNavigator : Navigator {

    /**
     * Check if a pane role is available in the current state.
     *
     * @param role Pane role to check
     * @return true if the role is configured in the current PaneNode
     */
    fun isPaneAvailable(role: PaneRole): Boolean

    /**
     * Get the current content of a specific pane.
     *
     * @param role Pane role to query
     * @return The NavNode content of the pane, or null if role not configured
     */
    fun paneContent(role: PaneRole): NavNode?

    /**
     * Navigate to a destination in a specific pane of an adaptive layout.
     *
     * Use this when you need to target a particular pane in a multi-pane layout
     * (for example, a master–detail or split view) without changing the primary
     * navigation stack.
     *
     * @param destination The destination to display in the targeted pane.
     * @param role The [PaneRole] identifying which pane to navigate within.
     */
    fun navigateToPane(destination: NavDestination, role: PaneRole = PaneRole.Supporting)

    /**
     * Navigate back within a specific pane.
     *
     * Pops from the specified pane's stack regardless of which pane is active.
     *
     * @param role Pane role to pop from
     * @return true if navigation occurred, false if pane stack was empty
     */
    fun navigateBackInPane(role: PaneRole): Boolean
}

/**
 * Safely cast Navigator to PaneNavigator if supported.
 *
 * @return PaneNavigator instance if this Navigator supports pane operations, null otherwise
 */
fun Navigator.asPaneNavigator(): PaneNavigator? = this as? PaneNavigator
