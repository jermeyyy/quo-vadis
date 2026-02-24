package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.navigation.node.NavNode

/**
 * Result of a tree-aware back operation.
 */
sealed interface BackResult {
    /** Back was handled, new tree state returned */
    data class Handled(val newState: NavNode) : BackResult

    /** Back should be delegated to system (e.g., close app) */
    data object DelegateToSystem : BackResult

    /** Back could not be handled (internal error) */
    data object CannotHandle : BackResult
}
