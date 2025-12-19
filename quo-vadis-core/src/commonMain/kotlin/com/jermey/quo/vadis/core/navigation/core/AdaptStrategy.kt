package com.jermey.quo.vadis.core.navigation.core

import kotlinx.serialization.Serializable

/**
 * Strategy for adapting a pane when space is insufficient.
 */
@Serializable
enum class AdaptStrategy {
    /** Hide the pane completely */
    Hide,

    /** Show as a levitated overlay (modal/dialog-like) */
    Levitate,

    /** Reflow under another pane (vertical stacking) */
    Reflow
}