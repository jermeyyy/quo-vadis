package com.jermey.quo.vadis.core.navigation.internal.tree.config

/**
 * Configures behavior when a StackNode becomes empty after pop.
 */
enum class PopBehavior {
    /**
     * Remove the empty stack from parent (cascading pop).
     * This continues popping until a non-empty container is found.
     */
    CASCADE,

    /**
     * Preserve the empty stack in place.
     * The stack remains but with no children.
     */
    PRESERVE_EMPTY
}
