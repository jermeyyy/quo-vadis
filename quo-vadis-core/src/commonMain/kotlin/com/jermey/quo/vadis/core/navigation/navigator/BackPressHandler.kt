package com.jermey.quo.vadis.core.navigation.navigator

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
