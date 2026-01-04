package com.jermey.quo.vadis.core.navigation.internal.tree.result

import com.jermey.quo.vadis.core.navigation.node.NavNode
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Result of a pop operation that respects PaneBackBehavior.
 */
sealed class PopResult {
    /** Successfully popped within current pane */
    data class Popped(val newState: NavNode) : PopResult()

    /** Pane is empty, behavior depends on PaneBackBehavior */
    data class PaneEmpty(val paneRole: PaneRole) : PopResult()

    /** Cannot pop - would leave tree in invalid state */
    data object CannotPop : PopResult()

    /** Back behavior requires scaffold/visual change (renderer must handle) */
    data object RequiresScaffoldChange : PopResult()
}
