package com.jermey.quo.vadis.core.navigation.tree.result

import com.jermey.quo.vadis.core.navigation.PaneNode
import com.jermey.quo.vadis.core.navigation.StackNode
import com.jermey.quo.vadis.core.navigation.TabNode
import com.jermey.quo.vadis.core.navigation.pane.PaneRole

/**
 * Result of a push operation with tab awareness.
 *
 * Used internally to determine how a push should be handled when
 * navigating within a TabNode context.
 */
internal sealed class PushStrategy {
    /** Push to the specified stack normally */
    data class PushToStack(val targetStack: StackNode) : PushStrategy()

    /** Switch to an existing tab that contains the destination */
    data class SwitchToTab(val tabNode: TabNode, val tabIndex: Int) : PushStrategy()

    /** Push to a specific pane's stack within a PaneNode */
    data class PushToPaneStack(val paneNode: PaneNode, val role: PaneRole) : PushStrategy()

    /** Destination is out of scope - push to parent stack */
    data class PushOutOfScope(val parentStack: StackNode) : PushStrategy()
}
