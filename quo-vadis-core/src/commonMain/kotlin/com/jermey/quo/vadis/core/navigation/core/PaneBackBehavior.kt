package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.Serializable

/**
 * Back navigation behavior within a PaneNode.
 */
@Serializable
enum class PaneBackBehavior {
    /** Back forces a change in which pane(s) are visible */
    PopUntilScaffoldValueChange,

    /** Back forces a change in which pane is "active" (focused) */
    PopUntilCurrentDestinationChange,

    /** Back forces a change in content of any pane */
    PopUntilContentChange,

    /** Simple pop from active pane's stack */
    PopLatest
}
