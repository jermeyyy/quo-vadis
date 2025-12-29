package com.jermey.quo.vadis.core.navigation

/**
 * Interface for components that can handle back press events.
 *
 * Implementations return `true` if they consume the back press event,
 * or `false` if the event should bubble up to the parent component.
 *
 * This interface enables hierarchical back press handling where child navigators
 * can handle back presses first, and only pass them to parent navigators when
 * they cannot handle them (e.g., when their backstack is empty).
 *
 * Example hierarchy:
 * ```
 * MainNavigator (handles app-level back)
 *   └─ TabNavigator (handles tab switching)
 *       └─ Tab Navigator (handles within-tab navigation)
 * ```
 *
 * Back press flow:
 * 1. Event starts at deepest child
 * 2. Child returns true → event consumed, stop
 * 3. Child returns false → pass to parent
 * 4. Continue until consumed or reach system
 */
interface BackPressHandler {
    /**
     * Handle a back press event.
     *
     * @return `true` if the event was consumed (handled), `false` if it should bubble up.
     */
    fun onBack(): Boolean
}

/**
 * Interface for navigators that can contain child navigators.
 *
 * Provides the default delegation pattern for hierarchical navigation:
 * child navigators get first chance to handle back press events.
 */
interface ParentNavigator : BackPressHandler {
    /**
     * Get the currently active child navigator, if any.
     *
     * This is used for back press delegation - the active child gets
     * first chance to handle the back press.
     *
     * @return The active child handler, or null if no child is active.
     */
    val activeChild: BackPressHandler? get() = null

    /**
     * Handle back press with delegation to child.
     *
     * Default implementation:
     * 1. Try active child first (if exists and returns true → consumed)
     * 2. If child doesn't consume, try this navigator's own logic
     *
     * Override [handleBackInternal] to provide navigator-specific back logic.
     */
    override fun onBack(): Boolean {
        // First, try delegating to active child
        val child = activeChild
        if (child != null && child.onBack()) {
            return true // Child consumed the event
        }

        // Child didn't consume (or no child), try our own logic
        return handleBackInternal()
    }

    /**
     * Handle back press for this navigator specifically (without delegation).
     *
     * Implementations should define their own back press logic here,
     * e.g., popping from backstack, switching tabs, etc.
     *
     * @return `true` if this navigator consumed the event, `false` otherwise.
     */
    fun handleBackInternal(): Boolean
}
