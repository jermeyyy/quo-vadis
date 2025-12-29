package com.jermey.quo.vadis.core.navigation.pane

import kotlinx.serialization.Serializable

/**
 * Defines the semantic role of a pane within an adaptive layout.
 * Roles determine adaptation strategies and navigation behavior.
 */
@Serializable
enum class PaneRole {
    /** The primary content pane (e.g., document editor, video player) */
    Primary,

    /** Supporting content that provides context to primary (e.g., comments, related items) */
    Supporting,

    /** Optional extra pane for supplementary content */
    Extra
}
